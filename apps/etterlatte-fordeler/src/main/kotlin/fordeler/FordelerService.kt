package no.nav.etterlatte.fordeler

import fordeler.FordelerFeatureToggle
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingKlient
import no.nav.etterlatte.fordeler.FordelerResultat.GyldigForBehandling
import no.nav.etterlatte.fordeler.FordelerResultat.IkkeGyldigForBehandling
import no.nav.etterlatte.fordeler.FordelerResultat.UgyldigHendelse
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.Barnepensjon
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PersonType
import no.nav.etterlatte.libs.common.pdl.AkseptererIkkePersonerUtenIdentException
import no.nav.etterlatte.libs.common.pdl.FantIkkePersonException
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.tidspunkt.utcKlokke
import no.nav.etterlatte.pdltjenester.PdlTjenesterKlient
import no.nav.etterlatte.pdltjenester.PersonFinnesIkkeException
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

data class Fordeling(
    val soeknadId: Long,
    val fordeltTil: Vedtaksloesning,
    val kriterier: List<FordelerKriterie>,
    val gradering: AdressebeskyttelseGradering? = null,
)

sealed class FordelerResultat {
    class GyldigForBehandling(val gradering: AdressebeskyttelseGradering? = null) : FordelerResultat()

    class UgyldigHendelse(val message: String) : FordelerResultat()

    class IkkeGyldigForBehandling(val ikkeOppfylteKriterier: List<FordelerKriterie>) : FordelerResultat()

    class TrengerManuellJournalfoering(val melding: String) : FordelerResultat()
}

class FordelerService(
    private val fordelerKriterier: FordelerKriterier,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val fordelerRepository: FordelerRepository,
    private val behandlingKlient: BehandlingKlient,
    private val klokke: Clock = utcKlokke(),
    private val maxFordelingTilGjenny: Long,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun sjekkGyldighetForBehandling(event: FordelerEvent): FordelerResultat {
        if (ugyldigHendelse(event)) {
            return UgyldigHendelse(
                "Hendelsen er ikke lenger gyldig (${hendelseGyldigTil(event)})",
            )
        }
        val alleScenarierTillates = alleScenarierTillates(featureToggleService)

        return try {
            fordelSoeknad(event, alleScenarierTillates).let {
                when (it.fordeltTil) {
                    Vedtaksloesning.GJENNY -> GyldigForBehandling(it.gradering)
                    Vedtaksloesning.PESYS -> IkkeGyldigForBehandling(it.kriterier)
                }
            }
        } catch (e: AkseptererIkkePersonerUtenIdentException) {
            logger.warn(
                "Fikk en familierelasjon som mangler ident fra PDL. Disse tilfellene støtter vi ikke per nå." +
                    " Se sikkerlogg for detaljer",
            )
            sikkerLogg.info("Søknad ${event.soeknadId} har en familierelasjon som mangler ident", e)

            if (featureToggleService.isEnabled(FordelerFeatureToggle.ManuellJournalfoering, false)) {
                FordelerResultat.TrengerManuellJournalfoering(e.detail)
            } else {
                IkkeGyldigForBehandling(listOf(FordelerKriterie.FAMILIERELASJON_MANGLER_IDENT))
            }
        } catch (e: PersonFinnesIkkeException) {
            UgyldigHendelse("Person fra søknaden med fnr=${e.fnr} finnes ikke i PDL")
        }
    }

    private fun fordelSoeknad(
        event: FordelerEvent,
        tillatAlleScenarier: Boolean,
    ): Fordeling {
        val eksisterendeFordeling = finnEksisterendeFordeling(event.soeknadId)
        logger.debug("Eksisterende fordeling: ${eksisterendeFordeling?.fordeltTil?.name}")
        if (eksisterendeFordeling == null) {
            logger.debug("Finner nyFordeling for søknad-ID ${event.soeknadId}, og tillatAlle: $tillatAlleScenarier")
        }
        return eksisterendeFordeling
            ?: nyFordeling(event, tillatAlleScenarier).apply { lagre() }
    }

    private fun finnEksisterendeFordeling(soeknadId: Long): Fordeling? =
        fordelerRepository
            .finnFordeling(soeknadId)
            ?.let {
                Fordeling(
                    it.soeknadId,
                    Vedtaksloesning.valueOf(it.fordeling),
                    fordelerRepository.finnKriterier(it.soeknadId).map { FordelerKriterie.valueOf(it) },
                )
            }

    private fun Fordeling.lagre() {
        fordelerRepository.lagreFordeling(FordeltTransient(soeknadId, fordeltTil.name, kriterier.map { it.name }))
    }

    private fun nyFordeling(
        event: FordelerEvent,
        tillatAlleScenarier: Boolean,
    ): Fordeling =
        runBlocking {
            val soeknad: Barnepensjon = event.soeknad
            val barn = pdlTjenesterKlient.hentPerson(hentBarnRequest(soeknad)) ?: throw FantIkkePersonException()
            val avdoed =
                pdlTjenesterKlient.hentPerson(hentAvdoedRequest(soeknad))
                    ?: return@runBlocking Fordeling(
                        event.soeknadId,
                        Vedtaksloesning.PESYS,
                        listOf(FordelerKriterie.AVDOED_FINNES_IKKE_I_PDL),
                    )

            val gjenlevende =
                if (harGjenlevendeForeldre(soeknad)) {
                    logger.warn("Henter gjenlevende person...")
                    pdlTjenesterKlient.hentPerson(hentGjenlevendeRequest(soeknad))
                        ?: throw FantIkkePersonException()
                } else {
                    null
                }

            fordelerKriterier.sjekkMotKriterier(barn, avdoed, gjenlevende, soeknad).let {
                if ((it.kandidat || tillatAlleScenarier) &&
                    fordelerRepository.antallFordeltTil(Vedtaksloesning.GJENNY.name) < maxFordelingTilGjenny
                ) {
                    val adressebeskyttetPerson =
                        finnAdressebeskyttetPerson(listOfNotNull(barn, avdoed, gjenlevende))
                    Fordeling(
                        event.soeknadId,
                        Vedtaksloesning.GJENNY,
                        emptyList(),
                        adressebeskyttetPerson?.adressebeskyttelse,
                    )
                } else {
                    Fordeling(event.soeknadId, Vedtaksloesning.PESYS, it.forklaring)
                }
            }
        }

    private fun alleScenarierTillates(featureToggleService: FeatureToggleService) =
        featureToggleService.isEnabled(FordelerFeatureToggle.TillatAlleScenarier, false)

    private fun finnAdressebeskyttetPerson(personer: List<Person>): Person? {
        return personer.firstOrNull {
            it.adressebeskyttelse != AdressebeskyttelseGradering.UGRADERT
        }
    }

    private fun ugyldigHendelse(event: FordelerEvent) = event.hendelseGyldigTil.isBefore(OffsetDateTime.now(klokke))

    private fun hendelseGyldigTil(event: FordelerEvent) = event.hendelseGyldigTil.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    private fun harGjenlevendeForeldre(soeknad: Barnepensjon) =
        soeknad.foreldre.any {
            it.type == PersonType.GJENLEVENDE_FORELDER
        }

    private fun hentGjenlevendeRequest(soeknad: Barnepensjon) =
        HentPersonRequest(
            foedselsnummer =
                soeknad.foreldre.first {
                    it.type == PersonType.GJENLEVENDE_FORELDER
                }.foedselsnummer.svar.toFolkeregisteridentifikator(),
            rolle = PersonRolle.GJENLEVENDE,
            saktype = SakType.BARNEPENSJON,
        )

    private fun hentAvdoedRequest(soeknad: Barnepensjon) =
        HentPersonRequest(
            foedselsnummer =
                soeknad.foreldre.first {
                    it.type == PersonType.AVDOED
                }.foedselsnummer.svar.toFolkeregisteridentifikator(),
            rolle = PersonRolle.AVDOED,
            saktype = SakType.BARNEPENSJON,
        )

    private fun hentBarnRequest(soeknad: Barnepensjon) =
        HentPersonRequest(
            foedselsnummer = soeknad.soeker.foedselsnummer.svar.toFolkeregisteridentifikator(),
            rolle = PersonRolle.BARN,
            saktype = SakType.BARNEPENSJON,
        )

    fun hentSakId(
        fnr: String,
        barnepensjon: SakType,
        gradering: AdressebeskyttelseGradering?,
    ): Long {
        return runBlocking {
            behandlingKlient.hentSak(fnr, barnepensjon, gradering)
        }
    }

    fun opprettOppgave(sakId: Long) {
        return runBlocking { behandlingKlient.opprettOppgave(sakId) }
    }
}

fun Foedselsnummer.toFolkeregisteridentifikator(): Folkeregisteridentifikator {
    return Folkeregisteridentifikator.of(this.value)
}
