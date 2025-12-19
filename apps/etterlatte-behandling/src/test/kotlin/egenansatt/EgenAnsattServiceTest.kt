package no.nav.etterlatte.egenansatt

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.PdltjenesterKlientTest
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.BrukerServiceImpl
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingRequest
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.SkjermingKlientImpl
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.krr.DigitalKontaktinformasjon
import no.nav.etterlatte.krr.KrrKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.PdlFolkeregisterIdentListe
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.skjermet.EgenAnsattSkjermet
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabaseContext
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporingImpl
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.persongalleri
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sak.SakServiceImpl
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakTilgang
import no.nav.etterlatte.sak.SakTilgangImpl
import no.nav.etterlatte.sak.SakendringerDao
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import no.nav.etterlatte.tilgangsstyring.OppdaterTilgangService
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class EgenAnsattServiceTest(
    val dataSource: DataSource,
) {
    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var sakLesDao: SakLesDao
    private lateinit var sakendringerDao: SakendringerDao
    private lateinit var oppgaveRepo: OppgaveDaoImpl
    private lateinit var oppgaveRepoMedSporing: OppgaveDaoMedEndringssporingImpl
    private lateinit var sakService: SakService
    private lateinit var oppgaveService: OppgaveService
    private lateinit var egenAnsattService: EgenAnsattService
    private lateinit var oppdaterTilgangService: OppdaterTilgangService
    private lateinit var user: SaksbehandlerMedEnheterOgRoller
    private lateinit var sakTilgang: SakTilgang
    private val grunnlagService: GrunnlagService = mockk(relaxed = true)
    private val hendelser: BehandlingHendelserKafkaProducer = mockk()
    private val pdlTjenesterKlient = spyk<PdltjenesterKlientTest>()
    private val persongalleri = persongalleri()
    private val saksbehandlerService = mockk<SaksbehandlerService>()

    @BeforeAll
    fun beforeAll() {
        val krrKlient =
            mockk<KrrKlient> {
                coEvery { hentDigitalKontaktinformasjon(any()) } returns
                    DigitalKontaktinformasjon(
                        personident = "",
                        aktiv = true,
                        kanVarsles = true,
                        reservert = false,
                        spraak = "nb",
                        epostadresse = null,
                        mobiltelefonnummer = null,
                        sikkerDigitalPostkasse = null,
                    )
            }
        val norg2Klient = mockk<Norg2Klient>()

        coEvery { grunnlagService.opprettGrunnlag(any(), any()) } just runs
        every { grunnlagService.lagreNyeSaksopplysningerBareSak(any(), any()) } just runs
        every { grunnlagService.grunnlagFinnesForSak(any()) } returns false

        val featureToggleService = mockk<FeatureToggleService>()
        val skjermingKlient = mockk<SkjermingKlientImpl>()
        oppdaterTilgangService = mockk()
        sakLesDao = SakLesDao(ConnectionAutoclosingTest(dataSource))
        sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)))
        sakendringerDao = SakendringerDao(ConnectionAutoclosingTest(dataSource))
        oppgaveRepo = OppgaveDaoImpl(ConnectionAutoclosingTest(dataSource))
        oppgaveRepoMedSporing = OppgaveDaoMedEndringssporingImpl(oppgaveRepo, ConnectionAutoclosingTest(dataSource))
        sakTilgang = SakTilgangImpl(sakSkrivDao, sakLesDao)
        val brukerService = BrukerServiceImpl(pdlTjenesterKlient, norg2Klient)

        sakService =
            spyk(
                SakServiceImpl(
                    sakSkrivDao,
                    sakLesDao,
                    sakendringerDao,
                    skjermingKlient,
                    brukerService,
                    grunnlagService,
                    krrKlient,
                    pdlTjenesterKlient,
                    featureToggleService,
                    oppdaterTilgangService,
                    sakTilgang,
                    mockk(),
                ),
            )
        oppgaveService =
            spyk(
                OppgaveService(oppgaveRepoMedSporing, sakLesDao, mockk(), hendelser, saksbehandlerService),
            )
        egenAnsattService = EgenAnsattService(sakService, grunnlagService, oppdaterTilgangService)

        user = mockk<SaksbehandlerMedEnheterOgRoller>()
        val saksbehandlerMedRoller =
            mockk<SaksbehandlerMedRoller> {
                every { harRolleStrengtFortrolig() } returns false
                every { harRolleEgenAnsatt() } returns true
            }
        every { user.saksbehandlerMedRoller } returns saksbehandlerMedRoller
        every { user.name() } returns "User"

        coEvery { skjermingKlient.personErSkjermet(any()) } returns false
        every {
            pdlTjenesterKlient.hentGeografiskTilknytning(
                any(),
                any(),
            )
        } returns GeografiskTilknytning(kommune = "0301")
        every {
            norg2Klient.hentArbeidsfordelingForOmraadeOgTema(ArbeidsFordelingRequest("EYB", "0301"))
        } returns listOf(ArbeidsFordelingEnhet(Enheter.STEINKJER.navn, Enheter.STEINKJER.enhetNr))

        every { featureToggleService.isEnabled(any(), any()) } returns false
    }

    @BeforeEach
    fun before() {
        nyKontekstMedBrukerOgDatabaseContext(user, DatabaseContextTest(dataSource))
    }

    @Test
    fun sjekkAtSettingAvSkjermingFungererEtterOpprettelseAvSak() {
        val fnr = AVDOED_FOEDSELSNUMMER.value
        val fnr2 = AVDOED2_FOEDSELSNUMMER.value

        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(fnr) } returns
            PdlFolkeregisterIdentListe(
                listOf(
                    PdlIdentifikator.FolkeregisterIdent(Folkeregisteridentifikator.of(fnr)),
                ),
            )
        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(fnr2) } returns
            PdlFolkeregisterIdentListe(
                listOf(
                    PdlIdentifikator.FolkeregisterIdent(Folkeregisteridentifikator.of(fnr)),
                ),
            )
        every { user.enheter() } returns listOf(Enheter.EGNE_ANSATTE.enhetNr)

        every { oppdaterTilgangService.haandtergraderingOgEgenAnsatt(any(), any(), any()) } just Runs
        val bruktSak =
            sakService.finnEllerOpprettSakMedGrunnlag(fnr, SakType.BARNEPENSJON, Enheter.EGNE_ANSATTE.enhetNr)
        sakService.finnEllerOpprettSakMedGrunnlag(fnr2, SakType.BARNEPENSJON, Enheter.EGNE_ANSATTE.enhetNr)
        every { grunnlagService.hentPersongalleri(bruktSak.id) } returns persongalleri

        assertNotNull(sakService.finnSak(fnr, SakType.BARNEPENSJON))
        assertNotNull(sakService.finnSak(fnr2, SakType.BARNEPENSJON))

        val egenAnsattSkjermet = EgenAnsattSkjermet(fnr, Tidspunkt.now(), true)
        egenAnsattService.haandterSkjerming(egenAnsattSkjermet)

        verify(exactly = 3) { oppdaterTilgangService.haandtergraderingOgEgenAnsatt(bruktSak.id, any(), any()) }
    }
}
