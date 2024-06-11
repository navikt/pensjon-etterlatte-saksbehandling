package no.nav.etterlatte.beregning.regler.avkorting

import io.kotest.assertions.asClue
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.avkorting.AvkortetYtelse
import no.nav.etterlatte.avkorting.AvkortetYtelseType
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.Restanse
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkortinggrunnlagLagre
import no.nav.etterlatte.beregning.regler.beregning
import no.nav.etterlatte.beregning.regler.beregningsperiode
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.beregning.regler.restanse
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.periode.Periode
import org.junit.jupiter.api.Test
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class BeregnAvkortingTest {
    @Test
    fun `Beregner avkortet ytelse for foerstegangsbehandling`() {
        val avkorting = `Avkorting foerstegangsbehandling`()
        with(avkorting.aarsoppgjoer.single().avkortetYtelseAar) {
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
    }

    @Test
    fun `Revurdering inntektsendring - foerste inntektsendring`() {
        val avkorting = `Avkorting ny inntekt en`()
        with(avkorting.aarsoppgjoer.single().avkortetYtelseAar) {
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
                        ytelseEtterAvkorting = 7758,
                        ytelseEtterAvkortingFoerRestanse = 7758,
                        avkortingsbeloep = 8924,
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
                        ytelseEtterAvkorting = 258,
                        ytelseEtterAvkortingFoerRestanse = 3258,
                        avkortingsbeloep = 13424,
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
        with(avkorting.aarsoppgjoer.single().avkortetYtelseAar) {
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
                        ytelseEtterAvkorting = 7758,
                        ytelseEtterAvkortingFoerRestanse = 7758,
                        avkortingsbeloep = 8924,
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
                        ytelseEtterAvkorting = 258,
                        ytelseEtterAvkortingFoerRestanse = 3258,
                        avkortingsbeloep = 13424,
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
                        ytelseEtterAvkortingFoerRestanse = 1008,
                        avkortingsbeloep = 15674,
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
    fun `Revurdering hevet beregning`() {
        val avkorting = `Avkorting revurdert beregning`()
        with(avkorting.aarsoppgjoer.single().avkortetYtelseAar) {
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
                        ytelseEtterAvkorting = 13317,
                        ytelseEtterAvkortingFoerRestanse = 13317,
                        avkortingsbeloep = 8924,
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
                        ytelseEtterAvkorting = 5817,
                        ytelseEtterAvkortingFoerRestanse = 8817,
                        avkortingsbeloep = 13424,
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
                        ytelseEtterAvkorting = 192,
                        ytelseEtterAvkortingFoerRestanse = 6567,
                        avkortingsbeloep = 15674,
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
        with(avkorting.aarsoppgjoer.single().avkortetYtelseAar) {
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
                        ytelseEtterAvkorting = 13317,
                        ytelseEtterAvkortingFoerRestanse = 13317,
                        avkortingsbeloep = 8924,
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
                        ytelseEtterAvkorting = 5817,
                        ytelseEtterAvkortingFoerRestanse = 8817,
                        avkortingsbeloep = 13424,
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
                        ytelseEtterAvkorting = 3005,
                        ytelseEtterAvkortingFoerRestanse = 7692,
                        avkortingsbeloep = 14549,
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
        with(avkorting.aarsoppgjoer.single().avkortetYtelseAar) {
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
                        ytelseEtterAvkorting = 13317,
                        ytelseEtterAvkortingFoerRestanse = 13317,
                        avkortingsbeloep = 8924,
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
                        ytelseEtterAvkorting = 5817,
                        ytelseEtterAvkortingFoerRestanse = 8817,
                        avkortingsbeloep = 13424,
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
                        ytelseEtterAvkorting = 3005,
                        ytelseEtterAvkortingFoerRestanse = 7692,
                        avkortingsbeloep = 14549,
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
    fun `Revurdering inntektsendring nytt år`() {
        val avkorting = `Ny inntekt for nytt år`()
        with(avkorting.hentAarsoppgjoer(YearMonth.of(2024, 1)).avkortetYtelseAar) {
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
                        ytelseEtterAvkorting = 13317,
                        ytelseEtterAvkortingFoerRestanse = 13317,
                        avkortingsbeloep = 8924,
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
                        ytelseEtterAvkorting = 5817,
                        ytelseEtterAvkortingFoerRestanse = 8817,
                        avkortingsbeloep = 13424,
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
                        ytelseEtterAvkorting = 3005,
                        ytelseEtterAvkortingFoerRestanse = 7692,
                        avkortingsbeloep = 14549,
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
        with(avkorting.hentAarsoppgjoer(YearMonth.of(2025, 1)).avkortetYtelseAar) {
            size shouldBe 1
            get(0).asClue {
                it.shouldBeEqualToIgnoringFields(
                    avkortetYtelse(
                        type = AvkortetYtelseType.AARSOPPGJOER,
                        periode =
                            Periode(
                                fom = YearMonth.of(2025, Month.JANUARY),
                                tom = null,
                            ),
                        ytelseEtterAvkorting = 5817,
                        ytelseEtterAvkortingFoerRestanse = 5817,
                        // avkortingsbeloep = 9026,
                        avkortingsbeloep = 16424,
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
        }
    }

    // TODO revurdering tilbake i året før uten endring?

    // TODO enda revurdering fra og med jan nytt år?

    private fun `Avkorting foerstegangsbehandling`() =
        Avkorting().beregnAvkortingMedNyttGrunnlag(
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
            .kopierAvkorting()
            .let {
                it.beregnAvkortingMedNyttGrunnlag(
                    nyttGrunnlag =
                        avkortinggrunnlagLagre(
                            id =
                                it.aarsoppgjoer
                                    .single()
                                    .inntektsavkorting
                                    .last()
                                    .grunnlag.id,
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

    private fun `Ny inntekt for nytt år`() =
        `Revurdering med virk mellom inntektsperioder`()
            .kopierAvkorting()
            .beregnAvkortingMedNyttGrunnlag(
                nyttGrunnlag =
                    avkortinggrunnlagLagre(
                        id = UUID.randomUUID(),
                        aarsinntekt = 500000,
                        fratrekkInnAar = 0,
                    ),
                behandlingstype = BehandlingType.REVURDERING,
                virkningstidspunkt = YearMonth.of(2025, Month.JANUARY),
                bruker = bruker,
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2025, Month.JANUARY),
                                    utbetaltBeloep = 22241,
                                ),
                            ),
                    ),
            )
}
