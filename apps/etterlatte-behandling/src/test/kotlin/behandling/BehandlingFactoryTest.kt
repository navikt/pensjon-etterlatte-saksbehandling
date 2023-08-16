package behandling

import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingFactory
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingServiceFeatureToggle
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.behandling.revurdering.RevurderingServiceImpl
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.oppgaveNy.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.oppgaveny.OppgaveServiceNy
import no.nav.etterlatte.revurdering
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakServiceFeatureToggle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.YearMonth
import java.util.*

class BehandlingFactoryTest {

    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()
    private val sakDaoMock = mockk<SakDao>()
    private val behandlingDaoMock = mockk<BehandlingDao>()
    private val hendelseDaoMock = mockk<HendelseDao>()
    private val behandlingHendelserKafkaProducerMock = mockk<BehandlingHendelserKafkaProducer>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val grunnlagsendringshendelseDao = mockk<GrunnlagsendringshendelseDao>()
    private val grunnlagService = mockk<GrunnlagService>()
    private val oppgaveService = mockk<OppgaveServiceNy>()
    private val behandlingService = mockk<BehandlingService>()
    private val mockOppgave = opprettNyOppgaveMedReferanseOgSak(
        "behandling",
        Sak("ident", SakType.BARNEPENSJON, 1L, Enheter.AALESUND.enhetNr),
        OppgaveKilde.BEHANDLING,
        OppgaveType.FOERSTEGANGSBEHANDLING,
        null
    )
    private val kommerBarnetTilGodeService = mockk<KommerBarnetTilGodeService>().also {
        every { it.hentKommerBarnetTilGode(any()) } returns null
    }
    private val revurderingDao = mockk<RevurderingDao>()
    private val revurderingService = RevurderingServiceImpl(
        oppgaveService,
        grunnlagService,
        behandlingHendelserKafkaProducerMock,
        featureToggleService,
        behandlingDaoMock,
        hendelseDaoMock,
        grunnlagsendringshendelseDao,
        kommerBarnetTilGodeService,
        revurderingDao,
        behandlingService,
        true
    )
    private val behandlingFactory = BehandlingFactory(
        oppgaveService,
        grunnlagService,
        revurderingService,
        sakDaoMock,
        behandlingDaoMock,
        hendelseDaoMock,
        behandlingHendelserKafkaProducerMock,
        featureToggleService
    )

    @BeforeEach
    fun before() {
        Kontekst.set(
            Context(
                user,
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(gjenbruk: Boolean, block: () -> T): T {
                        return block()
                    }
                }
            )
        )
    }

    @AfterEach
    fun after() {
        confirmVerified(sakDaoMock, behandlingDaoMock, hendelseDaoMock, behandlingHendelserKafkaProducerMock)
        clearAllMocks()
    }

    @Test
    fun startBehandling() {
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()
        val datoNaa = Tidspunkt.now().toLocalDatetimeUTC()

        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false
        every { featureToggleService.isEnabled(SakServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val opprettetBehandling = Foerstegangsbehandling(
            id = UUID.randomUUID(),
            sak = Sak(
                ident = "Soeker",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.defaultEnhet.enhetNr
            ),
            behandlingOpprettet = datoNaa,
            sistEndret = datoNaa,
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        val persongalleri = Persongalleri(
            "Soeker",
            "Innsender",
            emptyList(),
            listOf("Avdoed"),
            listOf("Gjenlevende")
        )

        every { sakDaoMock.hentSak(any()) } returns opprettetBehandling.sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns opprettetBehandling
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every { behandlingDaoMock.lagreGyldighetsproving(any()) } returns Unit
        every { behandlingDaoMock.alleBehandlingerISak(any()) } returns emptyList()
        every { behandlingDaoMock.lagreStatus(any(), any(), any()) } returns Unit
        every { behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(any(), any()) } returns Unit
        every { grunnlagService.leggInnNyttGrunnlag(any(), any()) } returns Unit
        every {
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendSoeknad(any(), any())
        } returns mockOppgave

        val resultat = behandlingFactory.opprettBehandling(
            1,
            persongalleri,
            datoNaa.toString(),
            Vedtaksloesning.GJENNY
        )!!

        Assertions.assertEquals(opprettetBehandling, resultat)
        Assertions.assertEquals(opprettetBehandling.sak, resultat.sak)
        Assertions.assertEquals(opprettetBehandling.id, resultat.id)
        Assertions.assertEquals(opprettetBehandling.behandlingOpprettet, resultat.behandlingOpprettet)
        Assertions.assertEquals(1, behandlingOpprettes.captured.sakId)
        Assertions.assertEquals(behandlingHentes.captured, behandlingOpprettes.captured.id)

        verify(exactly = 1) {
            sakDaoMock.hentSak(any())
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.alleBehandlingerISak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(any(), BehandlingHendelseType.OPPRETTET)
            grunnlagService.leggInnNyttGrunnlag(any(), any())
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendSoeknad(any(), any())
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendSoeknad(any(), any())
        }
    }

    @Test
    fun `skal opprette kun foerstegangsbehandling hvis det ikke finnes noen tidligere behandlinger`() {
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()
        val datoNaa = Tidspunkt.now().toLocalDatetimeUTC()

        every { featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false
        every { featureToggleService.isEnabled(SakServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val opprettetBehandling = Foerstegangsbehandling(
            id = UUID.randomUUID(),
            sak = Sak(
                ident = "Soeker",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.defaultEnhet.enhetNr
            ),
            behandlingOpprettet = datoNaa,
            sistEndret = datoNaa,
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        val persongalleri = Persongalleri(
            "Soeker",
            "Innsender",
            emptyList(),
            listOf("Avdoed"),
            listOf("Gjenlevende")
        )

        every { sakDaoMock.hentSak(any()) } returns opprettetBehandling.sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns opprettetBehandling
        every { behandlingDaoMock.alleBehandlingerISak(any()) } returns emptyList()
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every { behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(any(), any()) } returns Unit
        every { grunnlagService.leggInnNyttGrunnlag(any(), any()) } returns Unit
        every {
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendSoeknad(any(), any())
        } returns mockOppgave

        val foerstegangsbehandling = behandlingFactory.opprettBehandling(
            1,
            persongalleri,
            datoNaa.toString(),
            Vedtaksloesning.GJENNY
        )!!

        Assertions.assertTrue(foerstegangsbehandling is Foerstegangsbehandling)

        verify(exactly = 1) {
            sakDaoMock.hentSak(any())
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.alleBehandlingerISak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(any(), any())
            grunnlagService.leggInnNyttGrunnlag(any(), any())
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendSoeknad(any(), any())
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendSoeknad(any(), any())
        }
    }

    @Test
    fun `skal avbryte behandling hvis under behandling og opprette en ny`() {
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()
        val datoNaa = Tidspunkt.now().toLocalDatetimeUTC()

        every {
            featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false)
        } returns false
        every { featureToggleService.isEnabled(SakServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val underArbeidBehandling = Foerstegangsbehandling(
            id = UUID.randomUUID(),
            sak = Sak(
                ident = "Soeker",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.defaultEnhet.enhetNr
            ),
            behandlingOpprettet = datoNaa,
            sistEndret = datoNaa,
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        val persongalleri = Persongalleri(
            "Soeker",
            "Innsender",
            emptyList(),
            listOf("Avdoed"),
            listOf("Gjenlevende")
        )

        every { sakDaoMock.hentSak(any()) } returns underArbeidBehandling.sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns underArbeidBehandling
        every {
            behandlingDaoMock.alleBehandlingerISak(any())
        } returns emptyList()
        every { behandlingDaoMock.lagreStatus(any(), any(), any()) } returns Unit
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every { behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(any(), any()) } returns Unit
        every { grunnlagService.leggInnNyttGrunnlag(any(), any()) } returns Unit
        every {
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendSoeknad(any(), any())
        } returns mockOppgave
        every {
            oppgaveService.avbrytAapneOppgaverForBehandling(any())
        } just runs

        val foerstegangsbehandling = behandlingFactory.opprettBehandling(
            1,
            persongalleri,
            datoNaa.toString(),
            Vedtaksloesning.GJENNY
        )!!

        Assertions.assertTrue(foerstegangsbehandling is Foerstegangsbehandling)

        every {
            behandlingDaoMock.alleBehandlingerISak(any())
        } returns listOf(underArbeidBehandling)

        val nyfoerstegangsbehandling = behandlingFactory.opprettBehandling(
            1,
            persongalleri,
            datoNaa.toString(),
            Vedtaksloesning.GJENNY
        )
        Assertions.assertTrue(nyfoerstegangsbehandling is Foerstegangsbehandling)

        verify(exactly = 2) {
            sakDaoMock.hentSak(any())
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.alleBehandlingerISak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(any(), any())
            grunnlagService.leggInnNyttGrunnlag(any(), any())
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendSoeknad(any(), any())
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendSoeknad(any(), any())
        }
        verify {
            behandlingDaoMock.lagreStatus(any(), BehandlingStatus.AVBRUTT, any())
            oppgaveService.avbrytAapneOppgaverForBehandling(nyfoerstegangsbehandling!!.id.toString())
        }
    }

    @Test
    fun `skal lage ny behandling som revurdering hvis behandling er satt til iverksatt`() {
        val behandlingOpprettes = slot<OpprettBehandling>()
        val behandlingHentes = slot<UUID>()
        val datoNaa = Tidspunkt.now().toLocalDatetimeUTC()

        every {
            featureToggleService.isEnabled(BehandlingServiceFeatureToggle.FiltrerMedEnhetId, false)
        } returns false
        every { featureToggleService.isEnabled(SakServiceFeatureToggle.FiltrerMedEnhetId, false) } returns false

        val nyBehandling = Foerstegangsbehandling(
            id = UUID.randomUUID(),
            sak = Sak(
                ident = "Soeker",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.defaultEnhet.enhetNr
            ),
            behandlingOpprettet = datoNaa,
            sistEndret = datoNaa,
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            kilde = Vedtaksloesning.GJENNY
        )

        val persongalleri = Persongalleri(
            "Soeker",
            "Innsender",
            emptyList(),
            listOf("Avdoed"),
            listOf("Gjenlevende")
        )

        every { sakDaoMock.hentSak(any()) } returns nyBehandling.sak
        every { behandlingDaoMock.opprettBehandling(capture(behandlingOpprettes)) } returns Unit
        every { behandlingDaoMock.hentBehandling(capture(behandlingHentes)) } returns nyBehandling
        every {
            behandlingDaoMock.alleBehandlingerISak(any())
        } returns emptyList()
        every { behandlingDaoMock.lagreStatus(any(), any(), any()) } returns Unit
        every { hendelseDaoMock.behandlingOpprettet(any()) } returns Unit
        every { behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(any(), any()) } returns Unit
        every { grunnlagService.leggInnNyttGrunnlag(any(), any()) } returns Unit
        every {
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendSoeknad(any(), any())
        } returns mockOppgave
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave
        every {
            oppgaveService.tildelSaksbehandler(any(), any())
        } just runs

        val foerstegangsbehandling = behandlingFactory.opprettBehandling(
            1,
            persongalleri,
            datoNaa.toString(),
            Vedtaksloesning.GJENNY
        )!!

        Assertions.assertTrue(foerstegangsbehandling is Foerstegangsbehandling)

        val iverksattBehandlingId = UUID.randomUUID()
        val iverksattBehandling = Foerstegangsbehandling(
            id = iverksattBehandlingId,
            sak = Sak(
                ident = "Soeker",
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.defaultEnhet.enhetNr
            ),
            behandlingOpprettet = datoNaa,
            sistEndret = datoNaa,
            status = BehandlingStatus.IVERKSATT,
            soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
            gyldighetsproeving = null,
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 1),
                Grunnlagsopplysning.Saksbehandler.create("ident"),
                "begrunnelse"
            ),
            utenlandstilsnitt = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = KommerBarnetTilgode(
                JaNei.JA,
                "",
                Grunnlagsopplysning.Saksbehandler.create("saksbehandler"),
                iverksattBehandlingId
            ),
            kilde = Vedtaksloesning.GJENNY
        )

        every {
            behandlingDaoMock.alleBehandlingerISak(any())
        } returns listOf(iverksattBehandling)

        every { behandlingDaoMock.hentBehandling(any()) } returns revurdering(
            sakId = 1,
            revurderingAarsak = RevurderingAarsak.NY_SOEKNAD,
            enhet = Enheter.defaultEnhet.enhetNr
        )

        val revurderingsBehandling = behandlingFactory.opprettBehandling(
            1,
            persongalleri,
            datoNaa.toString(),
            Vedtaksloesning.GJENNY
        )
        Assertions.assertTrue(revurderingsBehandling is Revurdering)
        verify {
            grunnlagService.leggInnNyttGrunnlag(any(), any())
            oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendSoeknad(any(), any())
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        }
        verify(exactly = 2) {
            sakDaoMock.hentSak(any())
            behandlingDaoMock.hentBehandling(any())
            behandlingDaoMock.opprettBehandling(any())
            hendelseDaoMock.behandlingOpprettet(any())
            behandlingDaoMock.alleBehandlingerISak(any())
            behandlingHendelserKafkaProducerMock.sendMeldingForHendelse(any(), any())
        }
    }
}