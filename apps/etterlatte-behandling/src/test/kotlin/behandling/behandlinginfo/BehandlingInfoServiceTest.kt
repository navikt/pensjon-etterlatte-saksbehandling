package no.nav.etterlatte.behandling.behandlinginfo

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Brevutfall
import no.nav.etterlatte.libs.common.behandling.Feilutbetaling
import no.nav.etterlatte.libs.common.behandling.FeilutbetalingValg
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth
import java.util.UUID
import java.util.UUID.randomUUID

internal class BehandlingInfoServiceTest {
    private val behandlingInfoDao: BehandlingInfoDao = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val behandlingsstatusService: BehandlingStatusService = mockk()
    private val behandlingInfoService: BehandlingInfoService =
        BehandlingInfoService(behandlingInfoDao, behandlingService, behandlingsstatusService)

    companion object {
        val bruker = Saksbehandler("token", "ident", null)
    }

    @Test
    fun `Skal oppdatere behandlingsstatus hvis endret barnepensjon`() {
        val behandlingId = randomUUID()
        val brevutfall = brevutfall(behandlingId)
        val etterbetaling = etterbetaling(behandlingId = behandlingId)
        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)
        every { behandlingInfoDao.lagreBrevutfall(any()) } returns mockk()
        every { behandlingInfoDao.lagreEtterbetaling(any()) } returns mockk()
        every { behandlingsstatusService.settBeregnet(any(), any(), any()) } returns Unit

        behandlingInfoService.lagreBrevutfallOgEtterbetaling(
            behandlingId = behandlingId,
            brukerTokenInfo = bruker,
            opphoer = false,
            brevutfall = brevutfall,
            etterbetaling = etterbetaling,
        )

        verify {
            behandlingInfoDao.lagreBrevutfall(brevutfall)
            behandlingInfoDao.lagreEtterbetaling(etterbetaling)
            behandlingsstatusService.settBeregnet(behandlingId, bruker, false)
        }
    }

    @Test
    fun `Etterbetaling skal oppdatere behandlingsstatus hvis endret omstillingstoenad`() {
        val behandlingId = randomUUID()
        val brevutfall = brevutfall(behandlingId)
        val etterbetaling = etterbetaling(behandlingId = behandlingId)

        every { behandlingService.hentBehandling(any()) } returns
            behandling(
                behandlingId,
                BehandlingType.FØRSTEGANGSBEHANDLING,
                SakType.OMSTILLINGSSTOENAD,
                BehandlingStatus.AVKORTET,
            )
        every { behandlingInfoDao.lagreBrevutfall(any()) } returns mockk()
        every { behandlingInfoDao.lagreEtterbetaling(any()) } returns mockk()
        every { behandlingsstatusService.settAvkortet(any(), any(), any()) } returns Unit

        behandlingInfoService.lagreBrevutfallOgEtterbetaling(
            behandlingId = behandlingId,
            brukerTokenInfo = bruker,
            opphoer = false,
            brevutfall = brevutfall,
            etterbetaling = etterbetaling,
        )

        verify {
            behandlingInfoDao.lagreBrevutfall(brevutfall)
            behandlingInfoDao.lagreEtterbetaling(etterbetaling)
            behandlingsstatusService.settAvkortet(behandlingId, bruker, false)
        }
    }

    @Test
    fun `skal feile ved opprettelse av brevutfall hvis behandling ikke kan endres`() {
        val behandlingId = randomUUID()

        every { behandlingService.hentBehandling(any()) } returns
            behandling(
                behandlingId = behandlingId,
                behandlingStatus = BehandlingStatus.FATTET_VEDTAK,
            )

        assertThrows<BrevutfallException.BehandlingKanIkkeEndres> {
            behandlingInfoService.lagreBrevutfallOgEtterbetaling(
                behandlingId = behandlingId,
                brukerTokenInfo = bruker,
                opphoer = false,
                brevutfall = brevutfall(behandlingId),
                etterbetaling = null,
            )
        }
    }

    @Test
    fun `skal feile ved opprettelse av brevutfall hvis feilutbetaling ikke er satt`() {
        val behandlingId = randomUUID()

        every { behandlingService.hentBehandling(any()) } returns
            behandling(
                behandlingId = behandlingId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                behandlingType = BehandlingType.REVURDERING,
                behandlingStatus = BehandlingStatus.AVKORTET,
            )
        every { behandlingInfoDao.lagreBrevutfall(any()) } returns mockk()
        every { behandlingInfoDao.lagreEtterbetaling(any()) } returns mockk()
        every { behandlingsstatusService.settAvkortet(any(), any(), any()) } returns Unit

        assertThrows<BrevutfallException.FeilutbetalingIkkeSatt> {
            behandlingInfoService.lagreBrevutfallOgEtterbetaling(
                behandlingId = behandlingId,
                brukerTokenInfo = bruker,
                opphoer = false,
                brevutfall = brevutfall(behandlingId).copy(feilutbetaling = null),
                etterbetaling = etterbetaling(behandlingId = behandlingId),
            )
        }
    }

    @Test
    fun `skal feile ved opprettelse av etterbetaling hvis etterbetaling fra-dato er foer virkningstidspunkt`() {
        val behandlingId = randomUUID()
        val etterbetaling =
            etterbetaling(
                behandlingId = behandlingId,
                fom = YearMonth.of(2022, 12),
                tom = YearMonth.of(2023, 1),
            )

        every { behandlingInfoDao.hentEtterbetaling(any()) } returns etterbetaling

        assertThrows<EtterbetalingException.EtterbetalingFraDatoErFoerVirk> {
            behandlingInfoService.lagreEtterbetaling(behandling(behandlingId), etterbetaling)
        }
    }

    @Test
    fun `skal slette etterbetaling hvis den er null og det allerede finnes en etterbetaling`() {
        val behandlingId = randomUUID()

        every { behandlingInfoDao.hentEtterbetaling(any()) } returns etterbetaling(behandlingId)
        every { behandlingInfoDao.slettEtterbetaling(any()) } returns 1

        behandlingInfoService.lagreEtterbetaling(behandling(behandlingId), null)

        verify { behandlingInfoDao.slettEtterbetaling(behandlingId) }
    }

    @Test
    fun `skal lagre brevutfall med kun feilutbetaling ved opphoer av omstillingsstoenad`() {
        val behandlingId = randomUUID()

        every { behandlingService.hentBehandling(any()) } returns
            behandling(
                behandlingId = behandlingId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                behandlingType = BehandlingType.REVURDERING,
                behandlingStatus = BehandlingStatus.VILKAARSVURDERT,
                revurderingaarsak = Revurderingaarsak.SIVILSTAND,
            )
        every { behandlingInfoDao.lagreBrevutfall(any()) } returns mockk()
        every { behandlingsstatusService.settVilkaarsvurdert(any(), any(), any()) } returns Unit
        every { behandlingInfoDao.hentEtterbetaling(any()) } returns null

        behandlingInfoService.lagreBrevutfallOgEtterbetaling(
            behandlingId = behandlingId,
            brukerTokenInfo = bruker,
            opphoer = true,
            brevutfall = brevutfall(behandlingId).copy(aldersgruppe = null),
            etterbetaling = null,
        )

        verify {
            behandlingInfoDao.lagreBrevutfall(any())
            behandlingsstatusService.settVilkaarsvurdert(any(), any(), any())
        }
    }

    @Test
    fun `skal lagre brevutfall med kun feilutbetaling og aldergruppe ved opphoer av barnepensjon`() {
        val behandlingId = randomUUID()

        every { behandlingService.hentBehandling(any()) } returns
            behandling(
                behandlingId = behandlingId,
                sakType = SakType.BARNEPENSJON,
                behandlingType = BehandlingType.REVURDERING,
                behandlingStatus = BehandlingStatus.VILKAARSVURDERT,
                revurderingaarsak = Revurderingaarsak.ADOPSJON,
            )
        every { behandlingInfoDao.lagreBrevutfall(any()) } returns mockk()
        every { behandlingsstatusService.settVilkaarsvurdert(any(), any(), any()) } returns Unit
        every { behandlingInfoDao.hentEtterbetaling(any()) } returns null

        behandlingInfoService.lagreBrevutfallOgEtterbetaling(
            behandlingId = behandlingId,
            brukerTokenInfo = bruker,
            opphoer = true,
            brevutfall = brevutfall(behandlingId).copy(),
            etterbetaling = null,
        )

        verify {
            behandlingInfoDao.lagreBrevutfall(any())
            behandlingsstatusService.settVilkaarsvurdert(any(), any(), any())
        }
    }

    private fun behandling(
        behandlingId: UUID,
        behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        sakType: SakType = SakType.BARNEPENSJON,
        behandlingStatus: BehandlingStatus = BehandlingStatus.BEREGNET,
        revurderingaarsak: Revurderingaarsak? = null,
    ): Behandling =
        mockk {
            every { id } returns behandlingId
            every { type } returns behandlingType
            every { sak } returns
                mockk {
                    every { this@mockk.sakType } returns sakType
                }
            every { status } returns behandlingStatus
            every { revurderingsaarsak() } returns revurderingaarsak
            every { virkningstidspunkt } returns
                Virkningstidspunkt.create(
                    YearMonth.of(2023, 1),
                    "begrunnelse",
                    saksbehandler = Grunnlagsopplysning.Saksbehandler.create("ident"),
                )
        }

    private fun brevutfall(behandlingId: UUID) =
        Brevutfall(
            behandlingId = behandlingId,
            aldersgruppe = Aldersgruppe.UNDER_18,
            feilutbetaling = Feilutbetaling(FeilutbetalingValg.NEI, null),
            kilde = Grunnlagsopplysning.Saksbehandler.create("Saksbehandler01"),
        )

    private fun etterbetaling(
        behandlingId: UUID,
        fom: YearMonth = YearMonth.of(2023, 1),
        tom: YearMonth = YearMonth.of(2023, 2),
    ) = Etterbetaling(
        behandlingId = behandlingId,
        fom = fom,
        tom = tom,
        inneholderKrav = true,
        kilde = Grunnlagsopplysning.Saksbehandler.create("Saksbehandler01"),
    )
}
