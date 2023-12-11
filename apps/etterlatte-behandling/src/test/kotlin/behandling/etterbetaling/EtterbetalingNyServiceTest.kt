package behandling.etterbetaling

import io.mockk.mockk
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etterbetaling.EtterbetalingDao
import no.nav.etterlatte.behandling.etterbetaling.EtterbetalingService
import no.nav.etterlatte.behandling.etterbetaling.EtterbetalingUgyldigException
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.stream.Stream

class EtterbetalingNyServiceTest {
    private val behandlingService: BehandlingService = mockk()
    private val etterbetalingDao: EtterbetalingDao = mockk()

    private val etterbetalingService =
        EtterbetalingService(
            etterbetalingDao,
            behandlingService,
        )

    @Test
    fun `etterbetaling kan opprettes hvis det passer innenfor virkningstidspunktet`() {
        val sakId = 1L
        val virkningstidspunkt =
            Virkningstidspunkt(
                dato = YearMonth.of(2023, Month.OCTOBER),
                begrunnelse = "Hei",
                kilde = Grunnlagsopplysning.automatiskSaksbehandler,
            )
        val behandling =
            foerstegangsbehandling(
                sakId = sakId,
                virkningstidspunkt = virkningstidspunkt,
            )
        val etterbetaling =
            etterbetalingService.validerEtterbetaling(
                behandling,
                fraDato = virkningstidspunkt.dato.atDay(1),
                tilDato = virkningstidspunkt.dato.plusMonths(1).atDay(1),
            )

        assertEquals(virkningstidspunkt.dato, etterbetaling.fra)
        assertEquals(virkningstidspunkt.dato.plusMonths(1), etterbetaling.til)
        assertEquals(behandling.id, etterbetaling.behandlingId)
    }

    @ParameterizedTest(name = "{0} kaster riktig valideringsfeil")
    @MethodSource("valideringsCaserEtterbetaling")
    fun `etterbetaling valideringsfeil`(
        beskrivelse: String,
        virk: YearMonth,
        fom: LocalDate?,
        tom: LocalDate?,
        feil: Class<EtterbetalingUgyldigException>,
    ) {
        val sakId = 1L
        val virkningstidspunkt =
            Virkningstidspunkt(
                dato = virk,
                begrunnelse = "Hei",
                kilde = Grunnlagsopplysning.automatiskSaksbehandler,
            )
        val behandling =
            foerstegangsbehandling(
                sakId = sakId,
                virkningstidspunkt = virkningstidspunkt,
            )
        try {
            etterbetalingService.validerEtterbetaling(
                behandling,
                fraDato = fom,
                tilDato = tom,
            )
        } catch (e: Throwable) {
            assertInstanceOf(feil, e)
            return
        }
        fail { "Kastet ikke valideringsfeil" }
    }

    companion object {
        @JvmStatic
        fun valideringsCaserEtterbetaling(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    "fom etter tom",
                    YearMonth.of(2023, Month.OCTOBER),
                    LocalDate.of(2023, Month.NOVEMBER, 1),
                    LocalDate.of(2023, Month.OCTOBER, 1),
                    EtterbetalingUgyldigException.FraEtterTil::class.java,
                ),
                Arguments.of(
                    "tom er null",
                    YearMonth.of(2023, Month.OCTOBER),
                    LocalDate.of(2023, Month.OCTOBER, 1),
                    null,
                    EtterbetalingUgyldigException.ManglerDato::class.java,
                ),
                Arguments.of(
                    "fom er null",
                    YearMonth.of(2023, Month.OCTOBER),
                    null,
                    LocalDate.of(2023, Month.NOVEMBER, 1),
                    EtterbetalingUgyldigException.ManglerDato::class.java,
                ),
                Arguments.of(
                    "tom og fom er null",
                    YearMonth.of(2023, Month.OCTOBER),
                    null,
                    null,
                    EtterbetalingUgyldigException.ManglerDato::class.java,
                ),
                Arguments.of(
                    "fom foer virk",
                    YearMonth.of(2023, Month.OCTOBER),
                    LocalDate.of(2023, Month.SEPTEMBER, 1),
                    LocalDate.of(2023, Month.NOVEMBER, 1),
                    EtterbetalingUgyldigException.FraFoerVirk::class.java,
                ),
                Arguments.of(
                    "tom er fram i tid",
                    YearMonth.of(2023, Month.OCTOBER),
                    LocalDate.of(2023, Month.OCTOBER, 1),
                    YearMonth.now().plusMonths(1).atDay(1),
                    EtterbetalingUgyldigException.TilErFramITid::class.java,
                ),
            )
    }
}
