package no.nav.etterlatte.behandling.etteroppgjoer

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.innvilgedePerioderIEtteroppgjoersAar
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.oppgjoersPeriode
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.vedtak.InnvilgetPeriodeDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.YearMonth
import no.nav.etterlatte.libs.common.vedtak.Periode as Vedtaksperiode

class EtteroppgjoerPerioderTest {
    @Test
    fun `to innvilgede perioder gir ett oppgjoersvindu uten at hullet fylles`() {
        val januar = YearMonth.of(2025, 1)
        val mars = YearMonth.of(2025, 3)
        val juli = YearMonth.of(2025, 7)
        val desember = YearMonth.of(2025, 12)
        val perioder =
            innvilgedePerioderIEtteroppgjoersAar(
                listOf(
                    InnvilgetPeriodeDto(Vedtaksperiode(juli, null), emptyList()),
                    InnvilgetPeriodeDto(Vedtaksperiode(januar, mars), emptyList()),
                ),
                2025,
            )

        perioder shouldBe listOf(Periode(januar, mars), Periode(juli, desember))
        oppgjoersPeriode(perioder) shouldBe Periode(januar, desember)
    }

    @Test
    fun `perioder avgrenses til aaret og perioder utenfor aaret ignoreres`() {
        val perioder =
            innvilgedePerioderIEtteroppgjoersAar(
                listOf(
                    InnvilgetPeriodeDto(Vedtaksperiode(YearMonth.of(2023, 1), YearMonth.of(2023, 12)), emptyList()),
                    InnvilgetPeriodeDto(Vedtaksperiode(YearMonth.of(2024, 11), YearMonth.of(2025, 3)), emptyList()),
                    InnvilgetPeriodeDto(Vedtaksperiode(YearMonth.of(2025, 7), YearMonth.of(2026, 3)), emptyList()),
                    InnvilgetPeriodeDto(Vedtaksperiode(YearMonth.of(2027, 1), null), emptyList()),
                ),
                2025,
            )

        perioder shouldBe
            listOf(
                Periode(YearMonth.of(2025, 1), YearMonth.of(2025, 3)),
                Periode(YearMonth.of(2025, 7), YearMonth.of(2025, 12)),
            )
    }

    @Test
    fun `en periode som starter og slutter inne i aaret bevarer grensene`() {
        val fom = YearMonth.of(2025, 3)
        val tom = YearMonth.of(2025, 10)
        val perioder =
            innvilgedePerioderIEtteroppgjoersAar(
                listOf(InnvilgetPeriodeDto(Vedtaksperiode(fom, tom), emptyList())),
                2025,
            )

        oppgjoersPeriode(perioder) shouldBe Periode(fom, tom)
    }

    @Test
    fun `sak uten innvilgede perioder gir fortsatt feil`() {
        assertThrows<UgyldigForespoerselException> {
            innvilgedePerioderIEtteroppgjoersAar(emptyList(), 2025)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [2023, 2027])
    fun `sak uten innvilgede maaneder i etteroppgjoersaaret gir feil`(aar: Int) {
        assertThrows<InternfeilException> {
            innvilgedePerioderIEtteroppgjoersAar(
                listOf(
                    InnvilgetPeriodeDto(
                        Vedtaksperiode(YearMonth.of(aar, 1), YearMonth.of(aar, 12)),
                        emptyList(),
                    ),
                ),
                2025,
            )
        }
    }
}
