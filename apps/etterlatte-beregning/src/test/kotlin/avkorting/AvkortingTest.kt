package avkorting

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.avkorting.AvkortetYtelse
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingGrunnlag
import no.nav.etterlatte.beregning.regler.aarsoppgjoerMaaned
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.beregning
import no.nav.etterlatte.beregning.regler.beregningsperiode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.periode.Periode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth
import java.util.*

internal class AvkortingTest {

    @Nested
    inner class KopierAvkorting {

        private val avkorting = avkorting(
            avkortingGrunnlag = listOf(avkortinggrunnlag(), avkortinggrunnlag()),
            aarsoppgjoer = listOf(aarsoppgjoerMaaned())
        )

        @Test
        fun `Skal kopiere tidligere grunnlag men erstatte id`() {
            with(avkorting.kopierAvkorting()) {
                shouldBeEqualToIgnoringFields(avkorting, Avkorting::avkortingGrunnlag, Avkorting::aarsoppgjoer)
                with(avkortingGrunnlag) {
                    size shouldBe 2
                    get(0).id shouldNotBe avkorting.avkortingGrunnlag[0].id
                    get(1).id shouldNotBe avkorting.avkortingGrunnlag[1].id
                }
                avkortingsperioder shouldBe emptyList()
                avkortetYtelse shouldBe emptyList()
            }
        }

        @Test
        fun `Skal kopiere aarsoppgjoer`() {
            avkorting.kopierAvkorting().asClue {
                it.aarsoppgjoer shouldBe avkorting.aarsoppgjoer
            }
        }
    }

    @Nested
    inner class OppdaterMedInntektsgrunnlag {

        private val foersteGrunnlag = avkortinggrunnlag(
            periode = Periode(fom = YearMonth.of(2023, 1), tom = YearMonth.of(2023, 3))
        )
        private val andreGrunnlag = avkortinggrunnlag(
            aarsinntekt = 1000000,
            periode = Periode(fom = YearMonth.of(2023, 4), tom = null)
        )

        private val avkorting = avkorting(
            avkortingGrunnlag = listOf(foersteGrunnlag, andreGrunnlag),
            aarsoppgjoer = listOf(aarsoppgjoerMaaned())
        )

        @Test
        fun `Eksisterer det grunnlag med samme id skal eksisterende grunnlag oppdateres uten aa legge til nytt`() {
            val endretGrunnlag = andreGrunnlag.copy(aarsinntekt = 200000)

            val oppdatertAvkorting = avkorting.oppdaterMedInntektsgrunnlag(endretGrunnlag)

            oppdatertAvkorting.shouldBeEqualToIgnoringFields(avkorting, Avkorting::avkortingGrunnlag)
            with(oppdatertAvkorting.avkortingGrunnlag) {
                size shouldBe 2
                get(0) shouldBe foersteGrunnlag
                get(1) shouldBe endretGrunnlag
            }
        }

        @Test
        fun `Eksisterer ikke grunnlag skal det legges til og til og med paa periode til siste grunnlag skal settes`() {
            val nyttGrunnlag = avkortinggrunnlag(periode = Periode(fom = YearMonth.of(2023, 8), tom = null))

            val oppdatertAvkorting = avkorting.oppdaterMedInntektsgrunnlag(nyttGrunnlag)

            oppdatertAvkorting.shouldBeEqualToIgnoringFields(avkorting, Avkorting::avkortingGrunnlag)
            with(oppdatertAvkorting.avkortingGrunnlag) {
                size shouldBe 3
                get(0) shouldBe foersteGrunnlag
                get(1).shouldBeEqualToIgnoringFields(andreGrunnlag, AvkortingGrunnlag::periode)
                get(1).periode shouldBe Periode(fom = YearMonth.of(2023, 4), tom = YearMonth.of(2023, 7))
                get(2) shouldBe nyttGrunnlag
            }
        }
    }

    @Nested
    inner class BeregnAvkorting {

        private val virkningstidspunkt = YearMonth.of(2023, 3)
        private val beregning = beregning(
            beregninger = listOf(
                beregningsperiode(
                    datoFOM = virkningstidspunkt,
                    datoTOM = YearMonth.of(2023, 4),
                    utbetaltBeloep = 20902
                ),
                beregningsperiode(
                    datoFOM = YearMonth.of(2023, 5),
                    utbetaltBeloep = 22241
                )
            )
        )
        val avkorting = Avkorting.nyAvkorting().oppdaterMedInntektsgrunnlag(
            avkortinggrunnlag(
                periode = Periode(fom = virkningstidspunkt, tom = null),
                aarsinntekt = 300000,
                fratrekkInnAar = 50000,
                relevanteMaanederInnAar = 10
            )
        )

        @Nested
        inner class Foerstegangs {

            @Test
            fun `Skal beregne avkortingsperioder fra og med virkningstidspunkt`() {
                val beregnetAvkorting = avkorting.beregnAvkorting(
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    virkningstidspunkt,
                    beregning
                )
                beregnetAvkorting.avkortingsperioder.asClue {
                    it.size shouldBe 2
                    it[0].asClue { avkortingsperiode ->
                        avkortingsperiode.periode shouldBe Periode(
                            fom = virkningstidspunkt,
                            tom = YearMonth.of(2023, 4)
                        )
                        avkortingsperiode.avkorting shouldBe 9160
                    }
                    it[1].asClue { avkortingsperiode ->
                        avkortingsperiode.periode shouldBe Periode(fom = YearMonth.of(2023, 5), tom = null)
                        avkortingsperiode.avkorting shouldBe 9026
                    }
                }
            }

            @Test
            fun `Aarsoppgjoer skal vaere opprettet uten noen restanse`() {
                val beregnetAvkorting = avkorting.beregnAvkorting(
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    virkningstidspunkt,
                    beregning
                )
                beregnetAvkorting.aarsoppgjoer.asClue {
                    it.shouldContainExactly(
                        aarsoppgjoerMaaned(YearMonth.of(2023, 3), 20902, 9160, 11742, 0, 0, 11742),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 4), 20902, 9160, 11742, 0, 0, 11742),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 5), 22241, 9026, 13215, 0,0, 13215),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 6), 22241, 9026, 13215, 0,0, 13215),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 7), 22241, 9026, 13215, 0,0, 13215),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 8), 22241, 9026, 13215, 0,0, 13215),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 9), 22241, 9026, 13215, 0,0, 13215),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 10), 22241, 9026, 13215, 0, 0, 13215),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 11), 22241, 9026, 13215, 0,0, 13215),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 12), 22241, 9026, 13215, 0,0, 13215)
                    )
                }
            }

            @Test
            fun `Skal beregne ytelse etter avkorting fra virkningstidspunkt`() {
                val beregnetAvkorting = avkorting.beregnAvkorting(
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    virkningstidspunkt,
                    beregning
                )
                with(beregnetAvkorting.avkortetYtelse) {
                    size shouldBe 2
                    get(0).shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            periode = Periode(fom = virkningstidspunkt, tom = YearMonth.of(2023, 4)),
                            ytelseEtterAvkorting = 11742,
                            ytelseEtterAvkortingFoerRestanse = 11742,
                            restanse = 0,
                            avkortingsbeloep = 9160,
                            ytelseFoerAvkorting = 20902
                        ),
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde
                    )
                    get(1).shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            periode = Periode(fom = YearMonth.of(2023, 5), tom = null),
                            ytelseEtterAvkorting = 13215,
                            ytelseEtterAvkortingFoerRestanse = 13215,
                            restanse = 0,
                            avkortingsbeloep = 9026,
                            ytelseFoerAvkorting = 22241
                        ),
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde
                    )
                }
            }
        }

        @Nested
        inner class Revurdering {

            private val nyVirk = YearMonth.of(2023, 6)
            private val avkortingMedNyInntekt = avkorting.copy(
                aarsoppgjoer = listOf(
                    aarsoppgjoerMaaned(YearMonth.of(2023, 3), 20902, 9160, 11742, 0, 0, 11742),
                    aarsoppgjoerMaaned(YearMonth.of(2023, 4), 20902, 9160, 11742, 0, 0, 11742),
                    aarsoppgjoerMaaned(YearMonth.of(2023, 5), 22241, 9026, 13215, 0, 0, 13215),
                    aarsoppgjoerMaaned(YearMonth.of(2023, 6), 22241, 9026, 13215, 0, 0, 13215),
                    aarsoppgjoerMaaned(YearMonth.of(2023, 7), 22241, 9026, 13215, 0, 0, 13215),
                    aarsoppgjoerMaaned(YearMonth.of(2023, 8), 22241, 9026, 13215, 0, 0, 13215),
                    aarsoppgjoerMaaned(YearMonth.of(2023, 9), 22241, 9026, 13215, 0, 0, 13215),
                    aarsoppgjoerMaaned(YearMonth.of(2023, 10), 22241, 9026, 13215, 0, 0, 13215),
                    aarsoppgjoerMaaned(YearMonth.of(2023, 11), 22241, 9026, 13215, 0, 0, 13215),
                    aarsoppgjoerMaaned(YearMonth.of(2023, 12), 22241, 9026, 13215, 0, 0, 13215)
                )
            ).oppdaterMedInntektsgrunnlag(
                nyttGrunnlag = avkorting.avkortingGrunnlag.first().copy(
                    id = UUID.randomUUID(),
                    aarsinntekt = 400000
                )
            )

            @Test
            fun `Skal ikke kunne beregne uten et opprettet aarsoppjoer`() {
                assertThrows<IllegalArgumentException> {
                    avkorting(
                        aarsoppgjoer = emptyList()
                    ).beregnAvkorting(
                        BehandlingType.REVURDERING,
                        YearMonth.of(2023, 1),
                        beregning()
                    )
                }
            }

            @Test
            fun `Skal beregne avkortingsperioder fra og med foerste maaned i aarsoppgjoer med nytt inntektsgrunnlag`() {
                val beregnetAvkorting = avkortingMedNyInntekt.beregnAvkorting(
                    BehandlingType.REVURDERING,
                    nyVirk,
                    beregning
                )
                beregnetAvkorting.avkortingsperioder.asClue {
                    it.size shouldBe 2
                    it[0].asClue { avkortingsperiode ->
                        avkortingsperiode.periode shouldBe Periode(
                            fom = virkningstidspunkt,
                            tom = YearMonth.of(2023, 4)
                        )
                        avkortingsperiode.avkorting shouldBe 13660
                    }
                    it[1].asClue { avkortingsperiode ->
                        avkortingsperiode.periode shouldBe Periode(fom = YearMonth.of(2023, 5), tom = null)
                        avkortingsperiode.avkorting shouldBe 13526
                    }
                }
            }

            @Test
            fun `Aarsoppgjoer beregner restanse foer ny virk og fordele utover resterende maaneder etter ny virk`() {
                val beregnetAvkorting = avkortingMedNyInntekt.beregnAvkorting(
                    BehandlingType.REVURDERING,
                    nyVirk,
                    beregning
                )
                beregnetAvkorting.aarsoppgjoer.let {
                    it.shouldContainExactly(
                        aarsoppgjoerMaaned(YearMonth.of(2023, 3), 20902, 13660, 7242, 4500, 0, 11742),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 4), 20902, 13660, 7242, 4500, 0, 11742),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 5), 22241, 13526, 8715, 4500, 0, 13215),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 6), 22241, 13526, 8715, 0, 1928, 6787),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 7), 22241, 13526, 8715, 0, 1928, 6787),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 8), 22241, 13526, 8715, 0, 1928, 6787),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 9), 22241, 13526, 8715, 0, 1928, 6787),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 10), 22241, 13526, 8715, 0, 1928, 6787),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 11), 22241, 13526, 8715, 0, 1928, 6787),
                        aarsoppgjoerMaaned(YearMonth.of(2023, 12), 22241, 13526, 8715, 0, 1928, 6787)
                    )
                }
            }

            @Test
            fun `Aarsoppgjoer oppdateres med nye beregninger hvis de har endret seg`() {
                val beregnetAvkorting = avkortingMedNyInntekt.beregnAvkorting(
                    BehandlingType.REVURDERING,
                    nyVirk,
                    beregning.copy(
                        beregningsperioder = listOf(
                            beregningsperiode(
                                datoFOM = YearMonth.of(2023, 6),
                                utbetaltBeloep = 23000
                            )
                        )
                    )
                )
                beregnetAvkorting.aarsoppgjoer.let {
                    it[0].beregning shouldBe 20902
                    it[1].beregning shouldBe 20902
                    it[2].beregning shouldBe 22241
                    it[3].beregning shouldBe 23000
                    it[4].beregning shouldBe 23000
                    it[5].beregning shouldBe 23000
                    it[6].beregning shouldBe 23000
                    it[7].beregning shouldBe 23000
                    it[8].beregning shouldBe 23000
                    it[9].beregning shouldBe 23000
                }
            }

            @Test
            fun `Skal beregne ytelse etter avkorting og restanse fra nytt virkningstidspunkt`() {
                val beregnetAvkorting = avkortingMedNyInntekt.beregnAvkorting(
                    BehandlingType.REVURDERING,
                    nyVirk,
                    beregning
                )
                beregnetAvkorting.avkortetYtelse.asClue {
                    it.size shouldBe 1
                    it[0].shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            periode = Periode(fom = nyVirk, tom = null),
                            ytelseEtterAvkorting = 6787,
                            ytelseEtterAvkortingFoerRestanse = 8715,
                            restanse = 1928,
                            avkortingsbeloep = 13526,
                            ytelseFoerAvkorting = 22241
                        ),
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde
                    )
                }
            }
        }
    }
}