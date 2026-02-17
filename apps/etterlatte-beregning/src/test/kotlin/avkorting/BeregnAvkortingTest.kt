package no.nav.etterlatte.beregning.regler.avkorting

import io.kotest.assertions.asClue
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import no.nav.etterlatte.avkorting.AvkortetYtelse
import no.nav.etterlatte.avkorting.AvkortetYtelseType
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.Restanse
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkortinggrunnlagLagreDto
import no.nav.etterlatte.beregning.regler.beregning
import no.nav.etterlatte.beregning.regler.beregningsperiode
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.beregning.regler.inntektsavkorting
import no.nav.etterlatte.beregning.regler.restanse
import no.nav.etterlatte.beregning.regler.sanksjon
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.libs.common.beregning.SanksjonType
import no.nav.etterlatte.libs.common.beregning.SanksjonertYtelse
import no.nav.etterlatte.libs.common.periode.Periode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals

class BeregnAvkortingTest {
    @BeforeEach
    fun `mock grunnbeloep`() {
        mockkObject(GrunnbeloepRepository)
        every { GrunnbeloepRepository.historiskeGrunnbeloep } returns
            listOf(
                Grunnbeloep(
                    dato = YearMonth.of(2023, 5),
                    grunnbeloep = 118620,
                    grunnbeloepPerMaaned = 9885,
                    omregningsfaktor = BigDecimal("1.045591"),
                ),
                Grunnbeloep(
                    dato = YearMonth.of(2024, 5),
                    grunnbeloep = 124028,
                    grunnbeloepPerMaaned = 10336,
                    omregningsfaktor = BigDecimal("1.064076"),
                ),
            )
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `Beregner avkortet ytelse for foerstegangsbehandling`() {
        val avkorting = `Avkorting foerstegangsbehandling`()
        with(avkorting.aarsoppgjoer.single().avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer.single().avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer.single().avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer.single().avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer.single().avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer.single().avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer.single().avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer.single().avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer.single().avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer.single().avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer.single().avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer.single().avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer.single().avkortetYtelse) {
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
    fun `kan beregne med to innsendte inntekter`() {
        val avkorting = `Avkorting foerstegangsbehandling med to inntekter`()
        avkorting.aarsoppgjoer.size shouldBe 2
        with(avkorting.aarsoppgjoer[0].avkortetYtelse) {
            size shouldBe 2
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
                                tom = YearMonth.of(2024, Month.DECEMBER),
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
            }
        }
        with(avkorting.aarsoppgjoer[1].avkortetYtelse) {
            size shouldBe 2
            get(0).asClue {
                it.shouldBeEqualToIgnoringFields(
                    avkortetYtelse(
                        type = AvkortetYtelseType.AARSOPPGJOER,
                        periode =
                            Periode(
                                fom = YearMonth.of(2025, Month.JANUARY),
                                tom = YearMonth.of(2025, Month.APRIL),
                            ),
                        ytelseEtterAvkorting = 5883,
                        ytelseEtterAvkortingFoerRestanse = 5883,
                        avkortingsbeloep = 10799,
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
        }
    }

    @Test
    fun `Revurdering inntektsendring nytt år`() {
        val avkorting = `Revurdering ny inntekt for nytt år`()
        with(avkorting.aarsoppgjoer[0].avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer[1].avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer[0].avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer[1].avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer[0].avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer[1].avkortetYtelse) {
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
        with(avkorting.aarsoppgjoer[2].avkortetYtelse) {
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

    @Test
    fun `Tester å legge inn et opphør fom midt inne i første avkortingsår`() {
        val avkorting = `Revurdering der opphør flyttes tidligere`()
        assertEquals(1, avkorting.aarsoppgjoer.size)
        val aarsoppgjoer = avkorting.aarsoppgjoer.single()
        with(aarsoppgjoer.avkortetYtelse) {
            size shouldBe 2

            get(0).asClue {
                it.periode.fom shouldBe YearMonth.of(2024, Month.MARCH)
                it.periode.tom shouldBe YearMonth.of(2024, Month.APRIL)
                it.ytelseEtterAvkorting shouldBe 17474
                it.avkortingsbeloep shouldBe 4526
                it.ytelseFoerAvkorting shouldBe 22000
                it.restanse shouldBe null
            }
            get(1).asClue {
                it.periode.fom shouldBe YearMonth.of(2024, Month.MAY)
                it.periode.tom shouldBe YearMonth.of(2024, Month.SEPTEMBER)
                it.ytelseEtterAvkorting shouldBe 18576
                it.avkortingsbeloep shouldBe 4424
                it.ytelseFoerAvkorting shouldBe 23000
                it.restanse shouldBe null
            }
        }
    }

    @Test
    fun `Avkorting etteroppgjør beregnes riktig`() {
        val avkorting =
            `avkorting etteroppgjør 2024 legges inn`(
                loennsinntekt = 350_000,
            )
        val avkorting2024 = avkorting.aarsoppgjoer.single { it.aar == 2024 }
        with(avkorting2024.avkortetYtelse) {
            size shouldBe 3
            get(0).asClue {
                it.periode.fom shouldBe YearMonth.of(2024, Month.MARCH)
                it.periode.tom shouldBe YearMonth.of(2024, Month.MARCH)
                it.ytelseEtterAvkorting shouldBe 8715
                it.restanse shouldBe null
                it.avkortingsbeloep shouldBe 13526
                it.ytelseFoerAvkorting shouldBe 22241
            }
            get(1).asClue {
                it.periode.fom shouldBe YearMonth.of(2024, Month.APRIL)
                it.periode.tom shouldBe YearMonth.of(2024, Month.APRIL)
                it.ytelseEtterAvkorting shouldBe 8715
                it.restanse shouldBe null
                it.avkortingsbeloep shouldBe 13526
                it.ytelseFoerAvkorting shouldBe 22241
            }
            get(2).asClue {
                it.periode.fom shouldBe YearMonth.of(2024, Month.MAY)
                it.periode.tom shouldBe YearMonth.of(2024, Month.DECEMBER)
                it.ytelseEtterAvkorting shouldBe 8817
                it.restanse shouldBe null
                it.avkortingsbeloep shouldBe 13424
                it.ytelseFoerAvkorting shouldBe 22241
            }
        }
    }

    // TODO Revurdering opphør midt i et år

    private fun `Avkorting foerstegangsbehandling`() =
        Avkorting()
            .beregnAvkortingMedNyeGrunnlag(
                nyttGrunnlag =
                    listOf(
                        avkortinggrunnlagLagreDto(
                            aarsinntekt = 300000,
                            fratrekkInnAar = 50000,
                            fom = YearMonth.of(2024, Month.MARCH),
                        ),
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
                brukNyeReglerAvkorting = false,
            )

    private fun `Avkorting foerstegangsbehandling med to inntekter`() =
        Avkorting()
            .beregnAvkortingMedNyeGrunnlag(
                nyttGrunnlag =
                    listOf(
                        avkortinggrunnlagLagreDto(
                            aarsinntekt = 300000,
                            fratrekkInnAar = 50000,
                            fom = YearMonth.of(2024, Month.MARCH),
                        ),
                        avkortinggrunnlagLagreDto(
                            aarsinntekt = 350000,
                            fratrekkInnAar = 0,
                            fom = YearMonth.of(2025, Month.JANUARY),
                        ),
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
                                    datoTOM = YearMonth.of(2025, Month.APRIL),
                                    utbetaltBeloep = 16682,
                                ),
                                beregningsperiode(datoFOM = YearMonth.of(2025, Month.MAY), utbetaltBeloep = 18000),
                            ),
                    ),
                sanksjoner = emptyList(),
                opphoerFom = null,
                brukNyeReglerAvkorting = false,
            )

    private fun `Avkorting foerstegangsbehandling med sanksjon`() =
        Avkorting()
            .beregnAvkortingMedNyeGrunnlag(
                nyttGrunnlag =
                    listOf(
                        avkortinggrunnlagLagreDto(
                            aarsinntekt = 300000,
                            fratrekkInnAar = 50000,
                            fom = YearMonth.of(2024, Month.MARCH),
                        ),
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
                brukNyeReglerAvkorting = false,
            )

    private fun `Avkorting revurdering med en sanksjon åpen periode`() =
        `Avkorting foerstegangsbehandling`()
            .kopierAvkorting()
            .beregnAvkorting(
                virkningstidspunkt = YearMonth.of(2024, Month.MAY),
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
                opphoerFom = null,
                brukNyeReglerAvkorting = false,
            )

    private fun `Avkorting revurdering av sanksjon åpen periode lukker sanksjonsperioden`() =
        `Avkorting revurdering med en sanksjon åpen periode`()
            .kopierAvkorting()
            .beregnAvkorting(
                virkningstidspunkt = YearMonth.of(2024, Month.JUNE),
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(datoFOM = YearMonth.of(2024, Month.JUNE), utbetaltBeloep = 16682),
                            ),
                    ),
                sanksjoner = listOf(sanksjon(fom = YearMonth.of(2024, Month.MAY), tom = YearMonth.of(2024, Month.MAY))),
                opphoerFom = null,
                brukNyeReglerAvkorting = false,
            )

    private fun `Avkorting ny inntekt en`() =
        `Avkorting foerstegangsbehandling`()
            .kopierAvkorting()
            .beregnAvkortingMedNyeGrunnlag(
                nyttGrunnlag =
                    listOf(
                        avkortinggrunnlagLagreDto(
                            id = UUID.randomUUID(),
                            aarsinntekt = 400000,
                            fratrekkInnAar = 50000,
                            fom = YearMonth.of(2024, Month.JULY),
                        ),
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
                brukNyeReglerAvkorting = false,
            )

    private fun `Sanksjon tilbake i tid mellom inntektsgrunnlag`() =
        `Avkorting ny inntekt en`()
            .kopierAvkorting()
            .beregnAvkorting(
                YearMonth.of(2024, Month.MAY),
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
                opphoerFom = null,
                brukNyeReglerAvkorting = false,
            )

    private fun `Sanksjon etter inntektsendring legges inn`() =
        `Avkorting ny inntekt en`()
            .kopierAvkorting()
            .beregnAvkorting(
                YearMonth.of(2024, Month.AUGUST),
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
                opphoerFom = null,
                brukNyeReglerAvkorting = false,
            )

    private fun `Sanksjon etter inntektsendring lukkes`() =
        `Sanksjon etter inntektsendring legges inn`()
            .kopierAvkorting()
            .beregnAvkorting(
                YearMonth.of(2024, Month.SEPTEMBER),
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
                sanksjoner =
                    listOf(
                        sanksjon(fom = YearMonth.of(2024, Month.AUGUST), tom = YearMonth.of(2024, Month.AUGUST)),
                    ),
                opphoerFom = null,
                brukNyeReglerAvkorting = false,
            )

    private fun `avkorting etteroppgjør 2024 legges inn`(
        loennsinntekt: Int = 0,
        afp: Int = 0,
        naeringsinntekt: Int = 0,
        utland: Int = 0,
    ) = `Revurdering med virk mellom inntektsperioder`()
        .kopierAvkorting()
        .let { avkorting ->
            val periode2024 = avkorting.aarsoppgjoer.single { it.aar == 2024 }.periode()

            avkorting.beregnEtteroppgjoer(
                brukerTokenInfo = bruker,
                aar = 2024,
                loennsinntekt = loennsinntekt,
                afp = afp,
                naeringsinntekt = naeringsinntekt,
                utland = utland,
                sanksjoner = emptyList(),
                spesifikasjon = "",
                innvilgetPeriodeIEtteroppgjoersAar = periode2024,
                opphoerFom = null,
                brukNyeReglerAvkorting = false,
            )
        }

    private fun `Avkorting ny lavere inntekt to etter sanksjon`() =
        `Sanksjon etter inntektsendring lukkes`()
            .kopierAvkorting()
            .beregnAvkortingMedNyeGrunnlag(
                nyttGrunnlag =
                    listOf(
                        avkortinggrunnlagLagreDto(
                            id = UUID.randomUUID(),
                            aarsinntekt = 300000,
                            fratrekkInnAar = 50000,
                            fom = YearMonth.of(2024, Month.SEPTEMBER),
                        ),
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
                brukNyeReglerAvkorting = false,
            )

    private fun `Avkorting ny inntekt to`() =
        `Avkorting ny inntekt en`()
            .kopierAvkorting()
            .beregnAvkortingMedNyeGrunnlag(
                nyttGrunnlag =
                    listOf(
                        avkortinggrunnlagLagreDto(
                            id = UUID.randomUUID(),
                            aarsinntekt = 450000,
                            fratrekkInnAar = 50000,
                            fom = YearMonth.of(2024, Month.SEPTEMBER),
                        ),
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
                brukNyeReglerAvkorting = false,
            )

    private fun `Avkorting revurdert beregning`() =
        `Avkorting ny inntekt to`()
            .kopierAvkorting()
            .beregnAvkorting(
                YearMonth.of(2024, Month.MARCH),
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
                opphoerFom = null,
                brukNyeReglerAvkorting = false,
            )

    private fun `Avkorting korrigere siste inntekt`() =
        `Avkorting revurdert beregning`()
            .kopierAvkorting()
            .let {
                it.beregnAvkortingMedNyeGrunnlag(
                    listOf(
                        avkortinggrunnlagLagreDto(
                            id =
                                it.aarsoppgjoer
                                    .single()
                                    .inntektsavkorting()
                                    .last()
                                    .grunnlag.id,
                            aarsinntekt = 425000,
                            fratrekkInnAar = 50000,
                            fom = YearMonth.of(2024, Month.SEPTEMBER),
                        ),
                    ),
                    bruker = bruker,
                    beregning =
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
                    brukNyeReglerAvkorting = false,
                )
            }

    private fun `Revurdering med virk mellom inntektsperioder`() =
        `Avkorting korrigere siste inntekt`()
            .kopierAvkorting()
            .beregnAvkorting(
                YearMonth.of(2024, Month.APRIL),
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
                opphoerFom = null,
                brukNyeReglerAvkorting = false,
            )

    private fun `Revurdering ny inntekt for nytt år`() =
        `Revurdering med virk mellom inntektsperioder`()
            .kopierAvkorting()
            .beregnAvkortingMedNyeGrunnlag(
                nyttGrunnlag =
                    listOf(
                        avkortinggrunnlagLagreDto(
                            id = UUID.randomUUID(),
                            aarsinntekt = 500000,
                            fratrekkInnAar = 0,
                            fom = YearMonth.of(2025, Month.JANUARY),
                        ),
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
                brukNyeReglerAvkorting = false,
            )

    private fun `Revurdering med virk tilbake i tidligere år`() =
        `Revurdering ny inntekt for nytt år`()
            .kopierAvkorting()
            .beregnAvkorting(
                YearMonth.of(2024, Month.NOVEMBER),
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
                opphoerFom = null,
                brukNyeReglerAvkorting = false,
            )

    private fun `Revurdering ny inntekt nytt år med opphør`() =
        `Revurdering med virk tilbake i tidligere år`()
            .kopierAvkorting()
            .beregnAvkortingMedNyeGrunnlag(
                nyttGrunnlag =
                    listOf(
                        avkortinggrunnlagLagreDto(
                            id = UUID.randomUUID(),
                            aarsinntekt = 262500,
                            fratrekkInnAar = 0,
                            // TODO Legge til fratrekk ut år når det kommer
                            fom = YearMonth.of(2026, Month.JANUARY),
                        ),
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
                brukNyeReglerAvkorting = false,
            )

    private fun `Revurdering der opphør flyttes tidligere`() =
        `Førstegangsbehandling fra mars 2024 med opphør i mai 2025`()
            .kopierAvkorting(opphoerFom = YearMonth.of(2024, Month.OCTOBER))
            .beregnAvkorting(
                virkningstidspunkt = YearMonth.of(2024, Month.MARCH),
                beregning =
                    beregning(
                        beregninger =
                            listOf(
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2024, Month.MARCH),
                                    datoTOM = YearMonth.of(2024, Month.APRIL),
                                    utbetaltBeloep = 22_000,
                                ),
                                beregningsperiode(
                                    datoFOM = YearMonth.of(2024, Month.MAY),
                                    datoTOM = YearMonth.of(2024, Month.SEPTEMBER),
                                    utbetaltBeloep = 23_000,
                                ),
                            ),
                    ),
                sanksjoner = listOf(),
                opphoerFom = YearMonth.of(2024, Month.OCTOBER),
                brukNyeReglerAvkorting = false,
            )

    private fun `Førstegangsbehandling fra mars 2024 med opphør i mai 2025`() =
        Avkorting().beregnAvkortingMedNyeGrunnlag(
            nyttGrunnlag =
                listOf(
                    avkortinggrunnlagLagreDto(
                        id = UUID.randomUUID(),
                        aarsinntekt = 200_000,
                        fratrekkInnAar = 50_000,
                        fom = YearMonth.of(2024, Month.MARCH),
                    ),
                    avkortinggrunnlagLagreDto(
                        id = UUID.randomUUID(),
                        aarsinntekt = 250_000,
                        fratrekkInnAar = 0,
                        fom = YearMonth.of(2025, Month.JANUARY),
                    ),
                ),
            bruker = bruker,
            beregning =
                beregning(
                    beregninger =
                        listOf(
                            beregningsperiode(
                                datoFOM = YearMonth.of(2024, Month.MARCH),
                                datoTOM = YearMonth.of(2024, Month.APRIL),
                                utbetaltBeloep = 22_000,
                            ),
                            beregningsperiode(
                                datoFOM = YearMonth.of(2024, Month.MAY),
                                datoTOM = YearMonth.of(2025, Month.APRIL),
                                utbetaltBeloep = 23_000,
                            ),
                        ),
                ),
            sanksjoner = listOf(),
            opphoerFom = YearMonth.of(2025, Month.MAY),
            aldersovergang = null,
            brukNyeReglerAvkorting = false,
        )
}
