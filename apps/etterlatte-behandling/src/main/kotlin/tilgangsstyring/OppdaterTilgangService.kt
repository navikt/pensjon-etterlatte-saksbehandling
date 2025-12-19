package no.nav.etterlatte.tilgangsstyring

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.grunnlagsendring.vergemaalellerfremtidsfullmakt
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.hentPrioritertGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakTilgang
import org.slf4j.LoggerFactory
import java.time.LocalDate

fun Sak.erSpesialSak(): Boolean {
    val harAdressebeskyttelse =
        this.adressebeskyttelse?.let { gradering ->
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

    val harSpesialEnhet = Enheter.erSpesialTilgangsEnheter(this.enhet)

    return harAdressebeskyttelse || erEgenansatt || harSpesialEnhet
}

enum class TilgangToggles(
    private val key: String,
) : FeatureToggle {
    VURDER_SKJERMING_OGSAA_MED_HENSYN_TIL_VERGER("vurder-skjerming-ogsaa-med-hensyn-til-verger"),
    ;

    override fun key(): String = key
}

class OppdaterTilgangService(
    private val skjermingKlient: SkjermingKlient,
    private val pdltjenesterKlient: PdlTjenesterKlient,
    private val brukerService: BrukerService,
    private val oppgaveService: OppgaveService,
    private val sakSkrivDao: SakSkrivDao,
    private val sakTilgang: SakTilgang,
    private val sakLesDao: SakLesDao,
    private val featureToggleService: FeatureToggleService,
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
        grunnlag: Grunnlag?,
    ) {
        logger.info("Håndterer tilganger for sakid $sakId")
        val sak = sakLesDao.hentSak(sakId)
        if (sak == null) {
            logger.warn(
                "Fant ikke sak $sakId som  skulle oppdatert tilgang. Det ligger muligens noe junky data i" +
                    " grunnlagstabellen knyttet til en sak som aldri ble opprettet ferdig.",
            )
            return
        }
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
            sakTilgang.oppdaterAdressebeskyttelse(sakId, identerMedGradering.hentPrioritertGradering())
            if (harRelevanteSkjermedePersoner(sak, persongalleri, grunnlag)) {
                sakTilgang.oppdaterSkjerming(sakId, true)
                val sakMedEnhet = SakMedEnhet(sakId, Enheter.EGNE_ANSATTE.enhetNr)
                sakSkrivDao.oppdaterEnhet(sakMedEnhet)
                oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(sakMedEnhet))
            } else {
                // Vi vil kun tilbakestille en sak som har vært egen ansatt, strengt fortrolig(/utland) eller fortrolig
                if (sak.erSpesialSak()) {
                    val enhetsNummer = hentEnhet(fnr = sak.ident, sak = sak)
                    val sakMedEnhet = SakMedEnhet(sakId, enhetsNummer)
                    sakSkrivDao.oppdaterEnhet(sakMedEnhet)
                    sakTilgang.oppdaterSkjerming(sakId, false)
                    oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(sakMedEnhet))
                }
            }
        }
    }

    private fun harRelevanteSkjermedePersoner(
        sak: Sak,
        persongalleri: Persongalleri,
        grunnlag: Grunnlag?,
    ): Boolean {
        val relevanteIPersongalleri =
            when (sak.sakType == SakType.BARNEPENSJON && persongalleri.soekerErOver18Aar()) {
                true -> listOf(persongalleri.soeker)
                false -> persongalleri.hentAlleIdentifikatorer()
            }
        val relevanteIdenterForSkjerming =
            if (featureToggleService.isEnabled(TilgangToggles.VURDER_SKJERMING_OGSAA_MED_HENSYN_TIL_VERGER, false)) {
                (relevanteIPersongalleri + vergersFoedselsnummer(grunnlag)).distinct()
            } else {
                relevanteIPersongalleri
            }
        return relevanteIdenterForSkjerming.any { fnr -> sjekkOmIdentErSkjermet(fnr) }
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

    private fun Persongalleri.soekerErOver18Aar(): Boolean {
        val fnrSoeker = this.soeker
        val foedselsdatoSoeker =
            krevIkkeNull(
                runBlocking {
                    pdltjenesterKlient.hentPerson(fnrSoeker, PersonRolle.BARN, SakType.BARNEPENSJON)
                }.foedselsdato,
            ) { "Fødselsdato mangler" }

        return LocalDate.now() >= foedselsdatoSoeker.plusYears(18)
    }

    fun vergersFoedselsnummer(grunnlag: Grunnlag?): List<String> =
        grunnlag
            ?.vergemaalellerfremtidsfullmakt(Saksrolle.SOEKER)
            ?.mapNotNull { it.vergeEllerFullmektig.motpartsPersonident?.value }
            ?: emptyList()
}
