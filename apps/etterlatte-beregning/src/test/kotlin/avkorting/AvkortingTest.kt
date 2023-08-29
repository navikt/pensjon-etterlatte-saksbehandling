package avkorting

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.avkorting.Aarsoppgjoer
import no.nav.etterlatte.avkorting.AvkortetYtelse
import no.nav.etterlatte.avkorting.AvkortetYtelseType
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingGrunnlag
import no.nav.etterlatte.avkorting.YtelseFoerAvkorting
import no.nav.etterlatte.avkorting.mapTilYtelseFoerAvkorting
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.beregning
import no.nav.etterlatte.beregning.regler.beregningsperiode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.*

internal class AvkortingTest {
/* TODO EY-2523
    @Nested
    inner class KopierAvkorting {

        private val virkningstidspunkt = YearMonth.of(2023, 3)
        private val beregning = beregning(
            beregninger = listOf(
                beregningsperiode(
                    datoFOM = virkningstidspunkt,
                    datoTOM = null,
                    utbetaltBeloep = 20902
                )
            )
        )
        private val avkorting = Avkorting(
            avkortingGrunnlag = listOf(avkortinggrunnlag(), avkortinggrunnlag()),
            aarsoppgjoer = Aarsoppgjoer(
                ytelseFoerAvkorting = beregning.mapTilYtelseFoerAvkorting(),
                tidligereAvkortetYtelse = listOf(
                    avkortetYtelse(
                        type = AvkortetYtelseType.TIDLIGERE,
                        periode = Periode(YearMonth.of(2023, 1), YearMonth.of(2023, 1))
                    ),
                )
            ),
            avkortetYtelse = listOf(
                avkortetYtelse(periode = Periode(YearMonth.of(2023, 2), null)),
            )
        )

        @Test
        fun `Skal kopiere tidligere grunnlag men erstatte id`() {
            with(avkorting.kopierAvkorting(virkningstidspunkt)) {
                avkortingGrunnlag.asClue {
                    it.size shouldBe 2
                    it[0].id shouldNotBe avkorting.avkortingGrunnlag[0].id
                    it[1].id shouldNotBe avkorting.avkortingGrunnlag[1].id
                }
                aarsoppgjoer.avkortingsperioder shouldBe emptyList()
                avkortetYtelse shouldBe emptyList()
            }
        }

        @Test
        fun `Skal kopiere ytelse foer avkorting til aarsoppgjoer`() {
            avkorting.kopierAvkorting(virkningstidspunkt).asClue {
                it.aarsoppgjoer.ytelseFoerAvkorting.shouldContainExactly(
                    YtelseFoerAvkorting(
                        beregning = 20902,
                        periode = Periode(YearMonth.of(2023, 3), null),
                        beregningsreferanse = beregning.beregningId
                    )
                )
            }
        }

        @Test
        fun `Skal flytte avkortetYtelse til tidligereAvkortetYtelse for gjeldende aarsoppgjoer og sette til og med`() {
            with(avkorting.kopierAvkorting(virkningstidspunkt).aarsoppgjoer) {
                tidligereAvkortetYtelse.size shouldBe 2
                tidligereAvkortetYtelse[0].asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkorting.aarsoppgjoer.tidligereAvkortetYtelse[0],
                        AvkortetYtelse::id,
                    )
                    it.id shouldNotBe avkorting.aarsoppgjoer.tidligereAvkortetYtelse[0].id
                }
                tidligereAvkortetYtelse[1].asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkorting.avkortetYtelse[0],
                        AvkortetYtelse::id,
                        AvkortetYtelse::periode,
                        AvkortetYtelse::type
                    )
                    it.id shouldNotBe avkorting.avkortetYtelse[0].id
                    it.periode shouldBe Periode(YearMonth.of(2023, 2), virkningstidspunkt.minusMonths(1))
                    it.type shouldBe AvkortetYtelseType.TIDLIGERE
                }
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

        private val virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2023, 3))
        private val beregning = beregning(
            beregninger = listOf(
                beregningsperiode(
                    datoFOM = virkningstidspunkt.dato,
                    datoTOM = YearMonth.of(2023, 4),
                    utbetaltBeloep = 20902
                ),
                beregningsperiode(
                    datoFOM = YearMonth.of(2023, 5),
                    utbetaltBeloep = 22241
                )
            )
        )
        val avkorting = Avkorting().oppdaterMedInntektsgrunnlag(
            avkortinggrunnlag(
                periode = Periode(fom = virkningstidspunkt.dato, tom = null),
                aarsinntekt = 300000,
                fratrekkInnAar = 50000,
                relevanteMaanederInnAar = 10
            )
        )

        @Nested
        inner class Foerstegangs {

            @Test
            fun `Skal lagre beregninger som ytelse foer avkorting med referanse til beregningen`() {
                val beregnetAvkorting = avkorting.beregnAvkorting(
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    virkningstidspunkt,
                    beregning
                )
                beregnetAvkorting.aarsoppgjoer.ytelseFoerAvkorting.asClue {
                    it.size shouldBe 2
                    it[0] shouldBe YtelseFoerAvkorting(
                        beregning = 20902,
                        periode = Periode(
                            fom = virkningstidspunkt.dato,
                            tom = YearMonth.of(2023, 4)
                        ),
                        beregningsreferanse = beregning.beregningId
                    )
                    it[1] shouldBe YtelseFoerAvkorting(
                        beregning = 22241,
                        periode = Periode(fom = YearMonth.of(2023, 5), tom = null),
                        beregningsreferanse = beregning.beregningId
                    )
                }
            }

            @Test
            fun `Skal beregne avkortingsperioder fra og med virkningstidspunkt`() {
                val beregnetAvkorting = avkorting.beregnAvkorting(
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    virkningstidspunkt,
                    beregning
                )
                beregnetAvkorting.aarsoppgjoer.avkortingsperioder.asClue {
                    it.size shouldBe 2
                    it[0].asClue { avkortingsperiode ->
                        avkortingsperiode.periode shouldBe Periode(
                            fom = virkningstidspunkt.dato,
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
                            periode = Periode(fom = virkningstidspunkt.dato, tom = YearMonth.of(2023, 4)),
                            ytelseEtterAvkorting = 11742,
                            ytelseEtterAvkortingFoerRestanse = 11742,
                            restanse = 0,
                            avkortingsbeloep = 9160,
                            ytelseFoerAvkorting = 20902
                        ),
                        AvkortetYtelse::id,
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
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde
                    )
                }
            }
        }

        @Nested
        inner class Revurdering {

            private val nyVirk = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2023, 6))
            private val avkortingMedNyInntekt = avkorting
                .beregnAvkorting(BehandlingType.FØRSTEGANGSBEHANDLING, virkningstidspunkt, beregning)
                .kopierAvkorting(nyVirk.dato)
                .oppdaterMedInntektsgrunnlag(
                    nyttGrunnlag = avkorting.avkortingGrunnlag.first().copy(
                        id = UUID.randomUUID(),
                        aarsinntekt = 400000
                    )
                )

            @Test
            fun `Skal beregne avkortingsperioder fra og med foerste maaned i aarsoppgjoer med nytt inntektsgrunnlag`() {
                val beregnetAvkorting = avkortingMedNyInntekt.beregnAvkorting(
                    BehandlingType.REVURDERING,
                    nyVirk,
                    beregning
                )
                beregnetAvkorting.aarsoppgjoer.avkortingsperioder.asClue {
                    it.size shouldBe 2
                    it[0].asClue { avkortingsperiode ->
                        avkortingsperiode.periode shouldBe Periode(
                            fom = virkningstidspunkt.dato,
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
            fun `Aarsoppgjoer oppdateres med nye beregninger hvis de har endret seg`() {
                val idNyBeregning = UUID.randomUUID()
                val beregnetAvkorting = avkortingMedNyInntekt.beregnAvkorting(
                    BehandlingType.REVURDERING,
                    nyVirk,
                    beregning.copy(
                        beregningId = idNyBeregning,
                        beregningsperioder = listOf(
                            beregningsperiode(
                                datoFOM = YearMonth.of(2023, 6),
                                utbetaltBeloep = 23000
                            )
                        )
                    )
                )
                beregnetAvkorting.aarsoppgjoer.asClue {
                    it.ytelseFoerAvkorting.shouldContainExactly(
                        YtelseFoerAvkorting(
                            beregning = 20902,
                            periode = Periode(YearMonth.of(2023, 3), YearMonth.of(2023, 4)),
                            beregningsreferanse = beregning.beregningId
                        ), YtelseFoerAvkorting(
                            beregning = 22241,
                            periode = Periode(YearMonth.of(2023, 5), YearMonth.of(2023, 5)),
                            beregningsreferanse = beregning.beregningId
                        ),
                        YtelseFoerAvkorting(
                            beregning = 23000,
                            periode = Periode(YearMonth.of(2023, 6), null),
                            beregningsreferanse = idNyBeregning
                        )
                    )
                }
            }

            @Test
            fun `Beregner restanse ved aa finne diff mellom tidligere avkortet ytelse og avkortet ytelse reberegnet`() {
                val beregnetAvkorting = avkortingMedNyInntekt.beregnAvkorting(
                    BehandlingType.REVURDERING,
                    nyVirk,
                    beregning
                )
                with(beregnetAvkorting.aarsoppgjoer) {
                    tidligereAvkortetYtelse.asClue {
                        it.size shouldBe 2
                        it[0].shouldBeEqualToIgnoringFields(
                            avkortetYtelse(
                                type = AvkortetYtelseType.TIDLIGERE,
                                periode = Periode(fom = YearMonth.of(2023, 3), tom = YearMonth.of(2023, 4)),
                                ytelseEtterAvkorting = 11742,
                                ytelseEtterAvkortingFoerRestanse = 11742,
                                restanse = 0,
                                avkortingsbeloep = 9160,
                                ytelseFoerAvkorting = 20902
                            ),
                            AvkortetYtelse::id,
                            AvkortetYtelse::tidspunkt,
                            AvkortetYtelse::regelResultat,
                            AvkortetYtelse::kilde
                        )
                        it[1].shouldBeEqualToIgnoringFields(
                            avkortetYtelse(
                                type = AvkortetYtelseType.TIDLIGERE,
                                periode = Periode(fom = YearMonth.of(2023, 5), tom = YearMonth.of(2023, 5)),
                                ytelseEtterAvkorting = 13215,
                                ytelseEtterAvkortingFoerRestanse = 13215,
                                restanse = 0,
                                avkortingsbeloep = 9026,
                                ytelseFoerAvkorting = 22241
                            ),
                            AvkortetYtelse::id,
                            AvkortetYtelse::tidspunkt,
                            AvkortetYtelse::regelResultat,
                            AvkortetYtelse::kilde
                        )
                    }
                    tidligereAvkortetYtelseReberegnet.asClue {
                        it.size shouldBe 2
                        it[0].shouldBeEqualToIgnoringFields(
                            avkortetYtelse(
                                type = AvkortetYtelseType.REBEREGNET,
                                periode = Periode(fom = YearMonth.of(2023, 3), tom = YearMonth.of(2023, 4)),
                                ytelseEtterAvkorting = 7242,
                                ytelseEtterAvkortingFoerRestanse = 7242,
                                restanse = 0,
                                avkortingsbeloep = 13660,
                                ytelseFoerAvkorting = 20902
                            ),
                            AvkortetYtelse::id,
                            AvkortetYtelse::tidspunkt,
                            AvkortetYtelse::regelResultat,
                            AvkortetYtelse::kilde
                        )
                        it[1].shouldBeEqualToIgnoringFields(
                            avkortetYtelse(
                                type = AvkortetYtelseType.REBEREGNET,
                                periode = Periode(fom = YearMonth.of(2023, 5), tom = YearMonth.of(2023, 5)),
                                ytelseEtterAvkorting = 8715,
                                ytelseEtterAvkortingFoerRestanse = 8715,
                                restanse = 0,
                                avkortingsbeloep = 13526,
                                ytelseFoerAvkorting = 22241
                            ),
                            AvkortetYtelse::id,
                            AvkortetYtelse::tidspunkt,
                            AvkortetYtelse::regelResultat,
                            AvkortetYtelse::kilde
                        )
                    }
                    restanse.asClue {
                        it!!.totalRestanse shouldBe 13500
                        it.fordeltRestanse shouldBe 1928
                    }
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
                            periode = Periode(fom = nyVirk.dato, tom = null),
                            ytelseEtterAvkorting = 6787,
                            ytelseEtterAvkortingFoerRestanse = 8715,
                            restanse = 1928,
                            avkortingsbeloep = 13526,
                            ytelseFoerAvkorting = 22241
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde
                    )
                }
            }
        }
    }
*/
}