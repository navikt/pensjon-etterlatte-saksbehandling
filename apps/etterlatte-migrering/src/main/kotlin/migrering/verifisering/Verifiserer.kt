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
                feil.add(StrengtFortrolig)
            }
            feil.addAll(sjekkAtPersonerFinsIPDL(it))
            val soeker = personHenter.hentPerson(PersonRolle.BARN, request.soeker).getOrNull()

            if (soeker != null) {
                feil.addAll(sjekkAtSoekerHarRelevantVerge(request, soeker))
                if (!request.erUnder18) {
                    feil.addAll(sjekkAdresseOgUtlandsopphold(soeker))
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

    private fun sjekkAdresseOgUtlandsopphold(person: PersonDTO): List<Verifiseringsfeil> {
        val utlandSjekker = mutableListOf<Verifiseringsfeil>()
        val kontaktadresse = person.kontaktadresse ?: emptyList()
        val bostedsadresse = person.bostedsadresse ?: emptyList()
        val oppholdsadresse = person.oppholdsadresse ?: emptyList()
        val adresser = kontaktadresse + bostedsadresse + oppholdsadresse

        if (adresser.any { it.verdi.land != "NOR" }) {
            utlandSjekker.add(SoekerBorUtland)
        }

        if (adresser.isEmpty()) {
            utlandSjekker.add(BrukerManglerAdresse)
        }

        val harFlyttetFraNorge = person.utland?.verdi?.utflyttingFraNorge?.isNotEmpty() ?: false
        val harFlyttetTilNorge = person.utland?.verdi?.innflyttingTilNorge?.isNotEmpty() ?: false
        if (harFlyttetTilNorge || harFlyttetFraNorge) {
            utlandSjekker.add(SoekerHarBoddUtland)
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
            return listOf(SoekerHarFlereAvdoede)
        }
        return emptyList()
    }

    private fun sjekkOmForandringIForeldreforhold(
        request: MigreringRequest,
        soeker: PersonDTO,
    ): List<Exception> {
        val tidligereForeldre = request.avdoedForelder.map { it.ident.value } + listOfNotNull(request.gjenlevendeForelder).map { it.value }
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

data object StrengtFortrolig : Verifiseringsfeil() {
    override val message: String
        get() = "Skal ikke migrere strengt fortrolig sak"
}

data class PDLException(val kilde: Throwable) : Verifiseringsfeil() {
    override val message: String?
        get() = kilde.message
}

data object SoekerErOver18 : Verifiseringsfeil() {
    override val message: String
        get() = "Skal ikke per nå migrere søker der søker er over 18"
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

data object SoekerHarBoddUtland : Verifiseringsfeil() {
    override val message: String
        get() = "Søker har bodd utlands"
}

data object SoekerHarFlereAvdoede : Verifiseringsfeil() {
    override val message: String
        get() = "Søker har flere avøde"
}

data object ForeldreForholdHarEndretSeg : Verifiseringsfeil() {
    override val message: String
        get() = "Søker har oppdatert foreldreforhold"
}

data object BrukerManglerAdresse : Verifiseringsfeil() {
    override val message: String
        get() = "Bruker mangler adresse i PDL"
}
