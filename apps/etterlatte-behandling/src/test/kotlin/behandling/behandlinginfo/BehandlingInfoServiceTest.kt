package no.nav.etterlatte.behandling.behandlinginfo

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth
import java.util.UUID
import java.util.UUID.randomUUID

internal class BehandlingInfoServiceTest {
    private val behandlingInfoDao: BehandlingInfoDao = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val behandlingInfoService: BehandlingInfoService =
        BehandlingInfoService(behandlingInfoDao, behandlingService)

    @Test
    fun `skal feile ved opprettelse av brevutfall hvis behandling ikke kan endres`() {
        val behandlingId = randomUUID()

        every { behandlingService.hentBehandling(any()) } returns
            behandling(
                behandlingId,
                BehandlingStatus.FATTET_VEDTAK,
            )

        assertThrows<BrevutfallException.BehandlingKanIkkeEndres> {
            behandlingInfoService.lagreBrevutfall(behandlingId, brevutfall(behandlingId))
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
            behandlingInfoService.lagreEtterbetaling(behandlingId, etterbetaling)
        }
    }

    @Test
    fun `skal slette etterbetaling hvis den er null og det allerede finnes en etterbetaling`() {
        val behandlingId = randomUUID()

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)
        every { behandlingInfoDao.hentEtterbetaling(any()) } returns etterbetaling(behandlingId)
        every { behandlingInfoDao.slettEtterbetaling(any()) } returns 1

        behandlingInfoService.lagreEtterbetaling(behandlingId, null)

        verify { behandlingInfoDao.slettEtterbetaling(behandlingId) }
    }

    private fun behandling(
        behandlingId: UUID,
        behandlingStatus: BehandlingStatus = BehandlingStatus.BEREGNET,
    ): Behandling =
        mockk {
            every { id } returns behandlingId
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
