package no.nav.etterlatte.avkorting

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.avkortinggrunnlagLagre
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.libs.common.periode.Periode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Month
import java.time.YearMonth
import java.util.UUID

internal class AvkortingTest {
    @Nested
    inner class AvkortetYtelseFraVirkningstidspunkt {
        val avkorting =
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        Aarsoppgjoer(
                            avkortetYtelseAar =
                                listOf(
                                    avkortetYtelse(
                                        periode =
                                            Periode(
                                                fom = YearMonth.of(2024, Month.MARCH),
                                                tom = YearMonth.of(2024, Month.APRIL),
                                            ),
                                    ),
                                    avkortetYtelse(
                                        periode =
                                            Periode(
                                                fom = YearMonth.of(2024, Month.MAY),
                                                tom = YearMonth.of(2024, Month.JULY),
                                            ),
                                    ),
                                    avkortetYtelse(
                                        periode = Periode(fom = YearMonth.of(2024, Month.AUGUST), tom = null),
                                    ),
                                ),
                        ),
                    ),
            )

        @Test
        fun `fyller ut avkortet ytelse foer virkningstidspunkt ved aa kutte aarsoppgjoer fra virkningstidspunkt`() {
            avkorting.medYtelseFraOgMedVirkningstidspunkt(virkningstidspunkt = YearMonth.of(2024, Month.MAY)).asClue {
                it.avkortetYtelseFraVirkningstidspunkt.size shouldBe 2
                it.avkortetYtelseFraVirkningstidspunkt[0] shouldBe avkorting.aarsoppgjoer.single().avkortetYtelseAar[1]
                it.avkortetYtelseFraVirkningstidspunkt[1] shouldBe avkorting.aarsoppgjoer.single().avkortetYtelseAar[2]
            }
        }

        @Test
        fun `kutter periode fra aarsoppgjoer hvis virkningstidspunkt begynner midt i periode `() {
            avkorting.medYtelseFraOgMedVirkningstidspunkt(virkningstidspunkt = YearMonth.of(2024, Month.APRIL)).asClue {
                it.avkortetYtelseFraVirkningstidspunkt.size shouldBe 3
                with(it.avkortetYtelseFraVirkningstidspunkt[0]) {
                    shouldBeEqualToIgnoringFields(
                        avkorting.aarsoppgjoer.single().avkortetYtelseAar[0],
                        AvkortetYtelse::periode,
                    )
                    periode shouldBe
                        Periode(
                            fom = YearMonth.of(2024, Month.APRIL),
                            tom = YearMonth.of(2024, Month.APRIL),
                        )
                }
                it.avkortetYtelseFraVirkningstidspunkt[1] shouldBe avkorting.aarsoppgjoer.single().avkortetYtelseAar[1]
                it.avkortetYtelseFraVirkningstidspunkt[2] shouldBe avkorting.aarsoppgjoer.single().avkortetYtelseAar[2]
            }

            avkorting.medYtelseFraOgMedVirkningstidspunkt(virkningstidspunkt = YearMonth.of(2024, Month.JUNE)).asClue {
                it.avkortetYtelseFraVirkningstidspunkt.size shouldBe 2
                with(it.avkortetYtelseFraVirkningstidspunkt[0]) {
                    shouldBeEqualToIgnoringFields(
                        avkorting.aarsoppgjoer.single().avkortetYtelseAar[1],
                        AvkortetYtelse::periode,
                    )
                    periode shouldBe Periode(fom = YearMonth.of(2024, Month.JUNE), tom = YearMonth.of(2024, Month.JULY))
                }
                it.avkortetYtelseFraVirkningstidspunkt[1] shouldBe avkorting.aarsoppgjoer.single().avkortetYtelseAar[2]
            }

            avkorting.medYtelseFraOgMedVirkningstidspunkt(virkningstidspunkt = YearMonth.of(2024, Month.SEPTEMBER))
                .asClue {
                    it.avkortetYtelseFraVirkningstidspunkt.size shouldBe 1
                    with(it.avkortetYtelseFraVirkningstidspunkt[0]) {
                        shouldBeEqualToIgnoringFields(
                            avkorting.aarsoppgjoer.single().avkortetYtelseAar[2],
                            AvkortetYtelse::periode,
                        )
                        periode shouldBe Periode(fom = YearMonth.of(2024, Month.SEPTEMBER), tom = null)
                    }
                }
        }
    }

    @Nested
    inner class KopierAvkorting {
        private val virkningstidspunkt = YearMonth.of(2024, Month.JULY)
        private val beregningId = UUID.randomUUID()

        private val eksisterendeAvkorting =
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        Aarsoppgjoer(
                            ytelseFoerAvkorting =
                                listOf(
                                    YtelseFoerAvkorting(
                                        beregning = 20902,
                                        periode = Periode(virkningstidspunkt, null),
                                        beregningsreferanse = beregningId,
                                    ),
                                ),
                            inntektsavkorting =
                                listOf(
                                    Inntektsavkorting(avkortinggrunnlag()),
                                    Inntektsavkorting(avkortinggrunnlag()),
                                ),
                        ),
                    ),
            )

        private val nyAvkorting = eksisterendeAvkorting.kopierAvkorting()

        @Test
        fun `Skal kopiere tidligere inntekt men erstatte id`() {
            with(nyAvkorting) {
                aarsoppgjoer.single().inntektsavkorting.asClue {
                    it.size shouldBe 2
                    it[0].grunnlag.id shouldNotBe eksisterendeAvkorting.aarsoppgjoer.single().inntektsavkorting[0].grunnlag.id
                    it[1].grunnlag.id shouldNotBe eksisterendeAvkorting.aarsoppgjoer.single().inntektsavkorting[1].grunnlag.id
                }
            }
        }

        @Test
        fun `Skal kopiere ytelse foer avkorting til aarsoppgjoer`() {
            nyAvkorting.asClue {
                it.aarsoppgjoer.single().ytelseFoerAvkorting.shouldContainExactly(
                    YtelseFoerAvkorting(
                        beregning = 20902,
                        periode = Periode(fom = virkningstidspunkt, tom = null),
                        beregningsreferanse = beregningId,
                    ),
                )
            }
        }
    }

    @Nested
    inner class OppdaterMedInntektsgrunnlag {
        private val foersteInntekt =
            avkortinggrunnlag(
                periode = Periode(fom = YearMonth.of(2024, Month.JANUARY), tom = YearMonth.of(2024, Month.MARCH)),
                relevanteMaanederInnAar = 12,
            )
        private val andreInntekt =
            avkortinggrunnlag(
                aarsinntekt = 1000000,
                periode = Periode(fom = YearMonth.of(2024, Month.APRIL), tom = null),
                relevanteMaanederInnAar = 12,
            )
        private val avkorting =
            avkorting(
                inntektsavkorting =
                    listOf(
                        Inntektsavkorting(foersteInntekt),
                        Inntektsavkorting(andreInntekt),
                    ),
            )

        @Test
        fun `Eksisterer det inntekt med samme id skal eksisterende inntekt oppdateres uten aa legge til nytt`() {
            val endretInntekt = avkortinggrunnlagLagre(id = andreInntekt.id, aarsinntekt = 200000)
            val virkningstidspunkt = YearMonth.of(2024, Month.MARCH)

            val oppdatertAvkorting =
                avkorting.oppdaterMedInntektsgrunnlag(
                    endretInntekt,
                    virkningstidspunkt,
                    bruker,
                )

            oppdatertAvkorting.shouldBeEqualToIgnoringFields(avkorting, Avkorting::aarsoppgjoer)
            oppdatertAvkorting.aarsoppgjoer.single().shouldBeEqualToIgnoringFields(
                avkorting.aarsoppgjoer.single(),
                Aarsoppgjoer::inntektsavkorting,
            )
            with(oppdatertAvkorting.aarsoppgjoer.single().inntektsavkorting) {
                size shouldBe 2
                get(0).grunnlag shouldBe foersteInntekt
                with(get(1).grunnlag) {
                    aarsinntekt shouldBe endretInntekt.aarsinntekt
                    fratrekkInnAar shouldBe endretInntekt.fratrekkInnAar
                    inntektUtland shouldBe endretInntekt.inntektUtland
                    fratrekkInnAarUtland shouldBe endretInntekt.fratrekkInnAarUtland
                    spesifikasjon shouldBe endretInntekt.spesifikasjon
                }
            }
        }

        @Test
        fun `Eksisterer ikke inntekt skal det legges til og til og med paa periode til siste inntekt skal settes`() {
            val nyttGrunnlag = avkortinggrunnlagLagre()
            val virkningstidspunkt = YearMonth.of(2024, Month.AUGUST)

            val oppdatertAvkorting =
                avkorting.oppdaterMedInntektsgrunnlag(
                    nyttGrunnlag,
                    virkningstidspunkt,
                    bruker,
                )

            oppdatertAvkorting.shouldBeEqualToIgnoringFields(avkorting, Avkorting::aarsoppgjoer)
            oppdatertAvkorting.aarsoppgjoer.single().shouldBeEqualToIgnoringFields(
                avkorting.aarsoppgjoer.single(),
                Aarsoppgjoer::inntektsavkorting,
            )
            with(oppdatertAvkorting.aarsoppgjoer.single().inntektsavkorting) {
                size shouldBe 3
                get(0).grunnlag shouldBe foersteInntekt
                get(1).grunnlag.shouldBeEqualToIgnoringFields(andreInntekt, AvkortingGrunnlag::periode)
                get(1).grunnlag.periode shouldBe
                    Periode(
                        fom = YearMonth.of(2024, Month.APRIL),
                        tom = YearMonth.of(2024, Month.JULY),
                    )
                with(get(2).grunnlag) {
                    aarsinntekt shouldBe nyttGrunnlag.aarsinntekt
                    fratrekkInnAar shouldBe nyttGrunnlag.fratrekkInnAar
                    inntektUtland shouldBe nyttGrunnlag.inntektUtland
                    fratrekkInnAarUtland shouldBe nyttGrunnlag.fratrekkInnAarUtland
                    spesifikasjon shouldBe nyttGrunnlag.spesifikasjon
                }
            }
        }

        @Test
        fun `Relvante maaneder skal utledes basert paa virkningstidspunkt`() {
            val inntektDto = avkortinggrunnlagLagre()
            val virkningstidspunkt = YearMonth.of(2024, Month.MARCH)

            avkorting(inntektsavkorting = emptyList()).oppdaterMedInntektsgrunnlag(
                inntektDto,
                virkningstidspunkt,
                bruker,
            ).let {
                it.aarsoppgjoer.single().inntektsavkorting.single().grunnlag.relevanteMaanederInnAar shouldBe 10
            }
        }

        @Test
        fun `Relvante maaneder skal skal viderfoeres fra forrige inntekt for samme aar`() {
            val inntektDto = avkortinggrunnlagLagre()
            val virkningstidspunkt = YearMonth.of(2024, Month.AUGUST)

            avkorting.oppdaterMedInntektsgrunnlag(
                inntektDto,
                virkningstidspunkt,
                bruker,
            ).let {
                with(it.aarsoppgjoer.single().inntektsavkorting) {
                    size shouldBe 3
                    last().grunnlag.relevanteMaanederInnAar shouldBe 12
                }
            }
        }
    }
}
