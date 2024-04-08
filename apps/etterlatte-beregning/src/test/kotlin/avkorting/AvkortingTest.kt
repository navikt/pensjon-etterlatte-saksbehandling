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
import no.nav.etterlatte.beregning.regler.beregning
import no.nav.etterlatte.beregning.regler.beregningsperiode
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.beregning.regler.restanse
import no.nav.etterlatte.libs.common.behandling.BehandlingType
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
            )

        @Test
        fun `fyller ut avkortet ytelse foer virkningstidspunkt ved aa kutte aarsoppgjoer fra virkningstidspunkt`() {
            avkorting.medYtelseFraOgMedVirkningstidspunkt(virkningstidspunkt = YearMonth.of(2024, Month.MAY)).asClue {
                it.avkortetYtelseFraVirkningstidspunkt.size shouldBe 2
                it.avkortetYtelseFraVirkningstidspunkt[0] shouldBe avkorting.aarsoppgjoer.avkortetYtelseAar[1]
                it.avkortetYtelseFraVirkningstidspunkt[1] shouldBe avkorting.aarsoppgjoer.avkortetYtelseAar[2]
            }
        }

        @Test
        fun `kutter periode fra aarsoppgjoer hvis virkningstidspunkt begynner midt i periode `() {
            avkorting.medYtelseFraOgMedVirkningstidspunkt(virkningstidspunkt = YearMonth.of(2024, Month.APRIL)).asClue {
                it.avkortetYtelseFraVirkningstidspunkt.size shouldBe 3
                with(it.avkortetYtelseFraVirkningstidspunkt[0]) {
                    shouldBeEqualToIgnoringFields(avkorting.aarsoppgjoer.avkortetYtelseAar[0], AvkortetYtelse::periode)
                    periode shouldBe
                        Periode(
                            fom = YearMonth.of(2024, Month.APRIL),
                            tom = YearMonth.of(2024, Month.APRIL),
                        )
                }
                it.avkortetYtelseFraVirkningstidspunkt[1] shouldBe avkorting.aarsoppgjoer.avkortetYtelseAar[1]
                it.avkortetYtelseFraVirkningstidspunkt[2] shouldBe avkorting.aarsoppgjoer.avkortetYtelseAar[2]
            }

            avkorting.medYtelseFraOgMedVirkningstidspunkt(virkningstidspunkt = YearMonth.of(2024, Month.JUNE)).asClue {
                it.avkortetYtelseFraVirkningstidspunkt.size shouldBe 2
                with(it.avkortetYtelseFraVirkningstidspunkt[0]) {
                    shouldBeEqualToIgnoringFields(avkorting.aarsoppgjoer.avkortetYtelseAar[1], AvkortetYtelse::periode)
                    periode shouldBe Periode(fom = YearMonth.of(2024, Month.JUNE), tom = YearMonth.of(2024, Month.JULY))
                }
                it.avkortetYtelseFraVirkningstidspunkt[1] shouldBe avkorting.aarsoppgjoer.avkortetYtelseAar[2]
            }

            avkorting.medYtelseFraOgMedVirkningstidspunkt(virkningstidspunkt = YearMonth.of(2024, Month.SEPTEMBER))
                .asClue {
                    it.avkortetYtelseFraVirkningstidspunkt.size shouldBe 1
                    with(it.avkortetYtelseFraVirkningstidspunkt[0]) {
                        shouldBeEqualToIgnoringFields(
                            avkorting.aarsoppgjoer.avkortetYtelseAar[2],
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
            )

        private val nyAvkorting = eksisterendeAvkorting.kopierAvkorting()

        @Test
        fun `Skal kopiere tidligere grunnlag men erstatte id`() {
            with(nyAvkorting) {
                aarsoppgjoer.inntektsavkorting.asClue {
                    it.size shouldBe 2
                    it[0].grunnlag.id shouldNotBe eksisterendeAvkorting.aarsoppgjoer.inntektsavkorting[0].grunnlag.id
                    it[1].grunnlag.id shouldNotBe eksisterendeAvkorting.aarsoppgjoer.inntektsavkorting[1].grunnlag.id
                }
            }
        }

        @Test
        fun `Skal kopiere ytelse foer avkorting til aarsoppgjoer`() {
            nyAvkorting.asClue {
                it.aarsoppgjoer.ytelseFoerAvkorting.shouldContainExactly(
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
        private val foersteGrunnlag =
            avkortinggrunnlag(
                periode = Periode(fom = YearMonth.of(2024, Month.JANUARY), tom = YearMonth.of(2024, Month.MARCH)),
                relevanteMaanederInnAar = 12,
            )
        private val andreGrunnlag =
            avkortinggrunnlag(
                aarsinntekt = 1000000,
                periode = Periode(fom = YearMonth.of(2024, Month.APRIL), tom = null),
                relevanteMaanederInnAar = 12,
            )
        private val avkorting =
            avkorting(
                inntektsavkorting =
                    listOf(
                        Inntektsavkorting(foersteGrunnlag),
                        Inntektsavkorting(andreGrunnlag),
                    ),
            )

        @Test
        fun `Eksisterer det grunnlag med samme id skal eksisterende grunnlag oppdateres uten aa legge til nytt`() {
            val endretGrunnlag = avkortinggrunnlagLagre(id = andreGrunnlag.id, aarsinntekt = 200000)
            val virkningstidspunkt = YearMonth.of(2024, Month.MARCH)

            val oppdatertAvkorting =
                avkorting.oppdaterMedInntektsgrunnlag(
                    endretGrunnlag,
                    virkningstidspunkt,
                    bruker,
                )

            oppdatertAvkorting.shouldBeEqualToIgnoringFields(avkorting, Avkorting::aarsoppgjoer)
            oppdatertAvkorting.aarsoppgjoer.shouldBeEqualToIgnoringFields(
                avkorting.aarsoppgjoer,
                Aarsoppgjoer::inntektsavkorting,
            )
            with(oppdatertAvkorting.aarsoppgjoer.inntektsavkorting) {
                size shouldBe 2
                get(0).grunnlag shouldBe foersteGrunnlag
                with(get(1).grunnlag) {
                    aarsinntekt shouldBe endretGrunnlag.aarsinntekt
                    fratrekkInnAar shouldBe endretGrunnlag.fratrekkInnAar
                    inntektUtland shouldBe endretGrunnlag.inntektUtland
                    fratrekkInnAarUtland shouldBe endretGrunnlag.fratrekkInnAarUtland
                    spesifikasjon shouldBe endretGrunnlag.spesifikasjon
                }
            }
        }

        @Test
        fun `Eksisterer ikke grunnlag skal det legges til og til og med paa periode til siste grunnlag skal settes`() {
            val nyttGrunnlag = avkortinggrunnlagLagre()
            val virkningstidspunkt = YearMonth.of(2024, Month.AUGUST)

            val oppdatertAvkorting =
                avkorting.oppdaterMedInntektsgrunnlag(
                    nyttGrunnlag,
                    virkningstidspunkt,
                    bruker,
                )

            oppdatertAvkorting.shouldBeEqualToIgnoringFields(avkorting, Avkorting::aarsoppgjoer)
            oppdatertAvkorting.aarsoppgjoer.shouldBeEqualToIgnoringFields(
                avkorting.aarsoppgjoer,
                Aarsoppgjoer::inntektsavkorting,
            )
            with(oppdatertAvkorting.aarsoppgjoer.inntektsavkorting) {
                size shouldBe 3
                get(0).grunnlag shouldBe foersteGrunnlag
                get(1).grunnlag.shouldBeEqualToIgnoringFields(andreGrunnlag, AvkortingGrunnlag::periode)
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
            val grunnlag = avkortinggrunnlagLagre()
            val virkningstidspunkt = YearMonth.of(2024, Month.MARCH)

            avkorting(inntektsavkorting = emptyList()).oppdaterMedInntektsgrunnlag(
                grunnlag,
                virkningstidspunkt,
                bruker,
            ).let {
                it.aarsoppgjoer.inntektsavkorting.single().grunnlag.relevanteMaanederInnAar shouldBe 10
            }
        }

        @Test
        fun `Relvante maaneder skal skal viderfoeres fra forrige inntekt for samme aar`() {
            val grunnlag = avkortinggrunnlagLagre()
            val virkningstidspunkt = YearMonth.of(2024, Month.AUGUST)

            avkorting.oppdaterMedInntektsgrunnlag(
                grunnlag,
                virkningstidspunkt,
                bruker,
            ).let {
                with(it.aarsoppgjoer.inntektsavkorting) {
                    size shouldBe 3
                    last().grunnlag.relevanteMaanederInnAar shouldBe 12
                }
            }
        }
    }

    @Nested
    inner class BeregnAvkorting {
        @Test
        fun `Beregner avkortet ytelse for foerstegangsbehandling`() {
            val avkorting = `Avkorting foerstegangsbehandling`()
            with(avkorting.aarsoppgjoer.avkortetYtelseAar) {
                size shouldBe 2
                get(0).shouldBeEqualToIgnoringFields(
                    avkortetYtelse(
                        periode = Periode(fom = YearMonth.of(2024, Month.MARCH), tom = YearMonth.of(2024, Month.APRIL)),
                        ytelseEtterAvkorting = 6650,
                        ytelseEtterAvkortingFoerRestanse = 6650,
                        restanse = null,
                        avkortingsbeloep = 9026,
                        ytelseFoerAvkorting = 15676,
                        type = AvkortetYtelseType.AARSOPPGJOER,
                        inntektsgrunnlag = null,
                    ),
                    AvkortetYtelse::id,
                    AvkortetYtelse::tidspunkt,
                    AvkortetYtelse::regelResultat,
                    AvkortetYtelse::kilde,
                )
                get(1).shouldBeEqualToIgnoringFields(
                    avkortetYtelse(
                        periode = Periode(fom = YearMonth.of(2024, Month.MAY), tom = null),
                        ytelseEtterAvkorting = 7656,
                        ytelseEtterAvkortingFoerRestanse = 7656,
                        restanse = null,
                        avkortingsbeloep = 9026,
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
        fun `Revurdering inntektsendring - foerste inntektsendring`() {
            val avkorting = `Avkorting ny inntekt en`()
            with(avkorting.aarsoppgjoer.avkortetYtelseAar) {
                size shouldBe 3
                get(0).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode =
                                Periode(
                                    fom = YearMonth.of(2024, Month.MARCH),
                                    tom = YearMonth.of(2024, Month.APRIL),
                                ),
                            ytelseEtterAvkorting = 6650,
                            ytelseEtterAvkortingFoerRestanse = 6650,
                            avkortingsbeloep = 9026,
                            ytelseFoerAvkorting = 15676,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse shouldBe null
                }
                get(1).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode =
                                Periode(
                                    fom = YearMonth.of(2024, Month.MAY),
                                    tom = YearMonth.of(2024, Month.JUNE),
                                ),
                            ytelseEtterAvkorting = 7656,
                            ytelseEtterAvkortingFoerRestanse = 7656,
                            avkortingsbeloep = 9026,
                            ytelseFoerAvkorting = 16682,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse shouldBe null
                }
                get(2).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode = Periode(fom = YearMonth.of(2024, Month.JULY), tom = null),
                            ytelseEtterAvkorting = 156,
                            ytelseEtterAvkortingFoerRestanse = 3156,
                            avkortingsbeloep = 13526,
                            ytelseFoerAvkorting = 16682,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse!!.shouldBeEqualToIgnoringFields(
                        restanse(
                            totalRestanse = 18000,
                            fordeltRestanse = 3000,
                        ),
                        Restanse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                    )
                }
            }
        }

        @Test
        fun `Revurdering inntektsendring - andre inntektsendring`() {
            val avkorting = `Avkorting ny inntekt to`()
            with(avkorting.aarsoppgjoer.avkortetYtelseAar) {
                size shouldBe 4
                get(0).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode =
                                Periode(
                                    fom = YearMonth.of(2024, Month.MARCH),
                                    tom = YearMonth.of(2024, Month.APRIL),
                                ),
                            ytelseEtterAvkorting = 6650,
                            ytelseEtterAvkortingFoerRestanse = 6650,
                            avkortingsbeloep = 9026,
                            ytelseFoerAvkorting = 15676,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse shouldBe null
                }
                get(1).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode =
                                Periode(
                                    fom = YearMonth.of(2024, Month.MAY),
                                    tom = YearMonth.of(2024, Month.JUNE),
                                ),
                            ytelseEtterAvkorting = 7656,
                            ytelseEtterAvkortingFoerRestanse = 7656,
                            avkortingsbeloep = 9026,
                            ytelseFoerAvkorting = 16682,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse shouldBe null
                }
                get(2).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode =
                                Periode(
                                    fom = YearMonth.of(2024, Month.JULY),
                                    tom = YearMonth.of(2024, Month.AUGUST),
                                ),
                            ytelseEtterAvkorting = 156,
                            ytelseEtterAvkortingFoerRestanse = 3156,
                            avkortingsbeloep = 13526,
                            ytelseFoerAvkorting = 16682,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse!!.shouldBeEqualToIgnoringFields(
                        restanse(
                            totalRestanse = 18000,
                            fordeltRestanse = 3000,
                        ),
                        Restanse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                    )
                }
                get(3).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode = Periode(fom = YearMonth.of(2024, Month.SEPTEMBER), tom = null),
                            ytelseEtterAvkorting = 0,
                            ytelseEtterAvkortingFoerRestanse = 906,
                            avkortingsbeloep = 15776,
                            ytelseFoerAvkorting = 16682,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse!!.shouldBeEqualToIgnoringFields(
                        restanse(
                            totalRestanse = 25300,
                            fordeltRestanse = 6325,
                        ),
                        Restanse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                    )
                }
            }
        }

        @Test
        fun `Revurdering hevet beregnng`() {
            val avkorting = `Avkorting revurdert beregning`()
            with(avkorting.aarsoppgjoer.avkortetYtelseAar) {
                size shouldBe 4
                get(0).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode =
                                Periode(
                                    fom = YearMonth.of(2024, Month.MARCH),
                                    tom = YearMonth.of(2024, Month.APRIL),
                                ),
                            ytelseEtterAvkorting = 13215,
                            ytelseEtterAvkortingFoerRestanse = 13215,
                            avkortingsbeloep = 9026,
                            ytelseFoerAvkorting = 22241,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse shouldBe null
                }
                get(1).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode =
                                Periode(
                                    fom = YearMonth.of(2024, Month.MAY),
                                    tom = YearMonth.of(2024, Month.JUNE),
                                ),
                            ytelseEtterAvkorting = 13215,
                            ytelseEtterAvkortingFoerRestanse = 13215,
                            avkortingsbeloep = 9026,
                            ytelseFoerAvkorting = 22241,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse shouldBe null
                }
                get(2).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode =
                                Periode(
                                    fom = YearMonth.of(2024, Month.JULY),
                                    tom = YearMonth.of(2024, Month.AUGUST),
                                ),
                            ytelseEtterAvkorting = 5715,
                            ytelseEtterAvkortingFoerRestanse = 8715,
                            avkortingsbeloep = 13526,
                            ytelseFoerAvkorting = 22241,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse!!.shouldBeEqualToIgnoringFields(
                        restanse(
                            totalRestanse = 18000,
                            fordeltRestanse = 3000,
                        ),
                        Restanse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                    )
                }
                get(3).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode = Periode(fom = YearMonth.of(2024, Month.SEPTEMBER), tom = null),
                            ytelseEtterAvkorting = 90,
                            ytelseEtterAvkortingFoerRestanse = 6465,
                            avkortingsbeloep = 15776,
                            ytelseFoerAvkorting = 22241,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse!!.shouldBeEqualToIgnoringFields(
                        restanse(
                            totalRestanse = 25500,
                            fordeltRestanse = 6375,
                        ),
                        Restanse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                    )
                }
            }
        }

        @Test
        fun `Revurdering inntektsendring - korrigere siste inntekt`() {
            val avkorting = `Avkorting korrigere siste inntekt`()
            with(avkorting.aarsoppgjoer.avkortetYtelseAar) {
                size shouldBe 4
                get(0).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode =
                                Periode(
                                    fom = YearMonth.of(2024, Month.MARCH),
                                    tom = YearMonth.of(2024, Month.APRIL),
                                ),
                            ytelseEtterAvkorting = 13215,
                            ytelseEtterAvkortingFoerRestanse = 13215,
                            avkortingsbeloep = 9026,
                            ytelseFoerAvkorting = 22241,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse shouldBe null
                }
                get(1).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode =
                                Periode(
                                    fom = YearMonth.of(2024, Month.MAY),
                                    tom = YearMonth.of(2024, Month.JUNE),
                                ),
                            ytelseEtterAvkorting = 13215,
                            ytelseEtterAvkortingFoerRestanse = 13215,
                            avkortingsbeloep = 9026,
                            ytelseFoerAvkorting = 22241,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse shouldBe null
                }
                get(2).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode =
                                Periode(
                                    fom = YearMonth.of(2024, Month.JULY),
                                    tom = YearMonth.of(2024, Month.AUGUST),
                                ),
                            ytelseEtterAvkorting = 5715,
                            ytelseEtterAvkortingFoerRestanse = 8715,
                            avkortingsbeloep = 13526,
                            ytelseFoerAvkorting = 22241,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse!!.shouldBeEqualToIgnoringFields(
                        restanse(
                            totalRestanse = 18000,
                            fordeltRestanse = 3000,
                        ),
                        Restanse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                    )
                }
                get(3).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode = Periode(fom = YearMonth.of(2024, Month.SEPTEMBER), tom = null),
                            ytelseEtterAvkorting = 2903,
                            ytelseEtterAvkortingFoerRestanse = 7590,
                            avkortingsbeloep = 14651,
                            ytelseFoerAvkorting = 22241,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse!!.shouldBeEqualToIgnoringFields(
                        restanse(
                            totalRestanse = 18750,
                            fordeltRestanse = 4687,
                        ),
                        Restanse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                    )
                }
            }
        }

        @Test
        fun `Skal kunne reberegne avkorting uten endring for en revurdering med virk i mellom inntektsperioder`() {
            val avkorting = `Revurdering med virk mellom inntektsperioder`()
            with(avkorting.aarsoppgjoer.avkortetYtelseAar) {
                size shouldBe 5
                get(0).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode =
                                Periode(
                                    fom = YearMonth.of(2024, Month.MARCH),
                                    tom = YearMonth.of(2024, Month.MARCH),
                                ),
                            ytelseEtterAvkorting = 13215,
                            ytelseEtterAvkortingFoerRestanse = 13215,
                            avkortingsbeloep = 9026,
                            ytelseFoerAvkorting = 22241,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse shouldBe null
                }
                get(1).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode =
                                Periode(
                                    fom = YearMonth.of(2024, Month.APRIL),
                                    tom = YearMonth.of(2024, Month.APRIL),
                                ),
                            ytelseEtterAvkorting = 13215,
                            ytelseEtterAvkortingFoerRestanse = 13215,
                            avkortingsbeloep = 9026,
                            ytelseFoerAvkorting = 22241,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse shouldBe null
                }
                get(2).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode =
                                Periode(
                                    fom = YearMonth.of(2024, Month.MAY),
                                    tom = YearMonth.of(2024, Month.JUNE),
                                ),
                            ytelseEtterAvkorting = 13215,
                            ytelseEtterAvkortingFoerRestanse = 13215,
                            avkortingsbeloep = 9026,
                            ytelseFoerAvkorting = 22241,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse shouldBe null
                }
                get(3).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode =
                                Periode(
                                    fom = YearMonth.of(2024, Month.JULY),
                                    tom = YearMonth.of(2024, Month.AUGUST),
                                ),
                            ytelseEtterAvkorting = 5715,
                            ytelseEtterAvkortingFoerRestanse = 8715,
                            avkortingsbeloep = 13526,
                            ytelseFoerAvkorting = 22241,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse!!.shouldBeEqualToIgnoringFields(
                        restanse(
                            totalRestanse = 18000,
                            fordeltRestanse = 3000,
                        ),
                        Restanse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                    )
                }
                get(4).asClue {
                    it.shouldBeEqualToIgnoringFields(
                        avkortetYtelse(
                            type = AvkortetYtelseType.AARSOPPGJOER,
                            periode = Periode(fom = YearMonth.of(2024, Month.SEPTEMBER), tom = null),
                            ytelseEtterAvkorting = 2903,
                            ytelseEtterAvkortingFoerRestanse = 7590,
                            avkortingsbeloep = 14651,
                            ytelseFoerAvkorting = 22241,
                            inntektsgrunnlag = null,
                        ),
                        AvkortetYtelse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                        AvkortetYtelse::restanse,
                    )
                    it.restanse!!.shouldBeEqualToIgnoringFields(
                        restanse(
                            totalRestanse = 18750,
                            fordeltRestanse = 4687,
                        ),
                        Restanse::id,
                        AvkortetYtelse::tidspunkt,
                        AvkortetYtelse::regelResultat,
                        AvkortetYtelse::kilde,
                    )
                }
            }
        }

        private fun `Avkorting foerstegangsbehandling`() =
            Avkorting()
                .beregnAvkortingMedNyttGrunnlag(
                    nyttGrunnlag =
                        avkortinggrunnlagLagre(
                            aarsinntekt = 300000,
                            fratrekkInnAar = 50000,
                        ),
                    behandlingstype = BehandlingType.FØRSTEGANGSBEHANDLING,
                    virkningstidspunkt = YearMonth.of(2024, Month.MARCH),
                    bruker = bruker,
                    beregning =
                        beregning(
                            beregninger =
                                listOf(
                                    beregningsperiode(
                                        datoFOM = YearMonth.of(2024, Month.MARCH),
                                        datoTOM = YearMonth.of(2024, Month.APRIL),
                                        utbetaltBeloep = 15676,
                                    ),
                                    beregningsperiode(
                                        datoFOM = YearMonth.of(2024, Month.MAY),
                                        utbetaltBeloep = 16682,
                                    ),
                                ),
                        ),
                )

        private fun `Avkorting ny inntekt en`() =
            `Avkorting foerstegangsbehandling`()
                .kopierAvkorting()
                .beregnAvkortingMedNyttGrunnlag(
                    nyttGrunnlag =
                        avkortinggrunnlagLagre(
                            id = UUID.randomUUID(),
                            aarsinntekt = 400000,
                            fratrekkInnAar = 50000,
                        ),
                    behandlingstype = BehandlingType.REVURDERING,
                    virkningstidspunkt = YearMonth.of(2024, Month.JULY),
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
                )

        private fun `Avkorting ny inntekt to`() =
            `Avkorting ny inntekt en`()
                .kopierAvkorting()
                .beregnAvkortingMedNyttGrunnlag(
                    nyttGrunnlag =
                        avkortinggrunnlagLagre(
                            id = UUID.randomUUID(),
                            aarsinntekt = 450000,
                            fratrekkInnAar = 50000,
                        ),
                    behandlingstype = BehandlingType.REVURDERING,
                    virkningstidspunkt = YearMonth.of(2024, Month.SEPTEMBER),
                    bruker = bruker,
                    beregning =
                        beregning(
                            beregninger =
                                listOf(
                                    beregningsperiode(
                                        datoFOM = YearMonth.of(2024, Month.SEPTEMBER),
                                        utbetaltBeloep = 16682,
                                    ),
                                ),
                        ),
                )

        private fun `Avkorting revurdert beregning`() =
            `Avkorting ny inntekt to`()
                .kopierAvkorting()
                .beregnAvkortingRevurdering(
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2024, Month.MARCH),
                                    datoTOM = YearMonth.of(2024, Month.APRIL),
                                    utbetaltBeloep = 22241,
                                ),
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2024, Month.MAY),
                                    utbetaltBeloep = 22241,
                                ),
                            ),
                    ),
                )

        private fun `Avkorting korrigere siste inntekt`() =
            `Avkorting revurdert beregning`()
                .kopierAvkorting().let {
                    it.beregnAvkortingMedNyttGrunnlag(
                        nyttGrunnlag =
                            avkortinggrunnlagLagre(
                                id = it.aarsoppgjoer.inntektsavkorting.last().grunnlag.id,
                                aarsinntekt = 425000,
                                fratrekkInnAar = 50000,
                            ),
                        BehandlingType.REVURDERING,
                        virkningstidspunkt = YearMonth.of(2024, Month.SEPTEMBER),
                        bruker = bruker,
                        beregning(
                            beregninger =
                                listOf(
                                    beregningsperiode(
                                        datoFOM = YearMonth.of(2024, Month.SEPTEMBER),
                                        utbetaltBeloep = 22241,
                                    ),
                                ),
                        ),
                    )
                }

        private fun `Revurdering med virk mellom inntektsperioder`() =
            `Avkorting korrigere siste inntekt`()
                .kopierAvkorting()
                .beregnAvkortingRevurdering(
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2024, Month.APRIL),
                                    datoTOM = YearMonth.of(2024, Month.APRIL),
                                    utbetaltBeloep = 22241,
                                ),
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2024, Month.MAY),
                                    utbetaltBeloep = 22241,
                                ),
                            ),
                    ),
                )
    }
}
