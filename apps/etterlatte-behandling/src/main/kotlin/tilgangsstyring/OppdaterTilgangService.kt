package no.nav.etterlatte.tilgangsstyring

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.inTransaction
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
import no.nav.etterlatte.libs.common.sak.SakMedGraderingOgSkjermet
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.PersonManglerSak
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakTilgang
import org.slf4j.LoggerFactory

fun SakMedGraderingOgSkjermet.erSpesialSak(): Boolean {
    val harAdressebeskyttelse =
        this.adressebeskyttelseGradering?.let { gradering ->
            when (gradering) {
                AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> true
                AdressebeskyttelseGradering.STRENGT_FORTROLIG -> true
                AdressebeskyttelseGradering.FORTROLIG -> true
                AdressebeskyttelseGradering.UGRADERT -> false
            }
        } ?: false

    val erEgenansatt =
        this.erSkjermet?.let { skjerming ->
            when (skjerming) {
                true -> true
                false -> false
            }
        } ?: false

    return harAdressebeskyttelse || erEgenansatt
}

class OppdaterTilgangService(
    private val skjermingKlient: SkjermingKlient,
    private val pdltjenesterKlient: PdlTjenesterKlient,
    private val brukerService: BrukerService,
    private val oppgaveService: OppgaveService,
    private val sakSkrivDao: SakSkrivDao,
    private val sakTilgang: SakTilgang,
    private val sakLesDao: SakLesDao,
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
    ) {
        logger.info("Håndterer tilganger for sakid $sakId")
        val sak = inTransaction { sakLesDao.hentSak(sakId) ?: throw PersonManglerSak() }
        val alleIdenter = persongalleri.hentAlleIdentifikatorer()

        val identerMedGradering =
            alleIdenter
                .filter { Folkeregisteridentifikator.isValid(it) }
                .map { hentGraderingForIdent(it, sak) }

        if (identerMedGradering.any { it.harAdressebeskyttelse() }) {
            val hoyesteGradering = identerMedGradering.hentPrioritertGradering()
            sakTilgang.oppdaterAdressebeskyttelse(sakId, hoyesteGradering)
            /*
                Hvis det er fotrolig blir det en virtuell enhet og vi kan på forhånd ikke vite hvilken det er
                Med Streng fortrolig vet vi hvilke enheter som kan behandle disse
             */
            if (hoyesteGradering == AdressebeskyttelseGradering.FORTROLIG) {
                val enhet = hentEnhet(fnr = sak.ident, sak = sak)
                val sakMedEnhet = SakMedEnhet(sakId, enhet)
                sakSkrivDao.oppdaterEnhet(sakMedEnhet)
                oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(sakMedEnhet))
            } else {
                sakTilgang.settEnhetOmAdressebeskyttet(sak, hoyesteGradering)
                val enhet =
                    when (hoyesteGradering) {
                        AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> Enheter.STRENGT_FORTROLIG_UTLAND
                        AdressebeskyttelseGradering.STRENGT_FORTROLIG -> Enheter.STRENGT_FORTROLIG
                        else -> throw InternfeilException("Feil gradering, kun strengt fortrolig håndteres her")
                    }
                oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(SakMedEnhet(sakId, enhet.enhetNr)))
            }
        } else {
            val egenAnsattSkjerming = alleIdenter.map { fnr -> sjekkOmIdentErSkjermet(fnr) }
            sakTilgang.oppdaterAdressebeskyttelse(sakId, identerMedGradering.hentPrioritertGradering())
            if (egenAnsattSkjerming.any { it }) {
                sakTilgang.oppdaterSkjerming(sakId, true)
                val sakMedEnhet = SakMedEnhet(sakId, Enheter.EGNE_ANSATTE.enhetNr)
                sakSkrivDao.oppdaterEnhet(sakMedEnhet)
                oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(sakMedEnhet))
            } else {
                val tilgangSak = sakTilgang.hentGraderingForSak(sakId, HardkodaSystembruker.tilgang)
                /*
                    Vi vil kun tilbakestille en sak som har vært egen anstt, strengt fortrolig(/utland) eller fortrolig
                 */
                if (tilgangSak.erSpesialSak()) {
                    val enhetsNummer = hentEnhet(fnr = sak.ident, sak = sak)
                    val sakMedEnhet = SakMedEnhet(sakId, enhetsNummer)
                    sakSkrivDao.oppdaterEnhet(sakMedEnhet)
                    sakTilgang.oppdaterSkjerming(sakId, false)
                    oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(sakMedEnhet))
                }
            }
        }
    }

    fun fjernSkjermingFraSak(
        sak: Sak,
        fnr: String,
    ) {
        val enhet = hentEnhet(fnr = fnr, sak = sak)
        val sakerMedNyEnhet = SakMedEnhet(sak.id, enhet)
        sakSkrivDao.oppdaterEnhet(sakerMedNyEnhet)
        sakTilgang.oppdaterSkjerming(sak.id, false)
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
