package no.nav.etterlatte.behandling.behandlinginfo

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Brevutfall
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.token.Saksbehandler
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
    fun `brevutfall skal oppdatere behandlingsstatus hvis endret`() {
        val behandlingId = randomUUID()
        val brevutfall = brevutfall(behandlingId)
        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)
        every { behandlingInfoDao.lagreBrevutfall(any()) } returns mockk()
        every { behandlingsstatusService.settBeregnet(any(), any(), any()) } returns Unit

        behandlingInfoService.lagreBrevutfall(behandlingId, brevutfall, bruker)

        verify {
            behandlingInfoDao.lagreBrevutfall(brevutfall)
            behandlingsstatusService.settBeregnet(behandlingId, bruker, false)
        }
    }

    @Test
    fun `Etterbetaling skal oppdatere behandlingsstatus hvis endret`() {
        val behandlingId = randomUUID()
        val brevutfall = brevutfall(behandlingId)
        every { behandlingService.hentBehandling(any()) } returns
            behandling(
                behandlingId,
                SakType.OMSTILLINGSSTOENAD,
                BehandlingStatus.AVKORTET,
            )
        every { behandlingInfoDao.lagreBrevutfall(any()) } returns mockk()
        every { behandlingsstatusService.settAvkortet(any(), any(), any()) } returns Unit

        behandlingInfoService.lagreBrevutfall(behandlingId, brevutfall, bruker)

        verify {
            behandlingInfoDao.lagreBrevutfall(brevutfall)
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
            behandlingInfoService.lagreBrevutfall(behandlingId, brevutfall(behandlingId), bruker)
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

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)
        every { behandlingInfoDao.hentEtterbetaling(any()) } returns etterbetaling

        assertThrows<EtterbetalingException.EtterbetalingFraDatoErFoerVirk> {
            behandlingInfoService.lagreEtterbetaling(behandlingId, etterbetaling, bruker)
        }
    }

    @Test
    fun `skal slette etterbetaling hvis den er null og det allerede finnes en etterbetaling`() {
        val behandlingId = randomUUID()

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)
        every { behandlingInfoDao.hentEtterbetaling(any()) } returns etterbetaling(behandlingId)
        every { behandlingInfoDao.slettEtterbetaling(any()) } returns 1

        behandlingInfoService.lagreEtterbetaling(behandlingId, null, bruker)

        verify { behandlingInfoDao.slettEtterbetaling(behandlingId) }
    }

    private fun behandling(
        behandlingId: UUID,
        type: SakType = SakType.BARNEPENSJON,
        behandlingStatus: BehandlingStatus = BehandlingStatus.BEREGNET,
    ): Behandling =
        mockk {
            every { id } returns behandlingId
            every { sak } returns
                mockk {
                    every { sakType } returns type
                }
            every { status } returns behandlingStatus
            every { virkningstidspunkt } returns
                Virkningstidspunkt.create(YearMonth.of(2023, 1), "ident", "begrunnelse")
        }

    private fun brevutfall(behandlingId: UUID) =
        Brevutfall(
            behandlingId = behandlingId,
            aldersgruppe = Aldersgruppe.UNDER_18,
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
        kilde = Grunnlagsopplysning.Saksbehandler.create("Saksbehandler01"),
    )
}
