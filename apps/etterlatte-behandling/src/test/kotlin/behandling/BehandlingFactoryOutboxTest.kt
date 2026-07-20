package no.nav.etterlatte.behandling

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.prosessering.ProsesseringToggles
import no.nav.etterlatte.prosessering.ekteBehandlingMottakType
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.util.UUID

/**
 * Wiring-test for den ekte outbox-koblingen (PoC Fase 4e, Steg 2a): når en førstegangsbehandling
 * opprettes og [ProsesseringToggles.EKTE_OUTBOX] er på, skal [BehandlingFactory] legge nøyaktig
 * én [ekteBehandlingMottakType]-task i kø via produsenten. Er toggelen av (eller produsenten ikke
 * wiret), skal ingen task køes.
 *
 * At task-en faktisk committer/ruller tilbake atomisk med behandlings-skrivet er bevist separat i
 * `EkteOutboxBehandlingstransaksjonIntegrationTest`; her verifiseres kun selve gatingen/kallet.
 */
internal class BehandlingFactoryOutboxTest {
    private val sakId = SakId(1)
    private val sak = Sak("Soeker", SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr, null, false)
    private val datoNaa = Tidspunkt.now().toLocalDatetimeUTC()

    private val behandling =
        Foerstegangsbehandling(
            id = UUID.randomUUID(),
            sak = sak,
            behandlingOpprettet = datoNaa,
            sistEndret = datoNaa,
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = null,
            gyldighetsproeving = null,
            virkningstidspunkt = null,
            utlandstilknytning = null,
            boddEllerArbeidetUtlandet = null,
            kommerBarnetTilgode = null,
            vedtaksloesning = Vedtaksloesning.GJENNY,
            sendeBrev = true,
        )

    private val persongalleri =
        Persongalleri("Soeker", "Innsender", emptyList(), listOf("Avdoed"), listOf("Gjenlevende"))

    private val user = mockk<SaksbehandlerMedEnheterOgRoller>(relaxed = true)
    private val behandlingDao = mockk<BehandlingDao>(relaxUnitFun = true)
    private val hendelseDao = mockk<HendelseDao>(relaxUnitFun = true)
    private val sakService = mockk<SakService>()
    private val grunnlagService = mockk<GrunnlagService>(relaxUnitFun = true)
    private val oppgaveService = mockk<OppgaveService>(relaxUnitFun = true)
    private val produsent = mockk<efterlatte.prosessering.TaskProdusent>(relaxed = true)

    private val mockOppgave =
        opprettNyOppgaveMedReferanseOgSak(
            "behandling",
            sak,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING,
            null,
        )

    private fun byggFactory(toggleAktiv: Boolean): BehandlingFactory {
        val featureToggleService =
            mockk<FeatureToggleService> {
                every { isEnabled(ProsesseringToggles.EKTE_OUTBOX, false) } returns toggleAktiv
            }
        return BehandlingFactory(
            oppgaveService = oppgaveService,
            grunnlagService = grunnlagService,
            revurderingService = mockk(relaxed = true),
            gyldighetsproevingService = mockk(relaxed = true),
            sakService = sakService,
            behandlingDao = behandlingDao,
            hendelseDao = hendelseDao,
            behandlingHendelser = mockk(relaxed = true),
            kommerBarnetTilGodeService = mockk(relaxed = true),
            vilkaarsvurderingService = mockk(relaxed = true),
            behandlingInfoService = mockk(relaxed = true),
            tilgangsService = mockk(relaxed = true),
            prosesseringTaskProdusent = produsent,
            featureToggleService = featureToggleService,
        )
    }

    @BeforeEach
    fun before() {
        val databaseKontekst =
            object : DatabaseKontekst {
                override fun activeTx(): Connection = mockk(relaxed = true)

                override fun harIntransaction(): Boolean = true

                override fun <T> inTransaction(block: () -> T): T = block()
            }
        Kontekst.set(
            Context(
                user,
                databaseKontekst,
                mockk(relaxed = true),
                mockk<no.nav.etterlatte.libs.ktor.token.Saksbehandler>(relaxed = true),
            ),
        )

        every { sakService.finnSak(sakId) } returns sak
        every { behandlingDao.hentBehandlingerForSak(sakId) } returns emptyList()
        every { behandlingDao.hentBehandling(any()) } returns behandling
        every { grunnlagService.hentOpplysningsgrunnlagForSak(any()) } returns Grunnlag.empty()
        coEvery { grunnlagService.opprettGrunnlag(any(), any()) } returns Unit
        every {
            oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), gruppeId = any(), frist = any())
        } returns mockOppgave
    }

    @Test
    fun `toggle paa gir noeyaktig en ekte outbox-task`() {
        val factory = byggFactory(toggleAktiv = true)

        factory.opprettBehandling(
            sakId = sakId,
            persongalleri = persongalleri,
            mottattDato = datoNaa.toString(),
            kilde = Vedtaksloesning.GJENNY,
            request = factory.hentDataForOpprettBehandling(sakId),
            opprinnelse = BehandlingOpprinnelse.UKJENT,
        )

        verify(exactly = 1) {
            produsent.opprett(any(), ekteBehandlingMottakType, any(), any())
        }
    }

    @Test
    fun `toggle av gir ingen outbox-task`() {
        val factory = byggFactory(toggleAktiv = false)

        factory.opprettBehandling(
            sakId = sakId,
            persongalleri = persongalleri,
            mottattDato = datoNaa.toString(),
            kilde = Vedtaksloesning.GJENNY,
            request = factory.hentDataForOpprettBehandling(sakId),
            opprinnelse = BehandlingOpprinnelse.UKJENT,
        )

        verify(exactly = 0) {
            produsent.opprett(any(), any(), any(), any())
        }
    }
}
