package no.nav.etterlatte.migrering.verifisering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.grunnlag.VurdertBostedsland
import no.nav.etterlatte.libs.common.logging.samleExceptions
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.migrering.Migreringsstatus
import no.nav.etterlatte.migrering.PesysRepository
import no.nav.etterlatte.migrering.grunnlag.GrunnlagKlient
import no.nav.etterlatte.migrering.grunnlag.Utenlandstilknytningsjekker
import no.nav.etterlatte.migrering.pen.PenKlient
import no.nav.etterlatte.migrering.pen.SakSammendragResponse
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class Verifiserer(
    private val repository: PesysRepository,
    private val gjenlevendeForelderPatcher: GjenlevendeForelderPatcher,
    private val utenlandstilknytningsjekker: Utenlandstilknytningsjekker,
    private val personHenter: PersonHenter,
    private val grunnlagKlient: GrunnlagKlient,
    private val penKlient: PenKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun verifiserRequest(request: MigreringRequest): MigreringRequest {
        val patchedRequestResult = gjenlevendeForelderPatcher.patchGjenlevendeHvisIkkeOppgitt(request)

        val feilSomAvbryter = mutableListOf<Exception>()
        val feilSomMedfoererManuell = mutableListOf<Exception>()

        patchedRequestResult.onFailure { feilen ->
            feilSomMedfoererManuell.add(PDLException(feilen).also { it.addSuppressed(feilen) })
        }

        val patchedRequest = patchedRequestResult.getOrThrow()

        val soeker = personHenter.hentPerson(PersonRolle.BARN, patchedRequest.soeker).getOrNull()
        val finnesIPdlFeil = pesonerFinnesIPdlEllerSoekerErDoed(patchedRequest)
        feilSomAvbryter.addAll(finnesIPdlFeil)

        if (soeker != null) {
            feilSomMedfoererManuell.addAll(sjekkAtSoekerHarRelevantVerge(soeker))
            if (!patchedRequest.erUnder18) {
                feilSomMedfoererManuell.addAll(sjekkOmSoekerHaddeFlyktningerfordel(patchedRequest))
                feilSomMedfoererManuell.addAll(sjekkAdresseOgUtlandsopphold(patchedRequest.pesysId.id, soeker, patchedRequest))
                feilSomMedfoererManuell.addAll(sjekkOmSoekerHarFlereAvoedeForeldre(patchedRequest))
                feilSomMedfoererManuell.addAll(sjekkOmForandringIForeldreforhold(patchedRequest, soeker))
                // Trolig unødvendig da ukjente foreldre alltid er utlandsaker
                feilSomMedfoererManuell.addAll(sjekkOmUkjentForelder(patchedRequest))
            }
        }
        if (patchedRequest.beregning.meta?.beregningsMetodeType != "FOLKETRYGD") {
            feilSomMedfoererManuell.add(BeregningsmetodeIkkeNasjonal)
        }
        feilSomMedfoererManuell.addAll(verifiserUfoere(patchedRequest))

        val alleFeil = feilSomAvbryter + feilSomMedfoererManuell
        if (alleFeil.isNotEmpty()) {
            logger.warn(
                "Sak ${request.pesysId} har ufullstendige data i PDL, eller feiler verifisering av andre grunner. " +
                    "Kan ikke migrere. Se sikkerlogg for detaljer",
            )
            repository.lagreFeilkjoering(
                patchedRequest.toJson(),
                feilendeSteg = Migreringshendelser.VERIFISER.lagEventnameForType(),
                feil = alleFeil.map { it.message }.toJson(),
                pesysId = request.pesysId,
            )
            if (feilSomAvbryter.isNotEmpty()) {
                repository.oppdaterStatus(request.pesysId, Migreringsstatus.VERIFISERING_FEILA)
                throw samleExceptions(feilSomAvbryter)
            }
        }
        return patchedRequest.copy(
            utlandstilknytningType = utenlandstilknytningsjekker.finnUtenlandstilknytning(request),
            kanAutomatiskGjenopprettes = feilSomMedfoererManuell.isEmpty(),
            gradering = soeker?.adressebeskyttelse?.verdi,
        )
    }

    private fun verifiserUfoere(request: MigreringRequest): List<Verifiseringsfeil> {
        val ufoereSak = hentSakSammendragForSoeker(request.soeker.value)
        if (ufoereSak != null) {
            return listOf(HarUfoereSak(ufoereSak.sakStatus.name))
        }
        return emptyList()
    }

    private fun hentSakSammendragForSoeker(fnr: String): SakSammendragResponse? {
        logger.info("Prøver å hente sak sammendrag for ${fnr.maskerFnr()} fra PEN")
        val sakFraPEN = runBlocking { penKlient.sakMedUfoere(fnr) }
        logger.info("Henta sak sammendrag for ${fnr.maskerFnr()} fra PEN")
        return sakFraPEN.firstOrNull {
            it.sakType == SakSammendragResponse.UFORE_SAKTYPE &&
                listOf(
                    SakSammendragResponse.Status.LOPENDE,
                    SakSammendragResponse.Status.OPPRETTET,
                    SakSammendragResponse.Status.TIL_BEHANDLING,
                ).contains(it.sakStatus)
        }
    }

    private fun sjekkOmSoekerHaddeFlyktningerfordel(request: MigreringRequest): List<Verifiseringsfeil> =
        when (request.anvendtFlyktningerfordel()) {
            true -> listOf(SoekerHaddeFlyktningerfordel)
            false -> emptyList()
        }

    private fun pesonerFinnesIPdlEllerSoekerErDoed(request: MigreringRequest): List<Verifiseringsfeil> {
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

    private fun sjekkAtSoekerHarRelevantVerge(person: PersonDTO): List<Verifiseringsfeil> {
        if ((person.vergemaalEllerFremtidsfullmakt?.size ?: 0) > 1) {
            return listOf(BarnetHarFlereVerger)
        }
        if (person.vergemaalEllerFremtidsfullmakt.isNullOrEmpty()) {
            return emptyList()
        }

        return listOf(BarnetHarVergemaal)
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

        if (adresseland.none { erGyldigNaaEllerFramover(it) }) {
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

        val bostedsland = runBlocking { grunnlagKlient.hentBostedsland(request.soeker.value) }

        if (!bostedsland.erNorge()) {
            utlandSjekker.add(BostedslandErIkkeNorge(bostedsland))
        }

        return utlandSjekker
    }

    private fun erGyldigNaaEllerFramover(opplysning: OpplysningDTO<Adresse>) =
        opplysning.verdi.gyldigTilOgMed.let { tilOgMed -> tilOgMed == null || tilOgMed > LocalDateTime.now() }

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

    private fun sjekkOmUkjentForelder(request: MigreringRequest): List<Exception> {
        val foreldre = request.avdoedForelder.map { it.ident } + listOfNotNull(request.gjenlevendeForelder)
        if (foreldre.size < 2) {
            return listOf(UkjentForelder)
        }
        return emptyList()
    }
}

sealed class Verifiseringsfeil : Exception()

data class FinsIkkeIPDL(val rolle: PersonRolle, val id: Folkeregisteridentifikator) : Verifiseringsfeil() {
    override val message: String
        get() = toString()
}

data class HarUfoereSak(val status: String) : Verifiseringsfeil() {
    override val message: String
        get() = "Har uføretrygd med status $status"
}

data object SoekerHaddeFlyktningerfordel : Verifiseringsfeil() {
    override val message: String
        get() = "Søker hadde flyktningfordel"
}

data object BarnetHarFlereVerger : Verifiseringsfeil() {
    override val message: String
        get() = "Barnet har flere verger"
}

data object BarnetHarVergemaal : Verifiseringsfeil() {
    override val message: String
        get() = "Barn har vergemål eller framtidsfullmakt, støtte for det er deaktivert"
}

data class PDLException(val kilde: Throwable) : Verifiseringsfeil() {
    override val message: String?
        get() = kilde.message
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

data class BostedslandErIkkeNorge(val land: VurdertBostedsland) : Verifiseringsfeil() {
    override val message: String
        get() = "Bostedsland er ikke Norge, men $land"
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

data object UkjentForelder : Verifiseringsfeil() {
    override val message: String
        get() = "Søker har ukjent forelder"
}

data object BrukerManglerAdresse : Verifiseringsfeil() {
    override val message: String
        get() = "Bruker mangler adresse i PDL"
}

data object BeregningsmetodeIkkeNasjonal : Verifiseringsfeil() {
    override val message: String
        get() = "Sak har brukt annen beregningsmetode enn Nasjonal"
}
