package no.nav.etterlatte.tilgangsstyring

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.person.hentPrioritertGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.PersonManglerSak
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory

data class TilgangsUtfall(
    val sattAdressebeskyttelse: Boolean,
    val sattEgenansatt: Boolean,
)

class OppdaterTilgangService(
    private val sakService: SakService,
    private val skjermingKlient: SkjermingKlient,
    private val pdltjenesterKlient: PdlTjenesterKlient,
    private val brukerService: BrukerService,
    private val oppgaveService: OppgaveService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Her skal den høyeste graderingen aldri bli overskredet
     * Fra høyeste til lavest: STRENGT_FORTROLIG_UTLAND/STRENGT_FORTROLIG, FORTROLIG, EGEN_ANSATT
     * NB: Saker uten behandling vil ikke trenge dette
     **/
    fun haandtergraderingOgEgenAnsatt(
        sakId: SakId,
        persongalleri: Persongalleri,
    ): TilgangsUtfall {
        logger.info("Håndterer tilganger for sakid $sakId")
        val sak = sakService.finnSak(sakId) ?: throw PersonManglerSak()
        val alleIdenter = persongalleri.hentAlleIdentifikatorer()

        val identerMedGradering =
            alleIdenter
                .filter { Folkeregisteridentifikator.isValid(it) }
                .map { hentGraderingForIdent(it, sak) }

        if (identerMedGradering.any { it.harAdressebeskyttelse() }) {
            val hoyesteGradering = identerMedGradering.hentPrioritertGradering()
            sakService.oppdaterAdressebeskyttelse(sakId, hoyesteGradering)
            /*
                Hvis det er fotrolig blir det en virtuell enhet og vi kan på forhånd ikke vite hvilken det er
                Med Streng fortrolig vet vi hvilke enheter som kan behandle disse
             */
            if (hoyesteGradering == AdressebeskyttelseGradering.FORTROLIG) {
                val enhet = hentEnhet(fnr = sak.ident, sak = sak)
                val sakMedEnhet = listOf(SakMedEnhet(sakId, enhet))
                sakService.oppdaterEnhetForSaker(sakMedEnhet)
                oppgaveService.oppdaterEnhetForRelaterteOppgaver(sakMedEnhet)
                return TilgangsUtfall(sattAdressebeskyttelse = true, sattEgenansatt = false)
            } else {
                sakService.settEnhetOmAdresseebeskyttet(sak, hoyesteGradering)
                val enhet =
                    when (hoyesteGradering) {
                        AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> Enheter.STRENGT_FORTROLIG_UTLAND
                        AdressebeskyttelseGradering.STRENGT_FORTROLIG -> Enheter.STRENGT_FORTROLIG
                        else -> throw InternfeilException("Feil gradering, kun strengt fortrolig håndteres her")
                    }
                oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(SakMedEnhet(sakId, enhet.enhetNr)))
                return TilgangsUtfall(sattAdressebeskyttelse = true, sattEgenansatt = false)
            }
        } else {
            val egenAnsattSkjerming = alleIdenter.map { fnr -> sjekkOmIdentErSkjermet(fnr) }
            sakService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.UGRADERT)
            if (egenAnsattSkjerming.any { it }) {
                sakService.markerSakerMedSkjerming(listOf(sakId), true)
                val sakMedEnhet = listOf(SakMedEnhet(sakId, Enheter.EGNE_ANSATTE.enhetNr))
                sakService.oppdaterEnhetForSaker(sakMedEnhet)
                oppgaveService.oppdaterEnhetForRelaterteOppgaver(sakMedEnhet)
                return TilgangsUtfall(sattAdressebeskyttelse = false, sattEgenansatt = true)
            } else {
                val enhet = hentEnhet(fnr = sak.ident, sak = sak)
                val sakMedEnhet = listOf(SakMedEnhet(sakId, enhet))
                sakService.oppdaterEnhetForSaker(sakMedEnhet)
                sakService.markerSakerMedSkjerming(listOf(sakId), false)
                oppgaveService.oppdaterEnhetForRelaterteOppgaver(sakMedEnhet)
                return TilgangsUtfall(sattAdressebeskyttelse = false, sattEgenansatt = false)
            }
        }
    }

    fun fjernSkjermingFraSak(
        sak: Sak,
        fnr: String,
    ) {
        val enhet = hentEnhet(fnr = fnr, sak = sak)
        val sakerMedNyEnhet = SakMedEnhet(sak.id, enhet)
        sakService.oppdaterEnhetForSaker(listOf(sakerMedNyEnhet))
        sakService.markerSakerMedSkjerming(listOf(sak.id), false)
        oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(sakerMedNyEnhet))
    }

    private fun hentEnhet(
        fnr: String,
        sak: Sak,
    ): Enhetsnummer =
        brukerService
            .finnEnhetForPersonOgTema(
                fnr,
                sak.sakType.tema,
                sak.sakType,
            ).enhetNr

    private fun hentGraderingForIdent(
        fnr: String,
        sak: Sak,
    ): AdressebeskyttelseGradering =
        runBlocking {
            pdltjenesterKlient.hentAdressebeskyttelseForPerson(
                HentAdressebeskyttelseRequest(
                    PersonIdent(fnr),
                    sak.sakType,
                ),
            )
        }

    private fun sjekkOmIdentErSkjermet(fnr: String): Boolean =
        runBlocking {
            skjermingKlient.personErSkjermet(fnr)
        }
}
