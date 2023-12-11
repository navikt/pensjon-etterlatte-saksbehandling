package no.nav.etterlatte.behandling.behandlinginfo

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Brevutfall
import no.nav.etterlatte.libs.common.behandling.BrevutfallException
import no.nav.etterlatte.libs.common.behandling.EtterbetalingException
import no.nav.etterlatte.libs.common.behandling.EtterbetalingNy
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
    private val behandlingInfoService: BehandlingInfoService = BehandlingInfoService(behandlingInfoDao, behandlingService)

    @Test
    fun `skal feile ved opprettelse av brevutfall hvis behandling ikke kan endres`() {
        val behandlingId = randomUUID()

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId, BehandlingStatus.FATTET_VEDTAK)

        assertThrows<BrevutfallException.BehandlingKanIkkeEndres> {
            behandlingInfoService.lagreBrevutfall(brevutfall(behandlingId))
        }
    }

    @Test
    fun `skal feile ved opprettelse av brevutfall hvis etterbetaling fra-dato er foer virkningstidspunkt`() {
        val behandlingId = randomUUID()

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)

        assertThrows<EtterbetalingException.EtterbetalingFraDatoErFoerVirk> {
            behandlingInfoService.lagreBrevutfall(
                brevutfall(
                    behandlingId,
                    etterbetalingNy =
                        EtterbetalingNy(
                            fom = YearMonth.of(2022, 1),
                            tom = YearMonth.of(2023, 3),
                        ),
                ),
            )
        }
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

    private fun brevutfall(
        behandlingId: UUID,
        etterbetalingNy: EtterbetalingNy =
            EtterbetalingNy(
                fom = YearMonth.of(2023, 1),
                tom = YearMonth.of(2023, 2),
            ),
    ) = Brevutfall(
        behandlingId = behandlingId,
        etterbetalingNy = etterbetalingNy,
        aldersgruppe = Aldersgruppe.UNDER_18,
        kilde = Grunnlagsopplysning.Saksbehandler.create("Saksbehandler01"),
    )
}
