package no.nav.etterlatte.egenansatt

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.BrukerServiceImpl
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.skjermet.EgenAnsattSkjermet
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporingImpl
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sak.SakServiceImpl
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class EgenAnsattServiceTest(val dataSource: DataSource) {
    private val sikkerLogg: Logger = sikkerlogger()

    private lateinit var sakRepo: SakDao
    private lateinit var oppgaveRepo: OppgaveDaoImpl
    private lateinit var oppgaveRepoMedSporing: OppgaveDaoMedEndringssporingImpl
    private lateinit var sakService: SakService
    private lateinit var oppgaveService: OppgaveService
    private lateinit var egenAnsattService: EgenAnsattService
    private lateinit var user: SaksbehandlerMedEnheterOgRoller
    private val hendelser: BehandlingHendelserKafkaProducer = mockk()

    @BeforeAll
    fun beforeAll() {
        val pdltjenesterKlient = mockk<PdlTjenesterKlient>()
        val norg2Klient = mockk<Norg2Klient>()
        val featureToggleService = mockk<FeatureToggleService>()
        val skjermingKlient = mockk<SkjermingKlient>()
        sakRepo = SakDao(ConnectionAutoclosingTest(dataSource))
        oppgaveRepo = OppgaveDaoImpl(ConnectionAutoclosingTest(dataSource))
        oppgaveRepoMedSporing = OppgaveDaoMedEndringssporingImpl(oppgaveRepo, ConnectionAutoclosingTest(dataSource))
        val brukerService = BrukerServiceImpl(pdltjenesterKlient, norg2Klient)
        sakService =
            spyk(
                SakServiceImpl(sakRepo, skjermingKlient, brukerService),
            )
        oppgaveService =
            spyk(
                OppgaveService(oppgaveRepoMedSporing, sakRepo, hendelser),
            )
        egenAnsattService = EgenAnsattService(sakService, oppgaveService, sikkerLogg, brukerService)

        user = mockk<SaksbehandlerMedEnheterOgRoller>()

        every { user.name() } returns "User"

        coEvery { skjermingKlient.personErSkjermet(any()) } returns false
        every { pdltjenesterKlient.hentGeografiskTilknytning(any(), any()) } returns GeografiskTilknytning(kommune = "0301")
        every {
            norg2Klient.hentEnheterForOmraade("EYB", "0301")
        } returns listOf(ArbeidsFordelingEnhet(Enheter.STEINKJER.navn, Enheter.STEINKJER.enhetNr))

        every { featureToggleService.isEnabled(any(), any()) } returns false
    }

    @BeforeEach
    fun before() {
        Kontekst.set(
            Context(
                user,
                DatabaseContextTest(dataSource),
            ),
        )
    }

    @Test
    fun sjekkAtSettingAvSkjermingFungererEtterOpprettelseAvSak() {
        every { user.enheter() } returns listOf(Enheter.EGNE_ANSATTE.enhetNr)

        val fnr = AVDOED_FOEDSELSNUMMER.value
        sakService.finnEllerOpprettSak(fnr, SakType.BARNEPENSJON, overstyrendeEnhet = Enheter.EGNE_ANSATTE.enhetNr)
        val fnr2 = AVDOED2_FOEDSELSNUMMER.value
        sakService.finnEllerOpprettSak(fnr2, SakType.BARNEPENSJON, overstyrendeEnhet = Enheter.EGNE_ANSATTE.enhetNr)

        assertNotNull(sakService.finnSak(fnr, SakType.BARNEPENSJON))
        assertNotNull(sakService.finnSak(fnr2, SakType.BARNEPENSJON))

        val egenAnsattSkjermet = EgenAnsattSkjermet(fnr, Tidspunkt.now(), true)
        egenAnsattService.haandterSkjerming(egenAnsattSkjermet)

        verify { sakService.markerSakerMedSkjerming(any(), any()) }
    }
}
