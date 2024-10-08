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
import no.nav.etterlatte.beregning.regler.sanksjon
import no.nav.etterlatte.libs.common.beregning.SanksjonType
import no.nav.etterlatte.libs.common.beregning.SanksjonertYtelse
import no.nav.etterlatte.libs.common.periode.Periode
import org.junit.jupiter.api.Assertions
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
    fun `Førstegangsbehandling med sanksjon beregner riktig ytelse`() {
        val avkorting = `Avkorting foerstegangsbehandling med sanksjon`()
        with(avkorting.aarsoppgjoer.single().avkortetYtelseAar) {
            size shouldBe 4
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
                    periode = Periode(fom = YearMonth.of(2024, Month.MAY), tom = YearMonth.of(2024, Month.JULY)),
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
            get(2).shouldBeEqualToIgnoringFields(
                avkortetYtelse(
                    periode = Periode(fom = YearMonth.of(2024, Month.AUGUST), tom = YearMonth.of(2024, Month.AUGUST)),
                    ytelseEtterAvkorting = 0,
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
                AvkortetYtelse::sanksjon,
            )
            get(2).sanksjon?.sanksjonType shouldBe SanksjonType.BORTFALL
            get(3).shouldBeEqualToIgnoringFields(
                avkortetYtelse(
                    periode = Periode(fom = YearMonth.of(2024, Month.SEPTEMBER), tom = null),
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
    fun `Revurdering legger inn sanksjon beregner 0 i sanksjonsperioden`() {
        val avkorting = `Avkorting revurdering med en sanksjon åpen periode`()
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
                    sanksjon = null,
                ),
                AvkortetYtelse::id,
                AvkortetYtelse::tidspunkt,
                AvkortetYtelse::regelResultat,
                AvkortetYtelse::kilde,
            )
            get(1).shouldBeEqualToIgnoringFields(
                avkortetYtelse(
                    periode = Periode(fom = YearMonth.of(2024, Month.MAY), tom = null),
                    ytelseEtterAvkorting = 0,
                    ytelseEtterAvkortingFoerRestanse = 7758,
                    restanse = null,
                    avkortingsbeloep = 8924,
                    ytelseFoerAvkorting = 16682,
                    type = AvkortetYtelseType.AARSOPPGJOER,
                    inntektsgrunnlag = null,
                    sanksjon =
                        SanksjonertYtelse(
                            sanksjonId = get(1).sanksjon?.sanksjonId!!,
                            sanksjonType = SanksjonType.STANS,
                        ),
                ),
                AvkortetYtelse::id,
                AvkortetYtelse::tidspunkt,
                AvkortetYtelse::regelResultat,
                AvkortetYtelse::kilde,
            )
        }
    }

    @Test
    fun `Revurdering åpen sanksjonsperiode lukkes`() {
        val avkorting = `Avkorting revurdering av sanksjon åpen periode lukker sanksjonsperioden`()
        with(avkorting.aarsoppgjoer.single().avkortetYtelseAar) {
            size shouldBe 3
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
                    sanksjon = null,
                ),
                AvkortetYtelse::id,
                AvkortetYtelse::tidspunkt,
                AvkortetYtelse::regelResultat,
                AvkortetYtelse::kilde,
            )
            get(1).shouldBeEqualToIgnoringFields(
                avkortetYtelse(
                    periode = Periode(fom = YearMonth.of(2024, Month.MAY), tom = YearMonth.of(2024, Month.MAY)),
                    ytelseEtterAvkorting = 0,
                    ytelseEtterAvkortingFoerRestanse = 7758,
                    restanse = null,
                    avkortingsbeloep = 8924,
                    ytelseFoerAvkorting = 16682,
                    type = AvkortetYtelseType.AARSOPPGJOER,
                    inntektsgrunnlag = null,
                    sanksjon =
                        SanksjonertYtelse(
                            sanksjonId = get(1).sanksjon?.sanksjonId!!,
                            sanksjonType = SanksjonType.STANS,
                        ),
                ),
                AvkortetYtelse::id,
                AvkortetYtelse::tidspunkt,
                AvkortetYtelse::regelResultat,
                AvkortetYtelse::kilde,
            )
            get(2).shouldBeEqualToIgnoringFields(
                avkortetYtelse(
                    periode = Periode(fom = YearMonth.of(2024, Month.JUNE), tom = null),
                    ytelseEtterAvkorting = 7758,
                    ytelseEtterAvkortingFoerRestanse = 7758,
                    restanse = null,
                    avkortingsbeloep = 8924,
                    ytelseFoerAvkorting = 16682,
                    type = AvkortetYtelseType.AARSOPPGJOER,
                    inntektsgrunnlag = null,
                    sanksjon = null,
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
    fun `Revurdering legger inn sanksjon etter inntektsendring`() {
        val avkorting = `Sanksjon etter inntektsendring legges inn`()
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
                        periode = Periode(fom = YearMonth.of(2024, Month.JULY), tom = YearMonth.of(2024, Month.JULY)),
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
                        periode =
                            Periode(
                                fom = YearMonth.of(2024, Month.AUGUST),
                                tom = null,
                            ),
                        ytelseEtterAvkorting = 0,
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
                    AvkortetYtelse::sanksjon,
                )
                it.restanse!!.shouldBeEqualToIgnoringFields(
                    restanse(
                        totalRestanse = 18000,
                        fordeltRestanse = 18000,
                    ),
                    Restanse::id,
                    AvkortetYtelse::tidspunkt,
                    AvkortetYtelse::regelResultat,
                    AvkortetYtelse::kilde,
                )
                it.sanksjon!!.sanksjonType shouldBe SanksjonType.STANS
            }
        }
    }

    @Test
    fun `Revurdering legger til sanksjon tilbake i tid etter inntektsendring`() {
        val avkorting = `Sanksjon tilbake i tid mellom inntektsgrunnlag`()
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
                                tom = YearMonth.of(2024, Month.MAY),
                            ),
                        ytelseEtterAvkorting = 0,
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
                    AvkortetYtelse::sanksjon,
                )
                it.restanse!!.shouldBeEqualToIgnoringFields(
                    restanse(
                        18000,
                        3000,
                    ),
                    Restanse::id,
                    AvkortetYtelse::tidspunkt,
                    AvkortetYtelse::regelResultat,
                    AvkortetYtelse::kilde,
                )
                it.sanksjon?.sanksjonType shouldBe SanksjonType.BORTFALL
            }
            get(2).asClue {
                it.shouldBeEqualToIgnoringFields(
                    avkortetYtelse(
                        type = AvkortetYtelseType.AARSOPPGJOER,
                        periode = Periode(fom = YearMonth.of(2024, Month.JUNE), tom = null),
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
    fun `Revurdering lukker sanksjon etter inntektsendring`() {
        val avkorting = `Sanksjon etter inntektsendring lukkes`()
        with(avkorting.aarsoppgjoer.single().avkortetYtelseAar) {
            size shouldBe 5
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
                        periode = Periode(fom = YearMonth.of(2024, Month.JULY), tom = YearMonth.of(2024, Month.JULY)),
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
                        periode =
                            Periode(
                                fom = YearMonth.of(2024, Month.AUGUST),
                                tom = YearMonth.of(2024, Month.AUGUST),
                            ),
                        ytelseEtterAvkorting = 0,
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
                    AvkortetYtelse::sanksjon,
                )
                it.restanse!!.shouldBeEqualToIgnoringFields(
                    restanse(
                        totalRestanse = 18000,
                        fordeltRestanse = 3600,
                    ),
                    Restanse::id,
                    AvkortetYtelse::tidspunkt,
                    AvkortetYtelse::regelResultat,
                    AvkortetYtelse::kilde,
                )
                it.sanksjon!!.sanksjonType shouldBe SanksjonType.STANS
            }
            get(4).asClue {
                it.shouldBeEqualToIgnoringFields(
                    avkortetYtelse(
                        type = AvkortetYtelseType.AARSOPPGJOER,
                        periode =
                            Periode(
                                fom = YearMonth.of(2024, Month.SEPTEMBER),
                                tom = null,
                            ),
                        ytelseEtterAvkorting = 0,
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
                    AvkortetYtelse::sanksjon,
                )
                it.restanse!!.shouldBeEqualToIgnoringFields(
                    restanse(
                        totalRestanse = 18000,
                        fordeltRestanse = 3600,
                    ),
                    Restanse::id,
                    AvkortetYtelse::tidspunkt,
                    AvkortetYtelse::regelResultat,
                    AvkortetYtelse::kilde,
                )
                it.sanksjon shouldBe null
            }
        }
    }

    @Test
    fun `Revurdering inntektsendring - andre inntektsendring etter sanksjon`() {
        val avkorting = `Avkorting ny lavere inntekt to etter sanksjon`()
        with(avkorting.aarsoppgjoer.single().avkortetYtelseAar) {
            size shouldBe 5
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
                        periode = Periode(fom = YearMonth.of(2024, Month.JULY), tom = YearMonth.of(2024, Month.JULY)),
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
                        periode =
                            Periode(
                                fom = YearMonth.of(2024, Month.AUGUST),
                                tom = YearMonth.of(2024, Month.AUGUST),
                            ),
                        ytelseEtterAvkorting = 0,
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
                    AvkortetYtelse::sanksjon,
                )
                it.restanse!!.shouldBeEqualToIgnoringFields(
                    restanse(
                        totalRestanse = -15000,
                        fordeltRestanse = -3750,
                    ),
                    Restanse::id,
                    AvkortetYtelse::tidspunkt,
                    AvkortetYtelse::regelResultat,
                    AvkortetYtelse::kilde,
                )
                it.sanksjon!!.sanksjonType shouldBe SanksjonType.STANS
            }
            get(4).asClue {
                it.shouldBeEqualToIgnoringFields(
                    avkortetYtelse(
                        type = AvkortetYtelseType.AARSOPPGJOER,
                        periode =
                            Periode(
                                fom = YearMonth.of(2024, Month.SEPTEMBER),
                                tom = null,
                            ),
                        ytelseEtterAvkorting = 11508,
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
                    AvkortetYtelse::sanksjon,
                )
                it.restanse!!.shouldBeEqualToIgnoringFields(
                    restanse(
                        totalRestanse = -15000,
                        fordeltRestanse = -3750,
                    ),
                    Restanse::id,
                    AvkortetYtelse::tidspunkt,
                    AvkortetYtelse::regelResultat,
                    AvkortetYtelse::kilde,
                )
                it.sanksjon shouldBe null
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

    /**
     * Hvis du får en sanksjon på grunn av at du ikke jobber en måned (du tar permisjon), så vil den sammenhengende
     * inntektsendringen (minus en måned inntekt) gjøre at du kommer bedre ut av det (slik det ser ut nå).
     *
     * Vi må verifisere om det er slik i beregningen vår, og i så fall må vi korrigere. Problemet er nok at vi
     * avkorter mot en måned (der vi har sanksjon), men den måneden kan vi ikke avkorte noe mot.
     *
     * Konkret i eksempelet her har man en årsinntekt på 400 000, da med 33 333 kroner i måneden. Hvis bruker tar
     * permisjon en måned bryter de aktivitetskravet den måneden (hypotetisk), så vi setter inn en stans.
     *
     * Når vi starter ytelsen igjen tar vi med en oppdatert årsinntekt på 400 000 - 33 333 ~= 366 667, siden de
     * mister inntekten for den måneden de tok permisjon. Nå kompanserer vi for den avkortingen vi gjorde for mye
     * tidligere, og bruker ender opp med mer utbetalt fra oss enn hvis de ikke tok en måned permisjon (når testen
     * feiler).
     *
     * Problemet stammer nok fra at man får "credit" for å ha avkortet en del i måneden der man har sanksjon, men
     * det har man i praksis ikke. Dermed må den avkortingen fordeles på de andre månedene med ytelse.
     */
    @Test
    fun `stans en måned på grunn av manglende aktivitet + reduksjon av årsinntekt med en måned skal ikke lønne seg`() {
        val foersteberegning =
            `Avkorting førstegang 400000 årsinntekt`()
                .aarsoppgjoer
                .single()
                .avkortetYtelseAar
        val andreBeregning =
            `Revurdering lukker sanksjon en måned + redusert inntekt`()
                .aarsoppgjoer
                .single()
                .avkortetYtelseAar

        val mapUtbetaltFoerst = mutableMapOf<YearMonth, Int>()
        val mapUtbetaltSenere = mutableMapOf<YearMonth, Int>()

        for (i in 1..12) {
            val maaned = YearMonth.of(2024, i)
            val foerst =
                foersteberegning.find { it.periode.fom <= maaned && (it.periode.tom ?: maaned) >= maaned }?.ytelseEtterAvkorting ?: 0
            val senere =
                andreBeregning.find { it.periode.fom <= maaned && (it.periode.tom ?: maaned) >= maaned }?.ytelseEtterAvkorting ?: foerst
            mapUtbetaltFoerst[maaned] = foerst
            mapUtbetaltSenere[maaned] = senere
        }

        val total1 = mapUtbetaltFoerst.map { it.value }.sum()
        val total2 = mapUtbetaltSenere.map { it.value }.sum()

        Assertions.assertTrue(total1 >= total2)
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
        val avkorting = `Revurdering ny inntekt for nytt år`()
        with(avkorting.aarsoppgjoer[0].avkortetYtelseAar) {
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
                        periode =
                            Periode(
                                fom = YearMonth.of(2024, Month.SEPTEMBER),
                                tom = YearMonth.of(2024, Month.DECEMBER),
                            ),
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
        with(avkorting.aarsoppgjoer[1].avkortetYtelseAar) {
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

    @Test
    fun `Revurdering på tvers av år`() {
        val avkorting = `Revurdering med virk tilbake i tidligere år`()
        with(avkorting.aarsoppgjoer[0].avkortetYtelseAar) {
            size shouldBe 6
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
                        periode =
                            Periode(
                                fom = YearMonth.of(2024, Month.SEPTEMBER),
                                tom = YearMonth.of(2024, Month.OCTOBER),
                            ),
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
            get(5).asClue {
                it.shouldBeEqualToIgnoringFields(
                    avkortetYtelse(
                        type = AvkortetYtelseType.AARSOPPGJOER,
                        periode =
                            Periode(
                                fom = YearMonth.of(2024, Month.NOVEMBER),
                                tom = YearMonth.of(2024, Month.DECEMBER),
                            ),
                        ytelseEtterAvkorting = 1005,
                        ytelseEtterAvkortingFoerRestanse = 5692,
                        avkortingsbeloep = 14549,
                        ytelseFoerAvkorting = 20241,
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
        with(avkorting.aarsoppgjoer[1].avkortetYtelseAar) {
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
                        ytelseEtterAvkorting = 3817,
                        ytelseEtterAvkortingFoerRestanse = 3817,
                        avkortingsbeloep = 16424,
                        ytelseFoerAvkorting = 20241,
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

    @Test
    fun `Revurdering inntektsendring nytt år med opphør`() {
        val avkorting = `Revurdering ny inntekt nytt år med opphør`()
        with(avkorting.aarsoppgjoer[0].avkortetYtelseAar) {
            size shouldBe 6
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
                        periode =
                            Periode(
                                fom = YearMonth.of(2024, Month.SEPTEMBER),
                                tom = YearMonth.of(2024, Month.OCTOBER),
                            ),
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
            get(5).asClue {
                it.shouldBeEqualToIgnoringFields(
                    avkortetYtelse(
                        type = AvkortetYtelseType.AARSOPPGJOER,
                        periode =
                            Periode(
                                fom = YearMonth.of(2024, Month.NOVEMBER),
                                tom = YearMonth.of(2024, Month.DECEMBER),
                            ),
                        ytelseEtterAvkorting = 1005,
                        ytelseEtterAvkortingFoerRestanse = 5692,
                        avkortingsbeloep = 14549,
                        ytelseFoerAvkorting = 20241,
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
        with(avkorting.aarsoppgjoer[1].avkortetYtelseAar) {
            size shouldBe 1
            get(0).asClue {
                it.shouldBeEqualToIgnoringFields(
                    avkortetYtelse(
                        type = AvkortetYtelseType.AARSOPPGJOER,
                        periode =
                            Periode(
                                fom = YearMonth.of(2025, Month.JANUARY),
                                tom = YearMonth.of(2025, Month.DECEMBER),
                            ),
                        ytelseEtterAvkorting = 3817,
                        ytelseEtterAvkortingFoerRestanse = 3817,
                        avkortingsbeloep = 16424,
                        ytelseFoerAvkorting = 20241,
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
        with(avkorting.aarsoppgjoer[2].avkortetYtelseAar) {
            size shouldBe 1
            get(0).asClue {
                it.shouldBeEqualToIgnoringFields(
                    avkortetYtelse(
                        type = AvkortetYtelseType.AARSOPPGJOER,
                        periode =
                            Periode(
                                fom = YearMonth.of(2026, Month.JANUARY),
                                tom = YearMonth.of(2026, Month.JUNE),
                            ),
                        ytelseEtterAvkorting = 2917,
                        ytelseEtterAvkortingFoerRestanse = 2917,
                        avkortingsbeloep = 17324,
                        ytelseFoerAvkorting = 20241,
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

    // TODO Revurdering opphør midt i et år

    private fun `Avkorting foerstegangsbehandling`() =
        Avkorting()
            .beregnAvkortingMedNyttGrunnlag(
                nyttGrunnlag =
                    avkortinggrunnlagLagre(
                        aarsinntekt = 300000,
                        fratrekkInnAar = 50000,
                        fom = YearMonth.of(2024, Month.MARCH),
                    ),
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
                sanksjoner = emptyList(),
                opphoerFom = null,
            )

    private fun `Avkorting foerstegangsbehandling med sanksjon`() =
        Avkorting()
            .beregnAvkortingMedNyttGrunnlag(
                nyttGrunnlag =
                    avkortinggrunnlagLagre(
                        aarsinntekt = 300000,
                        fratrekkInnAar = 50000,
                        fom = YearMonth.of(2024, Month.MARCH),
                    ),
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
                sanksjoner =
                    listOf(
                        sanksjon(
                            fom = YearMonth.of(2024, Month.AUGUST),
                            tom = YearMonth.of(2024, Month.AUGUST),
                            type = SanksjonType.BORTFALL,
                        ),
                    ),
                opphoerFom = null,
            )

    private fun `Avkorting revurdering med en sanksjon åpen periode`() =
        `Avkorting foerstegangsbehandling`()
            .kopierAvkorting()
            .beregnAvkortingRevurdering(
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2024, Month.MAY),
                                    utbetaltBeloep = 16682,
                                ),
                            ),
                    ),
                sanksjoner = listOf(sanksjon(fom = YearMonth.of(2024, Month.MAY), tom = null)),
            )

    private fun `Avkorting revurdering av sanksjon åpen periode lukker sanksjonsperioden`() =
        `Avkorting revurdering med en sanksjon åpen periode`()
            .kopierAvkorting()
            .beregnAvkortingRevurdering(
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(datoFOM = YearMonth.of(2024, Month.JUNE), utbetaltBeloep = 16682),
                            ),
                    ),
                sanksjoner = listOf(sanksjon(fom = YearMonth.of(2024, Month.MAY), tom = YearMonth.of(2024, Month.MAY))),
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

    private fun `Sanksjon tilbake i tid mellom inntektsgrunnlag`() =
        `Avkorting ny inntekt en`()
            .kopierAvkorting()
            .beregnAvkortingRevurdering(
                beregning(
                    beregninger =
                        listOf(
                            beregningsperiode(
                                datoFOM = YearMonth.of(2024, Month.MAY),
                                datoTOM = null,
                                utbetaltBeloep = 16682,
                            ),
                        ),
                ),
                sanksjoner =
                    listOf(
                        sanksjon(
                            fom = YearMonth.of(2024, Month.MAY),
                            tom = YearMonth.of(2024, Month.MAY),
                            type = SanksjonType.BORTFALL,
                        ),
                    ),
            )

    private fun `Sanksjon etter inntektsendring legges inn`() =
        `Avkorting ny inntekt en`()
            .kopierAvkorting()
            .beregnAvkortingRevurdering(
                beregning(
                    beregninger =
                        listOf(
                            beregningsperiode(
                                datoFOM = YearMonth.of(2024, Month.AUGUST),
                                datoTOM = null,
                                utbetaltBeloep = 16682,
                            ),
                        ),
                ),
                sanksjoner =
                    listOf(
                        sanksjon(fom = YearMonth.of(2024, Month.AUGUST), tom = null),
                    ),
            )

    private fun `Sanksjon etter inntektsendring lukkes`() =
        `Sanksjon etter inntektsendring legges inn`()
            .kopierAvkorting()
            .beregnAvkortingRevurdering(
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(datoFOM = YearMonth.of(2024, Month.SEPTEMBER), utbetaltBeloep = 16682),
                            ),
                    ),
                sanksjoner =
                    listOf(
                        sanksjon(fom = YearMonth.of(2024, Month.AUGUST), tom = YearMonth.of(2024, Month.AUGUST)),
                    ),
            )

    private fun `Avkorting ny lavere inntekt to etter sanksjon`() =
        `Sanksjon etter inntektsendring lukkes`()
            .kopierAvkorting()
            .beregnAvkortingMedNyttGrunnlag(
                nyttGrunnlag =
                    avkortinggrunnlagLagre(
                        id = UUID.randomUUID(),
                        aarsinntekt = 300000,
                        fratrekkInnAar = 50000,
                        fom = YearMonth.of(2024, Month.SEPTEMBER),
                    ),
                bruker = bruker,
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2024, Month.SEPTEMBER),
                                    datoTOM = null,
                                    utbetaltBeloep = 16682,
                                ),
                            ),
                    ),
                sanksjoner =
                    listOf(
                        sanksjon(
                            fom = YearMonth.of(2024, Month.AUGUST),
                            tom = YearMonth.of(2024, Month.AUGUST),
                        ),
                    ),
                opphoerFom = null,
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
                        fom = YearMonth.of(2024, Month.SEPTEMBER),
                    ),
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
                sanksjoner = emptyList(),
                opphoerFom = null,
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
                sanksjoner = emptyList(),
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
                            fom = YearMonth.of(2024, Month.SEPTEMBER),
                        ),
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
                    sanksjoner = emptyList(),
                    opphoerFom = null,
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
                sanksjoner = emptyList(),
            )

    private fun `Revurdering ny inntekt for nytt år`() =
        `Revurdering med virk mellom inntektsperioder`()
            .kopierAvkorting()
            .beregnAvkortingMedNyttGrunnlag(
                nyttGrunnlag =
                    avkortinggrunnlagLagre(
                        id = UUID.randomUUID(),
                        aarsinntekt = 500000,
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
                                    utbetaltBeloep = 22241,
                                ),
                            ),
                    ),
                sanksjoner = emptyList(),
                opphoerFom = null,
            )

    private fun `Revurdering med virk tilbake i tidligere år`() =
        `Revurdering ny inntekt for nytt år`()
            .kopierAvkorting()
            .beregnAvkortingRevurdering(
                beregning(
                    beregninger =
                        listOf(
                            beregningsperiode(
                                datoFOM = YearMonth.of(2024, Month.NOVEMBER),
                                utbetaltBeloep = 20241,
                            ),
                        ),
                ),
                sanksjoner = emptyList(),
            )

    private fun `Revurdering ny inntekt nytt år med opphør`() =
        `Revurdering med virk tilbake i tidligere år`()
            .kopierAvkorting()
            .beregnAvkortingMedNyttGrunnlag(
                nyttGrunnlag =
                    avkortinggrunnlagLagre(
                        id = UUID.randomUUID(),
                        aarsinntekt = 262500,
                        fratrekkInnAar = 0,
                        // TODO Legge til fratrekk ut år når det kommer
                        fom = YearMonth.of(2026, Month.JANUARY),
                    ),
                bruker = bruker,
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2026, Month.JANUARY),
                                    utbetaltBeloep = 20241,
                                ),
                            ),
                    ),
                sanksjoner = emptyList(),
                opphoerFom = YearMonth.of(2026, Month.JULY),
            )

    private fun `Avkorting førstegang 400000 årsinntekt`() =
        Avkorting()
            .beregnAvkortingMedNyttGrunnlag(
                nyttGrunnlag =
                    avkortinggrunnlagLagre(
                        aarsinntekt = 400_000,
                        fratrekkInnAar = 0,
                        fom = YearMonth.of(2024, Month.JANUARY),
                    ),
                bruker = bruker,
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2024, Month.JANUARY),
                                    datoTOM = null,
                                    utbetaltBeloep = 23_255,
                                ),
                            ),
                    ),
                sanksjoner = emptyList(),
                opphoerFom = null,
            )

    private fun `Revurdering sanksjon stans start`() =
        `Avkorting førstegang 400000 årsinntekt`()
            .kopierAvkorting()
            .beregnAvkortingMedNyttGrunnlag(
                nyttGrunnlag =
                    avkortinggrunnlagLagre(
                        aarsinntekt = 400_000,
                        fratrekkInnAar = 0,
                        fom = YearMonth.of(2024, Month.MARCH),
                    ),
                bruker = bruker,
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2024, Month.MARCH),
                                    datoTOM = null,
                                    utbetaltBeloep = 23_255,
                                ),
                            ),
                    ),
                sanksjoner =
                    listOf(
                        sanksjon(
                            fom = YearMonth.of(2024, Month.MARCH),
                            tom = null,
                            type = SanksjonType.STANS,
                            beskrivelse = "",
                        ),
                    ),
                opphoerFom = null,
            )

    private fun `Revurdering lukker sanksjon en måned + redusert inntekt`() =
        `Revurdering sanksjon stans start`()
            .kopierAvkorting()
            .beregnAvkortingMedNyttGrunnlag(
                nyttGrunnlag =
                    avkortinggrunnlagLagre(
                        aarsinntekt = 366_667,
                        fratrekkInnAar = 0,
                        fom = YearMonth.of(2024, Month.APRIL),
                    ),
                bruker = bruker,
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2024, Month.APRIL),
                                    datoTOM = null,
                                    utbetaltBeloep = 23_255,
                                ),
                            ),
                    ),
                sanksjoner =
                    listOf(
                        sanksjon(
                            fom = YearMonth.of(2024, Month.MARCH),
                            tom = YearMonth.of(2024, Month.MARCH),
                            type = SanksjonType.STANS,
                            beskrivelse = "",
                        ),
                    ),
                opphoerFom = null,
            )
}
