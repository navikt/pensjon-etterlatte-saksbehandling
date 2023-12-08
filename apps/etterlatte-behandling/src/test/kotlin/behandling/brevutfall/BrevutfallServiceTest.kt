package no.nav.etterlatte.behandling.brevutfall

import io.mockk.every
import io.mockk.mockk
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

internal class BrevutfallServiceTest {
    private val brevutfallDao: BrevutfallDao = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val brevutfallService: BrevutfallService = BrevutfallService(brevutfallDao, behandlingService)

    @Test
    fun `skal feile ved opprettelse av brevutfall hvis behandling ikke kan endres`() {
        val behandlingId = randomUUID()

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId, BehandlingStatus.FATTET_VEDTAK)

        assertThrows<BrevutfallException.BehandlingKanIkkeEndres> {
            brevutfallService.lagreBrevutfall(brevutfall(behandlingId))
        }
    }

    @Test
    fun `skal feile ved opprettelse av brevutfall hvis etterbetaling fra-dato er foer virkningstidspunkt`() {
        val behandlingId = randomUUID()

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)

        assertThrows<BrevutfallException.EtterbetalingFraDatoErFoerVirk> {
            brevutfallService.lagreBrevutfall(
                brevutfall(
                    behandlingId,
                    etterbetaling =
                        Etterbetaling(
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
        etterbetaling: Etterbetaling =
            Etterbetaling(
                fom = YearMonth.of(2023, 1),
                tom = YearMonth.of(2023, 2),
            ),
    ) = Brevutfall(
        behandlingId = behandlingId,
        etterbetaling = etterbetaling,
        aldersgruppe = Aldersgruppe.UNDER_18,
        kilde = Grunnlagsopplysning.Saksbehandler.create("Saksbehandler01"),
    )
}
