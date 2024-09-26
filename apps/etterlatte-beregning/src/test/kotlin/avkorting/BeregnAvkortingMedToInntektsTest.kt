package no.nav.etterlatte.beregning.regler.avkorting

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.avkorting.AvkortetYtelse
import no.nav.etterlatte.avkorting.AvkortetYtelseType
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkortinggrunnlagLagre
import no.nav.etterlatte.beregning.regler.beregning
import no.nav.etterlatte.beregning.regler.beregningsperiode
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.libs.common.periode.Periode
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import java.time.Month
import java.time.YearMonth

class BeregnAvkortingMedToInntektsTest {
    @Test
    @Order(0)
    fun `Beregner avkortet ytelse for foerstegangsbehandling`() {
        val foerstegangsbehandlingInnevaerendeInnekt = `Avkorting førstegangsbehandling inneværende år`()
        val foerstegangsbehandlingNesteInntekt =
            `Avkorting førstegangsbehandling neste år`(foerstegangsbehandlingInnevaerendeInnekt)
        with(foerstegangsbehandlingNesteInntekt.aarsoppgjoer.first().avkortetYtelseAar) {
            size shouldBe 1
            get(0).shouldBeEqualToIgnoringFields(
                avkortetYtelse(
                    periode = Periode(fom = YearMonth.of(2024, Month.JULY), tom = null),
                    ytelseEtterAvkorting = 7758,
                    ytelseEtterAvkortingFoerRestanse = 7758,
                    restanse = null,
                    avkortingsbeloep = 8924,
                    ytelseFoerAvkorting = 16682,
                    type = AvkortetYtelseType.AARSOPPGJOER,
                    inntektsgrunnlag = null,
                ),
                AvkortetYtelse::id,
                AvkortetYtelse::tidspunkt,
                AvkortetYtelse::regelResultat,
                AvkortetYtelse::kilde,
            )
        }
        with(foerstegangsbehandlingNesteInntekt.aarsoppgjoer[1].avkortetYtelseAar) {
            size shouldBe 1
            get(0).shouldBeEqualToIgnoringFields(
                avkortetYtelse(
                    periode = Periode(fom = YearMonth.of(2025, Month.JANUARY), tom = null),
                    ytelseEtterAvkorting = 6820,
                    ytelseEtterAvkortingFoerRestanse = 6820,
                    restanse = null,
                    avkortingsbeloep = 9862,
                    ytelseFoerAvkorting = 16682,
                    type = AvkortetYtelseType.AARSOPPGJOER,
                    inntektsgrunnlag = null,
                ),
                AvkortetYtelse::id,
                AvkortetYtelse::tidspunkt,
                AvkortetYtelse::regelResultat,
                AvkortetYtelse::kilde,
            )
        }
    }

    @Test
    @Order(1)
    fun `Beregner avkortet ytelse for revurdering`() {
        val foerstegangsbehandlingInnevaerendeInnekt = `Avkorting førstegangsbehandling inneværende år`()
        val foerstegangsbehandlingNesteInntekt =
            `Avkorting førstegangsbehandling neste år`(foerstegangsbehandlingInnevaerendeInnekt)
        val revurderingInnevaerendeInnekt = `Avkorting revurdering inneværende år`(foerstegangsbehandlingNesteInntekt)
        val revurderingNesteInntekt = `Avkorting revurdering neste år`(revurderingInnevaerendeInnekt)
    }

    private fun `Avkorting førstegangsbehandling inneværende år`() =
        Avkorting()
            .beregnAvkortingMedNyttGrunnlag(
                nyttGrunnlag =
                    avkortinggrunnlagLagre(
                        aarsinntekt = 300000,
                        fratrekkInnAar = 150000,
                        fom = YearMonth.of(2024, Month.JULY),
                    ),
                bruker = bruker,
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2024, Month.JULY),
                                    utbetaltBeloep = 16682,
                                ),
                            ),
                    ),
                sanksjoner = emptyList(),
                opphoerFom = null,
            )

    private fun `Avkorting førstegangsbehandling neste år`(eksisterende: Avkorting) =
        eksisterende
            .beregnAvkortingMedNyttGrunnlag(
                nyttGrunnlag =
                    avkortinggrunnlagLagre(
                        aarsinntekt = 325000,
                        fratrekkInnAar = 0,
                        fom = YearMonth.of(2025, Month.JANUARY),
                    ),
                bruker = bruker,
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2025, Month.JANUARY),
                                    utbetaltBeloep = 16682,
                                ),
                            ),
                    ),
                sanksjoner = emptyList(),
                opphoerFom = null,
            )

    private fun `Avkorting revurdering inneværende år`(eksisterende: Avkorting) =
        eksisterende
            .beregnAvkortingMedNyttGrunnlag(
                nyttGrunnlag =
                    avkortinggrunnlagLagre(
                        aarsinntekt = 350000,
                        fratrekkInnAar = 150000,
                        fom = YearMonth.of(2024, Month.OCTOBER),
                    ),
                bruker = bruker,
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2024, Month.OCTOBER),
                                    utbetaltBeloep = 16682,
                                ),
                            ),
                    ),
                sanksjoner = emptyList(),
                opphoerFom = null,
            )

    private fun `Avkorting revurdering neste år`(eksisterende: Avkorting) =
        eksisterende
            .beregnAvkortingMedNyttGrunnlag(
                nyttGrunnlag =
                    avkortinggrunnlagLagre(
                        id =
                            eksisterende.aarsoppgjoer[1]
                                .inntektsavkorting[0]
                                .grunnlag.id,
                        aarsinntekt = 375000,
                        fratrekkInnAar = 0,
                        fom = YearMonth.of(2025, Month.JANUARY),
                    ),
                bruker = bruker,
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2025, Month.JANUARY),
                                    utbetaltBeloep = 16682,
                                ),
                            ),
                    ),
                sanksjoner = emptyList(),
                opphoerFom = null,
            )
}
