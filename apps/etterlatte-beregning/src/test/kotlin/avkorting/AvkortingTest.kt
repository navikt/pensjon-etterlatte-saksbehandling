package no.nav.etterlatte.avkorting

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.avkortinggrunnlagLagreDto
import no.nav.etterlatte.beregning.regler.avkortingsperiode
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.beregning.regler.ytelseFoerAvkorting
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingOverstyrtInnvilgaMaanederDto
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.periode.Periode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Month
import java.time.YearMonth
import java.util.UUID

internal class AvkortingTest {
    @Nested
    inner class AvkortigTilDto {
        val avkorting =
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        AarsoppgjoerLoepende(
                            id = UUID.randomUUID(),
                            aar = 2024,
                            fom = YearMonth.of(2024, Month.MARCH),
                            inntektsavkorting =
                                listOf(
                                    Inntektsavkorting(
                                        grunnlag =
                                            avkortinggrunnlag(
                                                periode =
                                                    Periode(
                                                        fom = YearMonth.of(2024, Month.MARCH),
                                                        tom = YearMonth.of(2024, Month.JULY),
                                                    ),
                                                aarsinntekt = 300000,
                                            ),
                                    ),
                                    Inntektsavkorting(
                                        grunnlag =
                                            avkortinggrunnlag(
                                                periode =
                                                    Periode(
                                                        fom = YearMonth.of(2024, Month.AUGUST),
                                                        tom = null,
                                                    ),
                                                aarsinntekt = 350000,
                                            ),
                                    ),
                                ),
                            avkortetYtelse =
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
                        AarsoppgjoerLoepende(
                            id = UUID.randomUUID(),
                            aar = 2025,
                            fom = YearMonth.of(2025, Month.JANUARY),
                            inntektsavkorting =
                                listOf(
                                    Inntektsavkorting(
                                        grunnlag =
                                            avkortinggrunnlag(
                                                periode =
                                                    Periode(
                                                        fom = YearMonth.of(2025, Month.JANUARY),
                                                        tom = null,
                                                    ),
                                                aarsinntekt = 400000,
                                            ),
                                    ),
                                ),
                            avkortetYtelse =
                                listOf(
                                    avkortetYtelse(
                                        periode =
                                            Periode(
                                                fom = YearMonth.of(2025, Month.JANUARY),
                                                tom = null,
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        @Test
        fun `flater ut inntekter fra alle årsoppgjør`() {
            avkorting.toDto(fraVirkningstidspunkt = YearMonth.of(2024, Month.MAY)).asClue {
                it.avkortingGrunnlag.size shouldBe 3

                it.avkortingGrunnlag[0] shouldBe
                    avkorting.aarsoppgjoer[0]
                        .inntektsavkorting()[0]
                        .grunnlag
                        .toDto()
                it.avkortingGrunnlag[1] shouldBe
                    avkorting.aarsoppgjoer[0]
                        .inntektsavkorting()[1]
                        .grunnlag
                        .toDto()
                it.avkortingGrunnlag[2] shouldBe
                    avkorting.aarsoppgjoer[1]
                        .inntektsavkorting()[0]
                        .grunnlag
                        .toDto()
            }
        }

        @Test
        fun `fyller ut avkortet ytelse foer virkningstidspunkt ved aa kutte aarsoppgjoer fra virkningstidspunkt`() {
            avkorting.toDto(fraVirkningstidspunkt = YearMonth.of(2024, Month.MAY)).asClue {
                it.avkortetYtelse.size shouldBe 3

                it.avkortetYtelse[0] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[1].toDto()
                it.avkortetYtelse[1] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto()

                it.avkortetYtelse[2] shouldBe avkorting.aarsoppgjoer[1].avkortetYtelse[0].toDto()
            }
        }

        @Test
        fun `kutter periode fra aarsoppgjoer hvis virkningstidspunkt begynner midt i periode `() {
            avkorting.toDto(fraVirkningstidspunkt = YearMonth.of(2024, Month.APRIL)).asClue {
                it.avkortetYtelse.size shouldBe 4
                with(it.avkortetYtelse[0]) {
                    shouldBeEqualToIgnoringFields(
                        avkorting.aarsoppgjoer[0].avkortetYtelse[0].toDto(),
                        AvkortetYtelseDto::fom,
                        AvkortetYtelseDto::tom,
                    )
                    fom shouldBe YearMonth.of(2024, Month.APRIL)
                    tom shouldBe YearMonth.of(2024, Month.APRIL)
                }
                it.avkortetYtelse[1] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[1].toDto()
                it.avkortetYtelse[2] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto()
            }

            avkorting.toDto(fraVirkningstidspunkt = YearMonth.of(2024, Month.JUNE)).asClue {
                it.avkortetYtelse.size shouldBe 3
                with(it.avkortetYtelse[0]) {
                    shouldBeEqualToIgnoringFields(
                        avkorting.aarsoppgjoer[0].avkortetYtelse[1].toDto(),
                        AvkortetYtelseDto::fom,
                        AvkortetYtelseDto::tom,
                    )
                    fom shouldBe YearMonth.of(2024, Month.JUNE)
                    tom shouldBe YearMonth.of(2024, Month.JULY)
                }
                it.avkortetYtelse[1] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto()
            }

            avkorting
                .toDto(fraVirkningstidspunkt = YearMonth.of(2024, Month.SEPTEMBER))
                .asClue {
                    it.avkortetYtelse.size shouldBe 2
                    with(it.avkortetYtelse[0]) {
                        shouldBeEqualToIgnoringFields(
                            avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto(),
                            AvkortetYtelseDto::fom,
                            AvkortetYtelseDto::tom,
                        )
                        fom shouldBe YearMonth.of(2024, Month.SEPTEMBER)
                        tom shouldBe null
                    }
                }

            avkorting
                .toDto(fraVirkningstidspunkt = YearMonth.of(2025, Month.JANUARY))
                .asClue {
                    it.avkortetYtelse.size shouldBe 1
                    with(it.avkortetYtelse[0]) {
                        shouldBeEqualToIgnoringFields(
                            avkorting.aarsoppgjoer[1].avkortetYtelse[0].toDto(),
                            AvkortetYtelseDto::fom,
                            AvkortetYtelseDto::tom,
                        )
                        fom shouldBe YearMonth.of(2025, Month.JANUARY)
                        tom shouldBe null
                    }
                }
        }

        @Test
        fun `fyller ut alle perioder med avkortet ytelse hvis virkningstidspunkt ikke er angitt`() {
            avkorting.toDto(fraVirkningstidspunkt = null).asClue {
                it.avkortetYtelse.size shouldBe 4

                it.avkortetYtelse[0] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[0].toDto()
                it.avkortetYtelse[1] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[1].toDto()
                it.avkortetYtelse[2] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto()
                it.avkortetYtelse[3] shouldBe avkorting.aarsoppgjoer[1].avkortetYtelse[0].toDto()
            }
        }
    }

    @Nested
    inner class AvkortingTilFrontend {
        val inntektFraMars24 =
            avkortinggrunnlag(
                innvilgaMaaneder = 10,
                periode =
                    Periode(
                        fom = YearMonth.of(2024, Month.MARCH),
                        tom = YearMonth.of(2024, Month.JULY),
                    ),
                aarsinntekt = 300000,
            )
        val inntektFraAug24 =
            avkortinggrunnlag(
                innvilgaMaaneder = 10,
                periode =
                    Periode(
                        fom = YearMonth.of(2024, Month.AUGUST),
                        tom = null,
                    ),
                aarsinntekt = 350000,
            )
        val inntektFraJan25 =
            avkortinggrunnlag(
                innvilgaMaaneder = 12,
                periode =
                    Periode(
                        fom = YearMonth.of(2025, Month.JANUARY),
                        tom = null,
                    ),
                aarsinntekt = 400000,
            )
        val avkorting =
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        AarsoppgjoerLoepende(
                            id = UUID.randomUUID(),
                            aar = 2024,
                            fom = YearMonth.of(2024, Month.MARCH),
                            inntektsavkorting =
                                listOf(
                                    Inntektsavkorting(grunnlag = inntektFraMars24),
                                    Inntektsavkorting(grunnlag = inntektFraAug24),
                                ),
                            avkortetYtelse =
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
                        AarsoppgjoerLoepende(
                            id = UUID.randomUUID(),
                            aar = 2025,
                            fom = YearMonth.of(2025, Month.JANUARY),
                            inntektsavkorting =
                                listOf(
                                    Inntektsavkorting(
                                        grunnlag = inntektFraJan25,
                                    ),
                                ),
                            avkortetYtelse =
                                listOf(
                                    avkortetYtelse(
                                        periode =
                                            Periode(
                                                fom = YearMonth.of(2025, Month.JANUARY),
                                                tom = null,
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        @Test
        fun `fyller ut avkortingsgrunnlag per årsoppgjør`() {
            avkorting.toFrontend().asClue {
                it.avkortingGrunnlag.size shouldBe 2
                it.avkortingGrunnlag[0].aar shouldBe 2024
                it.avkortingGrunnlag[1].aar shouldBe 2025
            }
        }

        @Test
        fun `fyller ut alle felter til avkortingsgrunnlag`() {
            avkorting.toFrontend().avkortingGrunnlag.first().historikk.first().asClue {
                it.fom shouldBe inntektFraAug24.periode.fom
                it.tom shouldBe inntektFraAug24.periode.tom
                it.inntektTom shouldBe inntektFraAug24.inntektTom
                it.fratrekkInnAar shouldBe inntektFraAug24.fratrekkInnAar
                it.inntektUtlandTom shouldBe inntektFraAug24.inntektUtlandTom
                it.fratrekkInnAarUtland shouldBe inntektFraAug24.fratrekkInnAarUtland
                it.spesifikasjon shouldBe inntektFraAug24.spesifikasjon
                it.innvilgaMaaneder shouldBe 10
            }
        }

        @Test
        fun `avkortingGrunnlag med fom samme som virk skal legges i eget felt og ikke ligge i historikk`() {
            avkorting.toFrontend(YearMonth.of(2024, Month.AUGUST)).asClue {
                it.avkortingGrunnlag.size shouldBe 2

                it.avkortingGrunnlag[0].fraVirk shouldNotBe null
                it.avkortingGrunnlag[0].fraVirk!!.fom shouldBe inntektFraAug24.periode.fom
                it.avkortingGrunnlag[0].fraVirk!!.inntektTom shouldBe inntektFraAug24.inntektTom
                it.avkortingGrunnlag[0].historikk.size shouldBe 1

                it.avkortingGrunnlag[1].fraVirk shouldBe null
                it.avkortingGrunnlag[0].historikk.size shouldBe 1
            }
        }

        @Test
        fun `fyller ut historiske avkortingsgrunnlag i rekkefølge nyligste først`() {
            avkorting.toFrontend(YearMonth.of(2025, Month.FEBRUARY)).asClue {
                it.avkortingGrunnlag.size shouldBe 2

                it.avkortingGrunnlag[0].historikk.size shouldBe 2
                it.avkortingGrunnlag[0].historikk[0].fom shouldBe inntektFraAug24.periode.fom
                it.avkortingGrunnlag[0].historikk[0].inntektTom shouldBe inntektFraAug24.inntektTom
                it.avkortingGrunnlag[0].historikk[1].fom shouldBe inntektFraMars24.periode.fom
                it.avkortingGrunnlag[0].historikk[1].inntektTom shouldBe inntektFraMars24.inntektTom

                it.avkortingGrunnlag[1].historikk.size shouldBe 1
                it.avkortingGrunnlag[1].historikk[0].fom shouldBe inntektFraJan25.periode.fom
                it.avkortingGrunnlag[1].historikk[0].inntektTom shouldBe inntektFraJan25.inntektTom
            }
        }

        @Test
        fun `fyller ut avkortet ytelse foer virkningstidspunkt ved aa kutte aarsoppgjoer fra virkningstidspunkt`() {
            avkorting.toFrontend(fraVirkningstidspunkt = YearMonth.of(2024, Month.MAY)).asClue {
                it.avkortetYtelse.size shouldBe 3

                it.avkortetYtelse[0] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[1].toDto()
                it.avkortetYtelse[1] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto()

                it.avkortetYtelse[2] shouldBe avkorting.aarsoppgjoer[1].avkortetYtelse[0].toDto()
            }
        }

        @Test
        fun `kutter periode fra aarsoppgjoer hvis virkningstidspunkt begynner midt i periode `() {
            avkorting.toFrontend(fraVirkningstidspunkt = YearMonth.of(2024, Month.APRIL)).asClue {
                it.avkortetYtelse.size shouldBe 4
                with(it.avkortetYtelse[0]) {
                    shouldBeEqualToIgnoringFields(
                        avkorting.aarsoppgjoer[0].avkortetYtelse[0].toDto(),
                        AvkortetYtelseDto::fom,
                        AvkortetYtelseDto::tom,
                    )
                    fom shouldBe YearMonth.of(2024, Month.APRIL)
                    tom shouldBe YearMonth.of(2024, Month.APRIL)
                }
                it.avkortetYtelse[1] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[1].toDto()
                it.avkortetYtelse[2] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto()
            }

            avkorting.toFrontend(fraVirkningstidspunkt = YearMonth.of(2024, Month.JUNE)).asClue {
                it.avkortetYtelse.size shouldBe 3
                with(it.avkortetYtelse[0]) {
                    shouldBeEqualToIgnoringFields(
                        avkorting.aarsoppgjoer[0].avkortetYtelse[1].toDto(),
                        AvkortetYtelseDto::fom,
                        AvkortetYtelseDto::tom,
                    )
                    fom shouldBe YearMonth.of(2024, Month.JUNE)
                    tom shouldBe YearMonth.of(2024, Month.JULY)
                }
                it.avkortetYtelse[1] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto()
            }

            avkorting
                .toFrontend(fraVirkningstidspunkt = YearMonth.of(2024, Month.SEPTEMBER))
                .asClue {
                    it.avkortetYtelse.size shouldBe 2
                    with(it.avkortetYtelse[0]) {
                        shouldBeEqualToIgnoringFields(
                            avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto(),
                            AvkortetYtelseDto::fom,
                            AvkortetYtelseDto::tom,
                        )
                        fom shouldBe YearMonth.of(2024, Month.SEPTEMBER)
                        tom shouldBe null
                    }
                }

            avkorting
                .toFrontend(fraVirkningstidspunkt = YearMonth.of(2025, Month.JANUARY))
                .asClue {
                    it.avkortetYtelse.size shouldBe 1
                    with(it.avkortetYtelse[0]) {
                        shouldBeEqualToIgnoringFields(
                            avkorting.aarsoppgjoer[1].avkortetYtelse[0].toDto(),
                            AvkortetYtelseDto::fom,
                            AvkortetYtelseDto::tom,
                        )
                        fom shouldBe YearMonth.of(2025, Month.JANUARY)
                        tom shouldBe null
                    }
                }
        }

        @Test
        fun `fyller ut alle perioder med avkortet ytelse hvis virkningstidspunkt ikke er angitt`() {
            avkorting.toFrontend(fraVirkningstidspunkt = null).asClue {
                it.avkortetYtelse.size shouldBe 4

                it.avkortetYtelse[0] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[0].toDto()
                it.avkortetYtelse[1] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[1].toDto()
                it.avkortetYtelse[2] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto()
                it.avkortetYtelse[3] shouldBe avkorting.aarsoppgjoer[1].avkortetYtelse[0].toDto()
            }
        }

        @Test
        fun `fyller ut tidligereAvkortetYtelse`() {
            avkorting
                .toFrontend(
                    fraVirkningstidspunkt = YearMonth.of(2024, Month.MAY),
                    forrigeAvkorting = avkorting,
                    behandlinStatus = BehandlingStatus.BEREGNET,
                ).asClue {
                    it.tidligereAvkortetYtelse.size shouldBe 4

                    it.tidligereAvkortetYtelse[0] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[0].toDto()
                    it.tidligereAvkortetYtelse[1] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[1].toDto()
                    it.tidligereAvkortetYtelse[2] shouldBe avkorting.aarsoppgjoer[0].avkortetYtelse[2].toDto()

                    it.tidligereAvkortetYtelse[3] shouldBe avkorting.aarsoppgjoer[1].avkortetYtelse[0].toDto()
                }
        }

        @Test
        fun `fyller ikke ut tidligereAvkortetYtelse hvis status iverksatt `() {
            avkorting
                .toFrontend(
                    fraVirkningstidspunkt = YearMonth.of(2024, Month.MAY),
                    forrigeAvkorting = avkorting,
                    behandlinStatus = BehandlingStatus.IVERKSATT,
                ).asClue {
                    it.tidligereAvkortetYtelse.size shouldBe 0
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
                        AarsoppgjoerLoepende(
                            id = UUID.randomUUID(),
                            aar = 2024,
                            fom = YearMonth.of(2024, 1),
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
                                    Inntektsavkorting(
                                        avkortinggrunnlag(
                                            periode =
                                                Periode(
                                                    fom = YearMonth.of(2024, 1),
                                                    tom = null,
                                                ),
                                        ),
                                    ),
                                    Inntektsavkorting(
                                        avkortinggrunnlag(
                                            periode =
                                                Periode(
                                                    fom = YearMonth.of(2024, 2),
                                                    tom = null,
                                                ),
                                        ),
                                    ),
                                ),
                        ),
                    ),
            )

        private val nyAvkorting = eksisterendeAvkorting.kopierAvkorting()

        @Test
        fun `Skal kopiere tidligere inntekt men erstatte id`() {
            with(nyAvkorting) {
                val aarsoppgjoer = aarsoppgjoer.single()
                aarsoppgjoer.id shouldNotBe eksisterendeAvkorting.aarsoppgjoer.single().id
                aarsoppgjoer.inntektsavkorting().asClue {
                    it.size shouldBe 2
                    it[0].grunnlag.id shouldNotBe
                        eksisterendeAvkorting.aarsoppgjoer
                            .single()
                            .inntektsavkorting()[0]
                            .grunnlag.id
                    it[1].grunnlag.id shouldNotBe
                        eksisterendeAvkorting.aarsoppgjoer
                            .single()
                            .inntektsavkorting()[1]
                            .grunnlag.id
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
        @Nested
        inner class Foerstegangsbehandling {
            @Test
            fun `Skal opprette nytt årsoppgjør med angitt foventet inntekt`() {
                val forventetInntekt =
                    avkortinggrunnlagLagreDto(
                        aarsinntekt = 200000,
                        fom = YearMonth.of(2024, Month.MARCH),
                    )

                val opprettaAvkorting =
                    Avkorting().oppdaterMedInntektsgrunnlag(
                        forventetInntekt,
                        bruker,
                    )

                opprettaAvkorting.aarsoppgjoer.single().shouldBeEqualToIgnoringFields(
                    aarsoppgjoer(
                        aar = 2024,
                        fom = YearMonth.of(2024, 3),
                    ),
                    AarsoppgjoerLoepende::id,
                    AarsoppgjoerLoepende::inntektsavkorting,
                )
                with(opprettaAvkorting.aarsoppgjoer.single().inntektsavkorting()) {
                    size shouldBe 1
                    with(get(0).grunnlag) {
                        inntektTom shouldBe forventetInntekt.inntektTom
                        fratrekkInnAar shouldBe forventetInntekt.fratrekkInnAar
                        inntektUtlandTom shouldBe forventetInntekt.inntektUtlandTom
                        fratrekkInnAarUtland shouldBe forventetInntekt.fratrekkInnAarUtland
                        spesifikasjon shouldBe forventetInntekt.spesifikasjon
                    }
                }
            }

            @Test
            fun `Skal opprette nytt årsoppgjør med angitt foventet inntekt og droppe fom da den er i neste år`() {
                val fomAar = 2024
                val forventetInntekt =
                    avkortinggrunnlagLagreDto(
                        aarsinntekt = 200000,
                        fom = YearMonth.of(fomAar, Month.MARCH),
                    )

                val opprettaAvkorting =
                    Avkorting().oppdaterMedInntektsgrunnlag(
                        forventetInntekt,
                        bruker,
                        opphoerFom = YearMonth.of(fomAar.plus(1), Month.MARCH),
                    )

                opprettaAvkorting.aarsoppgjoer.single().shouldBeEqualToIgnoringFields(
                    aarsoppgjoer(
                        aar = 2024,
                        fom = YearMonth.of(2024, 3),
                    ),
                    AarsoppgjoerLoepende::id,
                    AarsoppgjoerLoepende::inntektsavkorting,
                )
                with(opprettaAvkorting.aarsoppgjoer.single().inntektsavkorting()) {
                    size shouldBe 1
                    with(get(0).grunnlag) {
                        inntektTom shouldBe forventetInntekt.inntektTom
                        fratrekkInnAar shouldBe forventetInntekt.fratrekkInnAar
                        inntektUtlandTom shouldBe forventetInntekt.inntektUtlandTom
                        fratrekkInnAarUtland shouldBe forventetInntekt.fratrekkInnAarUtland
                        spesifikasjon shouldBe forventetInntekt.spesifikasjon
                    }
                }
            }

            @Test
            fun `Skal feile hvis opphøer er satt tilbake i tid `() {
                val fomAar = 2024
                val fom = YearMonth.of(fomAar, Month.MARCH)
                val forventetInntekt =
                    avkortinggrunnlagLagreDto(
                        aarsinntekt = 200000,
                        fom = fom,
                    )

                assertThrows<OpphoerErTilbakeITid> {
                    Avkorting().oppdaterMedInntektsgrunnlag(
                        forventetInntekt,
                        bruker,
                        opphoerFom = YearMonth.of(fomAar.minus(1), Month.MARCH),
                    )
                }
            }
        }

        @Test
        fun `Skal opprette nytt årsoppgjør inntekt og droppe fom da den er i neste år og ta den med i neste års `() {
            val fomAar = 2024
            val fomYearMonth = YearMonth.of(2024, Month.MARCH)
            val forventetInntekt =
                avkortinggrunnlagLagreDto(
                    aarsinntekt = 200000,
                    fom = fomYearMonth,
                )

            val tomAar = fomAar.plus(1)
            val tomYearMonth = YearMonth.of(tomAar, Month.MARCH)
            val opprettaAvkortingForstegangsbehandling =
                Avkorting().oppdaterMedInntektsgrunnlag(
                    forventetInntekt,
                    bruker,
                    opphoerFom = tomYearMonth,
                )

            opprettaAvkortingForstegangsbehandling.aarsoppgjoer.single().shouldBeEqualToIgnoringFields(
                aarsoppgjoer(
                    aar = fomAar,
                    fom = fomYearMonth,
                ),
                AarsoppgjoerLoepende::id,
                AarsoppgjoerLoepende::inntektsavkorting,
            )
            with(opprettaAvkortingForstegangsbehandling.aarsoppgjoer.single().inntektsavkorting()) {
                size shouldBe 1
                with(get(0).grunnlag) {
                    inntektTom shouldBe forventetInntekt.inntektTom
                    fratrekkInnAar shouldBe forventetInntekt.fratrekkInnAar
                    inntektUtlandTom shouldBe forventetInntekt.inntektUtlandTom
                    fratrekkInnAarUtland shouldBe forventetInntekt.fratrekkInnAarUtland
                    spesifikasjon shouldBe forventetInntekt.spesifikasjon
                }
            }

            val forventetInntektRevurdering =
                avkortinggrunnlagLagreDto(
                    aarsinntekt = 200000,
                    fom = YearMonth.of(tomAar, Month.JANUARY),
                )
            val revurdert =
                Avkorting(opprettaAvkortingForstegangsbehandling.aarsoppgjoer).oppdaterMedInntektsgrunnlag(
                    forventetInntektRevurdering,
                    bruker,
                    opphoerFom = tomYearMonth,
                )
            revurdert.aarsoppgjoer.size shouldBe 2
            val foerstegangsbehandlingen = revurdert.aarsoppgjoer.first()
            foerstegangsbehandlingen.aar shouldBe fomAar
            foerstegangsbehandlingen
                .inntektsavkorting()
                .first()
                .grunnlag.periode shouldBe
                Periode(fomYearMonth, YearMonth.of(fomAar, Month.DECEMBER))
            val revurderingen = revurdert.aarsoppgjoer[1]
            revurderingen.aar shouldBe tomAar
            revurderingen
                .inntektsavkorting()
                .first()
                .grunnlag.periode shouldBe
                Periode(YearMonth.of(tomAar, Month.JANUARY), YearMonth.of(tomAar, Month.FEBRUARY))
        }

        @Nested
        inner class Revurdering {
            private val foersteInntekt =
                avkortinggrunnlag(
                    periode = Periode(fom = YearMonth.of(2024, Month.JANUARY), tom = YearMonth.of(2024, Month.MARCH)),
                )
            private val andreInntekt =
                avkortinggrunnlag(
                    aarsinntekt = 1000000,
                    periode = Periode(fom = YearMonth.of(2024, Month.APRIL), tom = null),
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
                val endretInntekt =
                    avkortinggrunnlagLagreDto(
                        id = andreInntekt.id,
                        aarsinntekt = 200000,
                        fom = YearMonth.of(2024, Month.MARCH),
                    )

                val oppdatertAvkorting =
                    avkorting.oppdaterMedInntektsgrunnlag(
                        endretInntekt,
                        bruker,
                    )

                oppdatertAvkorting.shouldBeEqualToIgnoringFields(avkorting, Avkorting::aarsoppgjoer)
                oppdatertAvkorting.aarsoppgjoer.single().shouldBeEqualToIgnoringFields(
                    avkorting.aarsoppgjoer.single(),
                    AarsoppgjoerLoepende::inntektsavkorting,
                )
                with(oppdatertAvkorting.aarsoppgjoer.single().inntektsavkorting()) {
                    size shouldBe 2
                    get(0).grunnlag shouldBe foersteInntekt
                    with(get(1).grunnlag) {
                        inntektTom shouldBe endretInntekt.inntektTom
                        fratrekkInnAar shouldBe endretInntekt.fratrekkInnAar
                        inntektUtlandTom shouldBe endretInntekt.inntektUtlandTom
                        fratrekkInnAarUtland shouldBe endretInntekt.fratrekkInnAarUtland
                        spesifikasjon shouldBe endretInntekt.spesifikasjon
                    }
                }
            }

            @Test
            fun `Eksisterer ikke inntekt skal det legges til og til og med paa periode til siste inntekt skal settes`() {
                val nyttGrunnlag =
                    avkortinggrunnlagLagreDto(
                        fom = YearMonth.of(2024, Month.AUGUST),
                    )

                val oppdatertAvkorting =
                    avkorting.oppdaterMedInntektsgrunnlag(
                        nyttGrunnlag,
                        bruker,
                    )

                oppdatertAvkorting.shouldBeEqualToIgnoringFields(avkorting, Avkorting::aarsoppgjoer)
                oppdatertAvkorting.aarsoppgjoer.single().shouldBeEqualToIgnoringFields(
                    avkorting.aarsoppgjoer.single(),
                    AarsoppgjoerLoepende::inntektsavkorting,
                )
                with(oppdatertAvkorting.aarsoppgjoer.single().inntektsavkorting()) {
                    size shouldBe 3
                    get(0).grunnlag shouldBe foersteInntekt
                    get(1).grunnlag.shouldBeEqualToIgnoringFields(andreInntekt, ForventetInntekt::periode)
                    get(1).grunnlag.periode shouldBe
                        Periode(
                            fom = YearMonth.of(2024, Month.APRIL),
                            tom = YearMonth.of(2024, Month.JULY),
                        )
                    with(get(2).grunnlag) {
                        inntektTom shouldBe nyttGrunnlag.inntektTom
                        fratrekkInnAar shouldBe nyttGrunnlag.fratrekkInnAar
                        inntektUtlandTom shouldBe nyttGrunnlag.inntektUtlandTom
                        fratrekkInnAarUtland shouldBe nyttGrunnlag.fratrekkInnAarUtland
                        spesifikasjon shouldBe nyttGrunnlag.spesifikasjon
                    }
                }
            }

            @Test
            fun `Ny inntekt for et aarsoppgjoer som ikke finnes enda skal opprette det nye aaret`() {
                val nyttGrunnlag =
                    avkortinggrunnlagLagreDto(
                        aarsinntekt = 150000,
                        fom = YearMonth.of(2025, Month.JANUARY),
                    )

                val oppdatertAvkorting =
                    avkorting.oppdaterMedInntektsgrunnlag(
                        nyttGrunnlag,
                        bruker,
                    )

                oppdatertAvkorting.shouldBeEqualToIgnoringFields(avkorting, Avkorting::aarsoppgjoer)
                with(oppdatertAvkorting.aarsoppgjoer[0]) {
                    shouldBeEqualToIgnoringFields(
                        avkorting.aarsoppgjoer[0],
                        AarsoppgjoerLoepende::inntektsavkorting,
                        AarsoppgjoerLoepende::id,
                    )
                    with(inntektsavkorting()) {
                        size shouldBe 2
                        get(0).grunnlag shouldBe foersteInntekt
                        get(1).grunnlag.shouldBeEqualToIgnoringFields(andreInntekt, ForventetInntekt::periode)
                        get(1).grunnlag.periode shouldBe
                            Periode(
                                fom = YearMonth.of(2024, Month.APRIL),
                                tom = null,
                                // tom = YearMonth.of(2024, Month.DECEMBER), TODO er dette nødvendig?
                            )
                    }
                }
                with(oppdatertAvkorting.aarsoppgjoer[1]) {
                    shouldBeEqualToIgnoringFields(
                        aarsoppgjoer(
                            aar = 2025,
                            fom = YearMonth.of(2025, 1),
                        ),
                        AarsoppgjoerLoepende::inntektsavkorting,
                        AarsoppgjoerLoepende::id,
                    )
                    with(inntektsavkorting()) {
                        size shouldBe 1
                        with(get(0).grunnlag) {
                            inntektTom shouldBe nyttGrunnlag.inntektTom
                            fratrekkInnAar shouldBe nyttGrunnlag.fratrekkInnAar
                            inntektUtlandTom shouldBe nyttGrunnlag.inntektUtlandTom
                            fratrekkInnAarUtland shouldBe nyttGrunnlag.fratrekkInnAarUtland
                            spesifikasjon shouldBe nyttGrunnlag.spesifikasjon
                        }
                    }
                }
            }
        }
    }

    @Nested
    inner class SorterePerioder {
        @Test
        fun `Avkorting skal alltid sortere aarsoppgjoer ascending på år`() {
            assertThrows<InternfeilException> {
                Avkorting(
                    listOf(
                        AarsoppgjoerLoepende(aar = 2025, id = UUID.randomUUID(), fom = YearMonth.of(2025, 1)),
                        AarsoppgjoerLoepende(aar = 2024, id = UUID.randomUUID(), fom = YearMonth.of(2024, 1)),
                    ),
                )
            }
        }

        @Test
        fun `Aarsoppgjoer skal alltid sortere ytelseFoerAvkorting ascending på fom`() {
            assertThrows<InternfeilException> {
                val ytelseFoerAvkorting =
                    listOf(
                        ytelseFoerAvkorting(periode = Periode(fom = YearMonth.of(2024, 2), tom = null)),
                        ytelseFoerAvkorting(periode = Periode(fom = YearMonth.of(2024, 1), tom = null)),
                    )
                Avkorting(
                    listOf(
                        AarsoppgjoerLoepende(
                            ytelseFoerAvkorting = ytelseFoerAvkorting,
                            aar = 2024,
                            id = UUID.randomUUID(),
                            fom = YearMonth.of(2024, 1),
                        ),
                    ),
                )
            }
        }

        @Test
        fun `Aarsoppgjoer skal alltid sortere inntektsavkorting ascending på fom`() {
            assertThrows<InternfeilException> {
                val inntektsavkorting =
                    listOf(
                        Inntektsavkorting(
                            avkortinggrunnlag(
                                periode =
                                    Periode(
                                        fom = YearMonth.of(2024, 2),
                                        tom = null,
                                    ),
                            ),
                        ),
                        Inntektsavkorting(
                            avkortinggrunnlag(
                                periode =
                                    Periode(
                                        fom = YearMonth.of(2024, 1),
                                        tom = null,
                                    ),
                            ),
                        ),
                    )
                Avkorting(
                    listOf(
                        AarsoppgjoerLoepende(
                            inntektsavkorting = inntektsavkorting,
                            aar = 2024,
                            id = UUID.randomUUID(),
                            fom = YearMonth.of(2024, 1),
                        ),
                    ),
                )
            }
        }

        @Test
        fun `Aarsoppgjoer skal alltid sortere avkortetYtelse ascending på fom`() {
            assertThrows<InternfeilException> {
                val avkortetYtelse =
                    listOf(
                        avkortetYtelse(periode = Periode(fom = YearMonth.of(2024, 2), tom = null)),
                        avkortetYtelse(periode = Periode(fom = YearMonth.of(2024, 1), tom = null)),
                    )
                Avkorting(
                    listOf(
                        AarsoppgjoerLoepende(
                            avkortetYtelse = avkortetYtelse,
                            aar = 2024,
                            id = UUID.randomUUID(),
                            fom = YearMonth.of(2024, 1),
                        ),
                    ),
                )
            }
        }

        @Test
        fun `Inntektsavkorting skal alltid sortere avkortingsperioder ascending på fom`() {
            assertThrows<InternfeilException> {
                val avkortingsperioder =
                    listOf(
                        avkortingsperiode(fom = YearMonth.of(2024, 2), tom = null),
                        avkortingsperiode(fom = YearMonth.of(2024, 1), tom = null),
                    )
                Inntektsavkorting(
                    grunnlag = avkortinggrunnlag(),
                    avkortingsperioder = avkortingsperioder,
                )
            }
        }

        @Test
        fun `Inntektsavkorting skal alltid sortere avkortetYtelseForventetInntekt ascending på fom`() {
            assertThrows<InternfeilException> {
                val avkortetYtelseForventetInntekt =
                    listOf(
                        avkortetYtelse(periode = Periode(fom = YearMonth.of(2024, 2), tom = null)),
                        avkortetYtelse(periode = Periode(fom = YearMonth.of(2024, 1), tom = null)),
                    )
                Inntektsavkorting(
                    grunnlag = avkortinggrunnlag(),
                    avkortetYtelseForventetInntekt = avkortetYtelseForventetInntekt,
                )
            }
        }
    }

    @Nested
    inner class InnvilgaMaaneder {
        @Test
        fun `utledning av innvilga måneder`() {
            val grunnlag =
                avkortinggrunnlagLagreDto(
                    fom = YearMonth.of(2024, 3),
                )
            val opprettaAvkorting =
                Avkorting().oppdaterMedInntektsgrunnlag(
                    grunnlag,
                    bruker,
                )
            with(
                opprettaAvkorting.aarsoppgjoer
                    .single()
                    .inntektsavkorting()
                    .single()
                    .grunnlag,
            ) {
                innvilgaMaaneder shouldBe 10
                overstyrtInnvilgaMaanederAarsak shouldBe null
                overstyrtInnvilgaMaanederBegrunnelse shouldBe null
            }
        }

        @Test
        fun `utledning av innvilga måneder med opphør`() {
            val grunnlag =
                avkortinggrunnlagLagreDto(
                    fom = YearMonth.of(2024, 3),
                )
            val opprettaAvkorting =
                Avkorting().oppdaterMedInntektsgrunnlag(
                    nyttGrunnlag = grunnlag,
                    bruker = bruker,
                    opphoerFom = YearMonth.of(2024, 7),
                )
            with(
                opprettaAvkorting.aarsoppgjoer
                    .single()
                    .inntektsavkorting()
                    .single()
                    .grunnlag,
            ) {
                innvilgaMaaneder shouldBe 4
                overstyrtInnvilgaMaanederAarsak shouldBe null
                overstyrtInnvilgaMaanederBegrunnelse shouldBe null
            }
        }

        @Test
        fun `utledning av innvilga måneder med aldersoverang`() {
            val grunnlag =
                avkortinggrunnlagLagreDto(
                    fom = YearMonth.of(2024, 3),
                )
            val opprettaAvkorting =
                Avkorting().oppdaterMedInntektsgrunnlag(
                    nyttGrunnlag = grunnlag,
                    bruker = bruker,
                    opphoerFom = null,
                    aldersovergang = YearMonth.of(2024, 8),
                )
            with(
                opprettaAvkorting.aarsoppgjoer
                    .single()
                    .inntektsavkorting()
                    .single()
                    .grunnlag,
            ) {
                innvilgaMaaneder shouldBe 5
                overstyrtInnvilgaMaanederAarsak shouldBe OverstyrtInnvilgaMaanederAarsak.BLIR_67
                overstyrtInnvilgaMaanederBegrunnelse shouldNotBe null
            }
        }

        @Test
        fun `utledning av overstyrt innvilga måneder`() {
            val grunnlag =
                avkortinggrunnlagLagreDto(
                    fom = YearMonth.of(2024, 3),
                    overstyrtInnvilgaMaaneder =
                        AvkortingOverstyrtInnvilgaMaanederDto(
                            antall = 5,
                            aarsak = OverstyrtInnvilgaMaanederAarsak.TAR_UT_PENSJON_TIDLIG.name,
                            begrunnelse = "Begrunnelse",
                        ),
                )
            val opprettaAvkorting =
                Avkorting().oppdaterMedInntektsgrunnlag(
                    nyttGrunnlag = grunnlag,
                    bruker = bruker,
                )
            with(
                opprettaAvkorting.aarsoppgjoer
                    .single()
                    .inntektsavkorting()
                    .single()
                    .grunnlag,
            ) {
                innvilgaMaaneder shouldBe 5
                overstyrtInnvilgaMaanederAarsak shouldBe OverstyrtInnvilgaMaanederAarsak.TAR_UT_PENSJON_TIDLIG
                overstyrtInnvilgaMaanederBegrunnelse shouldBe "Begrunnelse"
            }
        }
    }
}
