package no.nav.etterlatte.migrering.verifisering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.logging.samleExceptions
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.migrering.Migreringsstatus
import no.nav.etterlatte.migrering.PesysRepository
import no.nav.etterlatte.migrering.grunnlag.GrunnlagKlient
import no.nav.etterlatte.migrering.grunnlag.Utenlandstilknytningsjekker
import no.nav.etterlatte.migrering.start.MigreringFeatureToggle
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class Verifiserer(
    private val repository: PesysRepository,
    private val gjenlevendeForelderPatcher: GjenlevendeForelderPatcher,
    private val utenlandstilknytningsjekker: Utenlandstilknytningsjekker,
    private val personHenter: PersonHenter,
    private val featureToggleService: FeatureToggleService,
    private val grunnlagKlient: GrunnlagKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun verifiserRequest(request: MigreringRequest): MigreringRequest {
        val patchedRequest = gjenlevendeForelderPatcher.patchGjenlevendeHvisIkkeOppgitt(request)
        val feil = mutableListOf<Exception>()
        patchedRequest.onFailure { feilen ->
            feil.add(PDLException(feilen).also { it.addSuppressed(feilen) })
        }
        patchedRequest.onSuccess {
            if (request.enhet.nr == "2103") {
                feil.add(StrengtFortroligPesys)
            }
            feil.addAll(sjekkAtPersonerFinsIPDL(it))
            val soeker = personHenter.hentPerson(PersonRolle.BARN, request.soeker).getOrNull()

            if (soeker != null) {
                if (soeker.adressebeskyttelse?.verdi?.erStrengtFortrolig() == true) {
                    feil.add(StrengtFortroligPDL)
                }
                feil.addAll(sjekkAtSoekerHarRelevantVerge(request, soeker))
                if (!request.erUnder18) {
                    feil.addAll(sjekkAdresseOgUtlandsopphold(request.pesysId.id, soeker, request))
                    feil.addAll(sjekkOmSoekerHarFlereAvoedeForeldre(request))
                    feil.addAll(sjekkOmForandringIForeldreforhold(request, soeker))
                }
            }
        }

        if (feil.isNotEmpty()) {
            haandterFeil(request, feil)
        }
        return patchedRequest.getOrThrow().copy(
            utlandstilknytningType = utenlandstilknytningsjekker.finnUtenlandstilknytning(request),
        )
    }

    private fun haandterFeil(
        request: MigreringRequest,
        feil: MutableList<Exception>,
    ) {
        logger.warn(
            "Sak ${request.pesysId} har ufullstendige data i PDL, eller feiler verifisering av andre grunner. " +
                "Kan ikke migrere. Se sikkerlogg for detaljer",
        )
        repository.lagreFeilkjoering(
            request.toJson(),
            feilendeSteg = Migreringshendelser.VERIFISER,
            feil = feil.map { it.message }.toJson(),
            pesysId = request.pesysId,
        )
        repository.oppdaterStatus(request.pesysId, Migreringsstatus.VERIFISERING_FEILA)
        throw samleExceptions(feil)
    }

    private fun sjekkAtPersonerFinsIPDL(request: MigreringRequest): List<Verifiseringsfeil> {
        val personer = mutableListOf(Pair(PersonRolle.BARN, request.soeker))
        request.avdoedForelder.forEach { personer.add(Pair(PersonRolle.AVDOED, it.ident)) }
        request.gjenlevendeForelder?.let { personer.add(Pair(PersonRolle.GJENLEVENDE, it)) }

        return personer
            .map {
                val person = personHenter.hentPerson(it.first, it.second)

                if (it.first == PersonRolle.BARN) {
                    if (person.getOrNull()?.doedsdato != null) {
                        return listOf(SoekerErDoed)
                    }
                }
                person
            }
            .filter { it.isFailure }
            .map { it.exceptionOrNull() }
            .filterIsInstance<Verifiseringsfeil>()
    }

    private fun sjekkAtSoekerHarRelevantVerge(
        request: MigreringRequest,
        person: PersonDTO,
    ): List<Verifiseringsfeil> {
        if ((person.vergemaalEllerFremtidsfullmakt?.size ?: 0) > 1) {
            return listOf(BarnetHarFlereVerger)
        }
        if (person.vergemaalEllerFremtidsfullmakt.isNullOrEmpty()) {
            return emptyList()
        }

        if (!featureToggleService.isEnabled(MigreringFeatureToggle.MigrerNaarSoekerHarVerge, false)) {
            return listOf(BarnetHarVergemaal)
        } else {
            return try {
                runBlocking {
                    if (grunnlagKlient.hentVergesAdresse(request.soeker.value) == null) {
                        listOf(VergeManglerAdresseFraPDL)
                    } else {
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                logger.error("Feil under henting av verges adresse", e)
                listOf(FeilUnderHentingAvVergesAdresse)
            }
        }
    }

    private fun sjekkAdresseOgUtlandsopphold(
        pesysId: Long,
        person: PersonDTO,
        request: MigreringRequest,
    ): List<Verifiseringsfeil> {
        val utlandSjekker = mutableListOf<Verifiseringsfeil>()
        val kontaktadresse = person.kontaktadresse ?: emptyList()
        val bostedsadresse = person.bostedsadresse ?: emptyList()
        val oppholdsadresse = person.oppholdsadresse ?: emptyList()
        val adresseland = kontaktadresse + bostedsadresse + oppholdsadresse

        logger.info(
            "Sak med pesysId=$pesysId har adresseland:" +
                " kontaktadresse=${kontaktadresse.map { it.verdi.land }}," +
                " bosted=${bostedsadresse.map { it.verdi.land }}," +
                " opphold?${oppholdsadresse.map { it.verdi.land }}",
        )

        if (adresseland.mapNotNull { it.verdi.land }.any { it.uppercase() != "NOR" }) {
            utlandSjekker.add(SoekerBorUtland)
        }

        if (adresseland.none { it.verdi.gyldigTilOgMed.let { tilOgMed -> tilOgMed == null || tilOgMed > LocalDateTime.now() } }) {
            utlandSjekker.add(BrukerManglerAdresse)
        }

        person.utland?.verdi?.let { utland ->
            val foedselsdato = person.foedselsdato?.verdi ?: request.soeker.getBirthDate()
            val datoFylte18 = foedselsdato.plusYears(18)

            if (utland.innflyttingTilNorge?.any { it.dato == null || it.dato!! >= datoFylte18 } == true) {
                utlandSjekker.add(SoekerHarInnflyttingTilNorgeEtter18Aar)
            }
            if (utland.utflyttingFraNorge?.any { it.dato == null || it.dato!! >= datoFylte18 } == true) {
                utlandSjekker.add(SoekerHarUtflyttingFraNorgeEtter18Aar)
            }
        }

        return utlandSjekker
    }

    private fun sjekkOmSoekerHarFlereAvoedeForeldre(request: MigreringRequest): List<Verifiseringsfeil> {
        if (request.avdoedForelder.size > 1) {
            return listOf(SoekerHarFlereAvdoede)
        }
        val gjenlevende =
            request.gjenlevendeForelder?.let {
                personHenter.hentPerson(PersonRolle.AVDOED, it).getOrNull()
            }

        if (gjenlevende?.doedsdato != null) {
            return listOf(SoekerHarFlereAvdoedePaaGjenoppstaattYtelse)
        }
        return emptyList()
    }

    private fun sjekkOmForandringIForeldreforhold(
        request: MigreringRequest,
        soeker: PersonDTO,
    ): List<Exception> {
        val tidligereForeldre =
            request.avdoedForelder.map { it.ident.value } + listOfNotNull(request.gjenlevendeForelder).map { it.value }
        val nyeForeldre = soeker.familieRelasjon?.verdi?.foreldre?.map { it.value }
        if (nyeForeldre == null || (tidligereForeldre.containsAll(nyeForeldre))) {
            listOf(ForeldreForholdHarEndretSeg)
        }
        return emptyList()
    }
}

sealed class Verifiseringsfeil : Exception()

data class FinsIkkeIPDL(val rolle: PersonRolle, val id: Folkeregisteridentifikator) : Verifiseringsfeil() {
    override val message: String
        get() = toString()
}

data object BarnetHarFlereVerger : Verifiseringsfeil() {
    override val message: String
        get() = "Barnet har flere verger"
}

data object BarnetHarVergemaal : Verifiseringsfeil() {
    override val message: String
        get() = "Barn har vergemål eller framtidsfullmakt, støtte for det er deaktivert"
}

data object StrengtFortroligPesys : Verifiseringsfeil() {
    override val message: String
        get() = "Skal ikke migrere strengt fortrolig saker (Pesys)"
}

data object StrengtFortroligPDL : Verifiseringsfeil() {
    override val message: String
        get() = "Skal ikke migrere strengt fortrolig saker (PDL)"
}

data class PDLException(val kilde: Throwable) : Verifiseringsfeil() {
    override val message: String?
        get() = kilde.message
}

data object VergeManglerAdresseFraPDL : Verifiseringsfeil() {
    override val message: String
        get() = "Verge mangler adresse i PDL"
}

data object FeilUnderHentingAvVergesAdresse : Verifiseringsfeil() {
    override val message: String
        get() = "Noe feil skjedde under henting av verges adresse, se detaljer i logg"
}

data object SoekerErDoed : Verifiseringsfeil() {
    override val message: String
        get() = "Søker er død"
}

data object SoekerBorUtland : Verifiseringsfeil() {
    override val message: String
        get() = "Søker bor utlands"
}

data object SoekerHarInnflyttingTilNorgeEtter18Aar : Verifiseringsfeil() {
    override val message: String
        get() = "Søker har registrert innflytting til Norge etter fylte 18 år eller manglende dato"
}

data object SoekerHarUtflyttingFraNorgeEtter18Aar : Verifiseringsfeil() {
    override val message: String
        get() = "Søker har registrert utflytting fra Norge etter fylte 18 år eller manglende dato"
}

data object SoekerHarFlereAvdoede : Verifiseringsfeil() {
    override val message: String
        get() = "Søker har flere avøde"
}

data object SoekerHarFlereAvdoedePaaGjenoppstaattYtelse : Verifiseringsfeil() {
    override val message: String
        get() = "Søker har flere avdøde på gjenoppstått ytelse"
}

data object ForeldreForholdHarEndretSeg : Verifiseringsfeil() {
    override val message: String
        get() = "Søker har oppdatert foreldreforhold"
}

data object BrukerManglerAdresse : Verifiseringsfeil() {
    override val message: String
        get() = "Bruker mangler adresse i PDL"
}
