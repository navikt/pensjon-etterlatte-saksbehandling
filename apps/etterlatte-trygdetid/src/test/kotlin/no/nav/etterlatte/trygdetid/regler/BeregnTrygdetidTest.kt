package no.nav.etterlatte.trygdetid.regler

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.FaktiskTrygdetid
import no.nav.etterlatte.libs.common.trygdetid.FremtidigTrygdetid
import no.nav.etterlatte.libs.common.trygdetid.land.LandNormalisert
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.trygdetid.TrygdetidGrunnlag
import no.nav.etterlatte.trygdetid.TrygdetidPeriode
import no.nav.etterlatte.trygdetid.TrygdetidType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.Period
import java.util.UUID
import java.util.stream.Stream

internal class BeregnTrygdetidTest {
    @Test
    fun `beregnTrygdetidMellomToDatoer skal gi en maaned naar periodeFra er dag 1 og periodeTil er siste dag i mnd`() {
        val grunnlag =
            TrygdetidPeriodeGrunnlag(
                periode =
                    FaktumNode(
                        verdi =
                            TrygdetidPeriodeMedPoengaar(
                                fra = LocalDate.of(2023, 1, 1),
                                til = LocalDate.of(2023, 1, 31),
                                poengInnAar = false,
                                poengUtAar = false,
                            ),
                        kilde = "Z1234",
                        beskrivelse = "Periode",
                    ),
            )

        val resultat = beregnTrygdetidForPeriode.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe Period.ofMonths(1)
    }

    @Test
    fun `beregnTrygdetidMellomToDatoer skal gi en dag naar periodeFra og periodeTil er like`() {
        val grunnlag =
            TrygdetidPeriodeGrunnlag(
                periode =
                    FaktumNode(
                        verdi =
                            TrygdetidPeriodeMedPoengaar(
                                fra = LocalDate.of(2023, 1, 1),
                                til = LocalDate.of(2023, 1, 1),
                                poengInnAar = false,
                                poengUtAar = false,
                            ),
                        kilde = "Z1234",
                        beskrivelse = "Periode",
                    ),
            )

        val resultat = beregnTrygdetidForPeriode.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe Period.ofDays(1)
    }

    @Test
    fun `beregnTrygdetidMellomToDatoer skal gi ett aar en maaned og en dag`() {
        val grunnlag =
            TrygdetidPeriodeGrunnlag(
                periode =
                    FaktumNode(
                        verdi =
                            TrygdetidPeriodeMedPoengaar(
                                fra = LocalDate.of(2023, 1, 1),
                                til = LocalDate.of(2024, 2, 1),
                                poengInnAar = false,
                                poengUtAar = false,
                            ),
                        kilde = "Z1234",
                        beskrivelse = "Periode",
                    ),
            )

        val resultat = beregnTrygdetidForPeriode.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe Period.of(1, 1, 1)
    }

    @ParameterizedTest(name = "fra {0} til {1} poengInnAar {2} poengUtAar {3} skal gi periode {4}")
    @MethodSource("verdierForPoengaarTest")
    fun `beregnTrygdetidMellomToDatoer skal ta hensyn til poengInnAar og poengUtAar`(
        fra: LocalDate,
        til: LocalDate,
        poengInnAar: Boolean,
        poengUtAar: Boolean,
        forventetPeriode: Period,
    ) {
        val grunnlag =
            TrygdetidPeriodeGrunnlag(
                periode =
                    FaktumNode(
                        verdi =
                            TrygdetidPeriodeMedPoengaar(
                                fra = fra,
                                til = til,
                                poengInnAar = poengInnAar,
                                poengUtAar = poengUtAar,
                            ),
                        kilde = "Z1234",
                        beskrivelse = "Periode",
                    ),
            )

        val resultat = beregnTrygdetidForPeriode.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe forventetPeriode
    }

    private fun totalTrygdetidGrunnlag(perioder: List<Period>) =
        TotalTrygdetidGrunnlag(
            FaktumNode(perioder, Grunnlagsopplysning.Saksbehandler("Z12345", Tidspunkt.now()), "Trygdetidsperioder"),
        )

    @ParameterizedTest(name = "{0}")
    @MethodSource("trygdetidGrunnlag")
    fun `beregnDetaljertBeregnetTrygdetid skal beregne riktig`(
        beskrivelse: String,
        perioder: List<TrygdetidGrunnlag>,
        forventet: DetaljertBeregnetTrygdetidResultat,
        datoer: Pair<LocalDate, LocalDate>?,
        norskPoengaar: Int?,
        yrkesskade: Boolean,
    ) {
        val grunnlag =
            FaktumNode(
                verdi =
                    TrygdetidGrunnlagMedAvdoed(
                        trygdetidGrunnlagListe = perioder,
                        foedselsDato = datoer?.first ?: LocalDate.of(1981, 2, 21),
                        doedsDato = datoer?.second ?: LocalDate.of(2023, 3, 15),
                        norskPoengaar = norskPoengaar,
                        yrkesskade = yrkesskade,
                        nordiskKonvensjon = false,
                    ),
                kilde = Grunnlagsopplysning.Saksbehandler("Z12345", Tidspunkt.now()),
                beskrivelse = "Perioder",
            )

        val trygdetidGrunnlagMedAvdoedGrunnlag = TrygdetidGrunnlagMedAvdoedGrunnlag(grunnlag)

        val resultat =
            beregnDetaljertBeregnetTrygdetidMedYrkesskade.anvend(
                trygdetidGrunnlagMedAvdoedGrunnlag,
                RegelPeriode(LocalDate.now()),
            )

        resultat.verdi shouldBe forventet
    }

    companion object {
        @JvmStatic
        fun verdierForPoengaarTest(): Stream<Arguments> =
            Stream.of(
                Arguments.of(LocalDate.of(2023, 5, 10), LocalDate.of(2023, 5, 30), false, false, Period.ofDays(21)),
                Arguments.of(LocalDate.of(2023, 5, 10), LocalDate.of(2023, 5, 30), false, true, Period.of(0, 7, 22)),
                Arguments.of(LocalDate.of(2023, 5, 10), LocalDate.of(2023, 5, 30), true, false, Period.of(0, 4, 30)),
                Arguments.of(LocalDate.of(2023, 5, 10), LocalDate.of(2023, 5, 30), true, true, Period.ofYears(1)),
                Arguments.of(LocalDate.of(2020, 2, 10), LocalDate.of(2023, 5, 30), false, false, Period.of(3, 3, 21)),
                Arguments.of(LocalDate.of(2020, 2, 10), LocalDate.of(2023, 5, 30), false, true, Period.of(3, 10, 22)),
                Arguments.of(LocalDate.of(2020, 2, 10), LocalDate.of(2023, 5, 30), true, false, Period.of(3, 4, 30)),
                Arguments.of(LocalDate.of(2020, 2, 10), LocalDate.of(2023, 5, 30), true, true, Period.ofYears(4)),
                // Justering poeng inn år gjør ingen forskjell hvis vi starter perioden 1.januar
                Arguments.of(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 3, 4), true, false, Period.of(0, 2, 4)),
                Arguments.of(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 3, 4), false, false, Period.of(0, 2, 4)),
                // Justering poeng ut år gjør ingen forskjell hvis vi slutter perioden 31. desember
                Arguments.of(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 12, 31), false, true, Period.of(0, 7, 0)),
                Arguments.of(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 12, 31), false, false, Period.of(0, 7, 0)),
                // Helt år har ingen ting å si hva du krysser av inn/ut
                Arguments.of(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), false, false, Period.ofYears(1)),
                Arguments.of(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), true, false, Period.ofYears(1)),
                Arguments.of(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), false, true, Period.ofYears(1)),
                Arguments.of(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), true, true, Period.ofYears(1)),
            )

        @JvmStatic
        fun trygdetidGrunnlag(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    // ...arguments =
                    "Nasjonal ikke poengAar",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2023, 4, 2),
                                til = LocalDate.of(2023, 6, 9),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(0, 3, 0),
                                antallMaaneder = 3,
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(0, 3, 0),
                                antallMaaneder = 3,
                            ),
                        fremtidigTrygdetidNorge = null,
                        fremtidigTrygdetidTeoretisk = null,
                        samletTrygdetidNorge = 0,
                        samletTrygdetidTeoretisk = 0,
                        prorataBroek = null,
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                    null,
                    null,
                    false,
                ),
                Arguments.of(
                    "Nasjonal poengInnAar",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2023, 4, 2),
                                til = LocalDate.of(2023, 6, 9),
                            ),
                            poengInnAar = true,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(0, 6, 0),
                                antallMaaneder = 6,
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(0, 6, 0),
                                antallMaaneder = 6,
                            ),
                        fremtidigTrygdetidNorge = null,
                        fremtidigTrygdetidTeoretisk = null,
                        samletTrygdetidNorge = 0,
                        samletTrygdetidTeoretisk = 0,
                        prorataBroek = null,
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                    null,
                    null,
                    false,
                ),
                Arguments.of(
                    "Nasjonal poengUtAar",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2023, 4, 2),
                                til = LocalDate.of(2023, 6, 9),
                            ),
                            poengInnAar = false,
                            poengUtAar = true,
                            medIProrata = true,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(0, 9, 0),
                                antallMaaneder = 9,
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(0, 9, 0),
                                antallMaaneder = 9,
                            ),
                        fremtidigTrygdetidNorge = null,
                        fremtidigTrygdetidTeoretisk = null,
                        samletTrygdetidNorge = 1,
                        samletTrygdetidTeoretisk = 1,
                        prorataBroek = null,
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                    null,
                    null,
                    false,
                ),
                Arguments.of(
                    "Nasjonal poengInnAar og poengUtAar",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2023, 4, 2),
                                til = LocalDate.of(2023, 6, 9),
                            ),
                            poengInnAar = true,
                            poengUtAar = true,
                            medIProrata = true,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(1, 0, 0),
                                antallMaaneder = 12,
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(1, 0, 0),
                                antallMaaneder = 12,
                            ),
                        fremtidigTrygdetidNorge = null,
                        fremtidigTrygdetidTeoretisk = null,
                        samletTrygdetidNorge = 1,
                        samletTrygdetidTeoretisk = 1,
                        prorataBroek = null,
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                    null,
                    null,
                    false,
                ),
                Arguments.of(
                    "Utland",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(1992, 10, 3),
                                til = LocalDate.of(2004, 12, 12),
                            ),
                            poengInnAar = false,
                            poengUtAar = true,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.SVERIGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2005, 1, 1),
                                til = LocalDate.of(2016, 12, 16),
                            ),
                            poengInnAar = false,
                            poengUtAar = true,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2017, 1, 1),
                                til = LocalDate.of(2022, 12, 31),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FREMTIDIG,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2023, 1, 29),
                                til = LocalDate.of(2042, 12, 31),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(18, 3, 0),
                                antallMaaneder = 219,
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(30, 3, 0),
                                antallMaaneder = 363,
                            ),
                        fremtidigTrygdetidNorge =
                            FremtidigTrygdetid(
                                periode = Period.of(15, 10, 0),
                                antallMaaneder = 190,
                                opptjeningstidIMaaneder = 362,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true,
                            ),
                        fremtidigTrygdetidTeoretisk =
                            FremtidigTrygdetid(
                                periode = Period.of(20, 0, 0),
                                antallMaaneder = 240,
                                opptjeningstidIMaaneder = 362,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = false,
                            ),
                        samletTrygdetidNorge = 34,
                        samletTrygdetidTeoretisk = 40,
                        prorataBroek = IntBroek(219, 363),
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                    Pair(
                        LocalDate.of(1976, 10, 3),
                        LocalDate.of(2023, 1, 29),
                    ),
                    null,
                    false,
                ),
                Arguments.of(
                    "Utland 2",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(1997, 2, 21),
                                til = LocalDate.of(2010, 12, 17),
                            ),
                            poengInnAar = false,
                            poengUtAar = true,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.SVERIGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2011, 1, 1),
                                til = LocalDate.of(2023, 2, 28),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FREMTIDIG,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2023, 3, 15),
                                til = LocalDate.of(2047, 12, 31),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(13, 11, 0),
                                antallMaaneder = 167,
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(26, 1, 0),
                                antallMaaneder = 313,
                            ),
                        fremtidigTrygdetidNorge =
                            FremtidigTrygdetid(
                                periode = Period.of(19, 2, 0),
                                antallMaaneder = 230,
                                opptjeningstidIMaaneder = 312,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true,
                            ),
                        fremtidigTrygdetidTeoretisk =
                            FremtidigTrygdetid(
                                periode = Period.of(24, 10, 0),
                                antallMaaneder = 298,
                                opptjeningstidIMaaneder = 312,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = false,
                            ),
                        samletTrygdetidNorge = 33,
                        samletTrygdetidTeoretisk = 40,
                        prorataBroek = IntBroek(167, 313),
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                    Pair(
                        LocalDate.of(1981, 2, 21),
                        LocalDate.of(2023, 3, 15),
                    ),
                    null,
                    false,
                ),
                Arguments.of(
                    "Utland 3",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.SVERIGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2011, 1, 1),
                                til = LocalDate.of(2023, 2, 28),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FREMTIDIG,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2023, 3, 15),
                                til = LocalDate.of(2047, 12, 31),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge = FaktiskTrygdetid(Period.ZERO, 0),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(12, 2, 0),
                                antallMaaneder = 146,
                            ),
                        fremtidigTrygdetidNorge =
                            FremtidigTrygdetid(
                                periode = Period.of(19, 2, 0),
                                antallMaaneder = 230,
                                opptjeningstidIMaaneder = 312,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true,
                            ),
                        fremtidigTrygdetidTeoretisk =
                            FremtidigTrygdetid(
                                periode = Period.of(19, 2, 0),
                                antallMaaneder = 230,
                                opptjeningstidIMaaneder = 312,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true,
                            ),
                        samletTrygdetidNorge = 19,
                        samletTrygdetidTeoretisk = 31,
                        prorataBroek = IntBroek(teller = 0, nevner = 146),
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                    Pair(
                        LocalDate.of(1981, 2, 21),
                        LocalDate.of(2023, 3, 15),
                    ),
                    null,
                    false,
                ),
                Arguments.of(
                    "Utland med overstyrt norsk poengaar",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2010, 1, 1),
                                til = LocalDate.of(2010, 12, 31),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.SVERIGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2011, 1, 1),
                                til = LocalDate.of(2023, 2, 28),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FREMTIDIG,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2023, 3, 15),
                                til = LocalDate.of(2047, 12, 31),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.ofYears(2),
                                antallMaaneder = 24,
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(14, 2, 0),
                                antallMaaneder = 170,
                            ),
                        fremtidigTrygdetidNorge = null,
                        fremtidigTrygdetidTeoretisk = null,
                        samletTrygdetidNorge = 2,
                        samletTrygdetidTeoretisk = 14,
                        prorataBroek = IntBroek(24, 170),
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                    Pair(
                        LocalDate.of(1981, 2, 21),
                        LocalDate.of(2023, 3, 15),
                    ),
                    2,
                    false,
                ),
                Arguments.of(
                    // ...arguments =
                    "Skal ikke treffe 4/5 regel hvis 18/20 år er opptjening",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2023, 2, 21).minusYears(18),
                                til = LocalDate.of(2023, 2, 21),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            type = TrygdetidType.FREMTIDIG,
                            bosted = LandNormalisert.NORGE.isoCode,
                            periode =
                                TrygdetidPeriode(
                                    fra = LocalDate.of(2023, 2, 21),
                                    til = LocalDate.of(2023, 2, 21).plusYears(30),
                                ),
                            poengInnAar = false,
                            poengUtAar = true,
                            medIProrata = true,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(18, 1, 0),
                                antallMaaneder = 217,
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(18, 1, 0),
                                antallMaaneder = 217,
                            ),
                        fremtidigTrygdetidNorge =
                            FremtidigTrygdetid(
                                periode = Period.of(30, 11, 0),
                                antallMaaneder = 371,
                                opptjeningstidIMaaneder = 239,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = false,
                            ),
                        fremtidigTrygdetidTeoretisk =
                            FremtidigTrygdetid(
                                periode = Period.of(30, 11, 0),
                                antallMaaneder = 371,
                                opptjeningstidIMaaneder = 239,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = false,
                            ),
                        samletTrygdetidNorge = 40,
                        samletTrygdetidTeoretisk = 40,
                        prorataBroek = null,
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                    Pair(
                        LocalDate.of(2023, 2, 21).minusYears(36),
                        LocalDate.of(2023, 2, 21),
                    ),
                    null,
                    false,
                ),
                Arguments.of(
                    // ...arguments =
                    "Skal ikke treffe 4/5 regel hvis nordisk konvensjon",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.SVERIGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2013, 5, 1),
                                til = LocalDate.of(2018, 12, 31),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2019, 1, 1),
                                til = LocalDate.of(2024, 9, 30),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            type = TrygdetidType.FREMTIDIG,
                            bosted = LandNormalisert.NORGE.isoCode,
                            periode =
                                TrygdetidPeriode(
                                    fra = LocalDate.of(2024, 10, 15),
                                    til = LocalDate.of(2041, 10, 20),
                                ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(5, 9, 0),
                                antallMaaneder = 69,
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(11, 5, 0),
                                antallMaaneder = 137,
                            ),
                        fremtidigTrygdetidNorge =
                            FremtidigTrygdetid(
                                periode = Period.of(22, 9, 0),
                                antallMaaneder = 273,
                                opptjeningstidIMaaneder = 259,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true,
                            ),
                        fremtidigTrygdetidTeoretisk =
                            FremtidigTrygdetid(
                                periode = Period.of(22, 9, 0),
                                antallMaaneder = 273,
                                opptjeningstidIMaaneder = 259,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true,
                            ),
                        samletTrygdetidNorge = 29,
                        samletTrygdetidTeoretisk = 34,
                        prorataBroek = IntBroek(teller = 69, nevner = 137),
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                    Pair(
                        LocalDate.of(2023, 2, 21).minusYears(36),
                        LocalDate.of(2023, 2, 21),
                    ),
                    null,
                    false,
                ),
                Arguments.of(
                    // ...arguments =
                    "Skal ikke få mer enn 40 år i brøken",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(1974, 12, 11),
                                til = LocalDate.of(1994, 6, 1),
                            ),
                            poengInnAar = false,
                            poengUtAar = true,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.DANMARK.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(1996, 1, 1),
                                til = LocalDate.of(2015, 12, 31),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            type = TrygdetidType.FREMTIDIG,
                            bosted = LandNormalisert.NORGE.isoCode,
                            periode =
                                TrygdetidPeriode(
                                    fra = LocalDate.of(2015, 1, 7),
                                    til =

                                        LocalDate.of(2024, 12, 31),
                                ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(20, 1, 0),
                                antallMaaneder = 241,
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(40, 1, 0),
                                antallMaaneder = 481,
                            ),
                        fremtidigTrygdetidNorge =
                            FremtidigTrygdetid(
                                periode = Period.of(7, 11, 0),
                                antallMaaneder = 95,
                                opptjeningstidIMaaneder = 481,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true,
                            ),
                        fremtidigTrygdetidTeoretisk =
                            FremtidigTrygdetid(
                                periode = Period.of(10, 0, 0),
                                antallMaaneder = 120,
                                opptjeningstidIMaaneder = 481,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = false,
                            ),
                        samletTrygdetidNorge = 28,
                        samletTrygdetidTeoretisk = 40,
                        prorataBroek = IntBroek(241, 480),
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                    Pair(
                        LocalDate.of(1958, 11, 12),
                        LocalDate.of(2015, 1, 7),
                    ),
                    null,
                    false,
                ),
                Arguments.of(
                    // ...arguments =
                    "Nasjonal ikke poengAar med yrkesskade",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2022, 4, 2),
                                til = LocalDate.of(2023, 6, 9),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(1, 3, 0),
                                antallMaaneder = 15,
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(1, 3, 0),
                                antallMaaneder = 15,
                            ),
                        fremtidigTrygdetidNorge = null,
                        fremtidigTrygdetidTeoretisk = null,
                        samletTrygdetidNorge = 40,
                        samletTrygdetidTeoretisk = 1,
                        prorataBroek = null,
                        overstyrt = false,
                        yrkesskade = true,
                        beregnetSamletTrygdetidNorge = 1,
                    ),
                    null,
                    null,
                    true,
                ),
                Arguments.of(
                    // ...arguments =
                    "Ikke mer enn en oppjustering",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.POLEN.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(1994, 2, 10),
                                til = LocalDate.of(2004, 12, 31),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.STORBRITANNIA.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2005, 3, 2),
                                til = LocalDate.of(2008, 12, 31),
                            ),
                            poengInnAar = true,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.DANMARK.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2009, 1, 1),
                                til = LocalDate.of(2011, 12, 12),
                            ),
                            poengInnAar = false,
                            poengUtAar = true,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.POLEN.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2012, 11, 1),
                                til = LocalDate.of(2012, 12, 31),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.DANMARK.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2014, 5, 23),
                                til = LocalDate.of(2019, 3, 25),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.POLEN.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2019, 10, 1),
                                til = LocalDate.of(2019, 12, 31),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2013, 6, 1),
                                til = LocalDate.of(2014, 5, 4),
                            ),
                            poengInnAar = true,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.DANMARK.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2020, 6, 1),
                                til = LocalDate.of(2021, 6, 3),
                            ),
                            poengInnAar = true,
                            poengUtAar = true,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.POLEN.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2022, 1, 1),
                                til = LocalDate.of(2022, 7, 31),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FREMTIDIG,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2022, 8, 23),
                                til = LocalDate.of(2044, 12, 31),
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = false,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(1, 5, 0),
                                antallMaaneder = 17,
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(27, 1, 0),
                                antallMaaneder = 325,
                            ),
                        fremtidigTrygdetidNorge =
                            FremtidigTrygdetid(
                                periode = Period.of(19, 8, 0),
                                antallMaaneder = 236,
                                opptjeningstidIMaaneder = 305,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true,
                            ),
                        fremtidigTrygdetidTeoretisk =
                            FremtidigTrygdetid(
                                periode = Period.of(22, 5, 0),
                                antallMaaneder = 269,
                                opptjeningstidIMaaneder = 305,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = false,
                            ),
                        samletTrygdetidNorge = 21,
                        samletTrygdetidTeoretisk = 40,
                        prorataBroek = IntBroek(17, 325),
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                    null,
                    null,
                    false,
                ),
                Arguments.of(
                    // ...arguments =
                    "Tester detaljer i avrunding mellom pesys og Gjenny",
                    listOf(
                        byggTrygdetidGrunnlag(
                            type = TrygdetidType.FREMTIDIG,
                            bosted = LandNormalisert.NORGE.isoCode,
                            periode =
                                TrygdetidPeriode(
                                    fra = LocalDate.of(2024, 2, 28),
                                    til = LocalDate.of(2045, 12, 31),
                                ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = false,
                        ),
                        byggTrygdetidGrunnlag(
                            type = TrygdetidType.FAKTISK,
                            bosted = LandNormalisert.DANMARK.isoCode,
                            periode =
                                TrygdetidPeriode(
                                    fra = LocalDate.of(1995, 6, 4),
                                    til = LocalDate.of(2015, 12, 30),
                                ),
                            poengInnAar = false,
                            poengUtAar = true,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            type = TrygdetidType.FAKTISK,
                            bosted = LandNormalisert.NORGE.isoCode,
                            periode = TrygdetidPeriode(fra = LocalDate.of(2017, 1, 1), til = LocalDate.of(2017, 3, 6)),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            type = TrygdetidType.FAKTISK,
                            bosted = LandNormalisert.NORGE.isoCode,
                            periode =
                                TrygdetidPeriode(
                                    fra = LocalDate.of(2018, 8, 17),
                                    til = LocalDate.of(2018, 11, 27),
                                ),
                            poengInnAar = true,
                            poengUtAar = true,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            type = TrygdetidType.FAKTISK,
                            bosted = LandNormalisert.DANMARK.isoCode,
                            periode =
                                TrygdetidPeriode(
                                    fra = LocalDate.of(2017, 5, 10),
                                    til = LocalDate.of(2017, 8, 31),
                                ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            type = TrygdetidType.FAKTISK,
                            bosted = LandNormalisert.DANMARK.isoCode,
                            periode =
                                TrygdetidPeriode(
                                    fra = LocalDate.of(2021, 10, 1),
                                    til = LocalDate.of(2021, 12, 31),
                                ),
                            poengInnAar = true,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            type = TrygdetidType.FAKTISK,
                            bosted = LandNormalisert.NORGE.isoCode,
                            periode = TrygdetidPeriode(fra = LocalDate.of(2022, 6, 3), til = LocalDate.of(2024, 1, 31)),
                            poengInnAar = true,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(3, 4, 0),
                                antallMaaneder = 40,
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(25, 2, 0),
                                antallMaaneder = 302,
                            ),
                        fremtidigTrygdetidNorge =
                            FremtidigTrygdetid(
                                periode = Period.of(17, 2, 0),
                                antallMaaneder = 206,
                                opptjeningstidIMaaneder = 343,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true,
                            ),
                        fremtidigTrygdetidTeoretisk =
                            FremtidigTrygdetid(
                                periode = Period.of(21, 11, 0),
                                antallMaaneder = 263,
                                opptjeningstidIMaaneder = 343,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = false,
                            ),
                        samletTrygdetidNorge = 21,
                        samletTrygdetidTeoretisk = 40,
                        prorataBroek = IntBroek(40, 302),
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                    LocalDate.of(1979, 6, 4) to LocalDate.of(2024, 2, 28),
                    null,
                    false,
                ),
                Arguments.of(
                    // ...arguments =
                    "Sjekker opprunding av faktisk trygdetid foer addering med framtidig trygdetid",
                    listOf(
                        byggTrygdetidGrunnlag(
                            type = TrygdetidType.FREMTIDIG,
                            bosted = LandNormalisert.NORGE.isoCode,
                            periode =
                                TrygdetidPeriode(
                                    fra = LocalDate.of(2022, 10, 19),
                                    til = LocalDate.of(2051, 12, 31),
                                ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = false,
                        ),
                        byggTrygdetidGrunnlag(
                            type = TrygdetidType.FAKTISK,
                            bosted = LandNormalisert.NORGE.isoCode,
                            periode =
                                TrygdetidPeriode(
                                    fra = LocalDate.of(2012, 7, 25),
                                    til = LocalDate.of(2022, 9, 30),
                                ),
                            poengInnAar = true,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(10, 9, 0),
                                antallMaaneder = 129,
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(10, 9, 0),
                                antallMaaneder = 129,
                            ),
                        fremtidigTrygdetidNorge =
                            FremtidigTrygdetid(
                                periode = Period.of(22, 9, 0),
                                antallMaaneder = 273,
                                opptjeningstidIMaaneder = 259,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true,
                            ),
                        fremtidigTrygdetidTeoretisk =
                            FremtidigTrygdetid(
                                periode = Period.of(22, 9, 0),
                                antallMaaneder = 273,
                                opptjeningstidIMaaneder = 259,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true,
                            ),
                        samletTrygdetidNorge = 34,
                        samletTrygdetidTeoretisk = 34,
                        prorataBroek = null,
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                    LocalDate.of(1985, 2, 16) to LocalDate.of(2022, 10, 19),
                    null,
                    false,
                ),
                Arguments.of(
                    // ...arguments =
                    "Sjekker opprunding av faktisk trygdetid foer addering med framtidig trygdetid",
                    listOf(
                        byggTrygdetidGrunnlag(
                            type = TrygdetidType.FAKTISK,
                            bosted = LandNormalisert.NORGE.isoCode,
                            periode =
                                TrygdetidPeriode(
                                    fra = LocalDate.of(2012, 7, 25),
                                    til = LocalDate.of(2022, 9, 30),
                                ),
                            poengInnAar = true,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                        byggTrygdetidGrunnlag(
                            type = TrygdetidType.FREMTIDIG,
                            bosted = LandNormalisert.NORGE.isoCode,
                            periode =
                                TrygdetidPeriode(
                                    fra = LocalDate.of(2022, 10, 19),
                                    til = LocalDate.of(2051, 12, 31),
                                ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = false,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(10, 9, 0),
                                antallMaaneder = 129,
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(10, 9, 0),
                                antallMaaneder = 129,
                            ),
                        fremtidigTrygdetidNorge =
                            FremtidigTrygdetid(
                                periode = Period.of(22, 9, 0),
                                antallMaaneder = 273,
                                opptjeningstidIMaaneder = 259,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true,
                            ),
                        fremtidigTrygdetidTeoretisk =
                            FremtidigTrygdetid(
                                periode = Period.of(22, 9, 0),
                                antallMaaneder = 273,
                                opptjeningstidIMaaneder = 259,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true,
                            ),
                        samletTrygdetidNorge = 34,
                        samletTrygdetidTeoretisk = 34,
                        prorataBroek = null,
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                    LocalDate.of(1985, 2, 16) to LocalDate.of(2022, 10, 19),
                    null,
                    false,
                ),
                Arguments.of(
                    "Runder ikke opp faktisk trygdetid hvis ingen framtidig trygdetid",
                    listOf(
                        byggTrygdetidGrunnlag(
                            type = TrygdetidType.FAKTISK,
                            bosted = LandNormalisert.NORGE.isoCode,
                            periode =
                                TrygdetidPeriode(
                                    fra = LocalDate.of(2012, 7, 25),
                                    til = LocalDate.of(2022, 6, 17),
                                ),
                            poengInnAar = true,
                            poengUtAar = false,
                            medIProrata = true,
                        ),
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(10, 6, 0),
                                antallMaaneder = 126,
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(10, 6, 0),
                                antallMaaneder = 126,
                            ),
                        fremtidigTrygdetidNorge = null,
                        fremtidigTrygdetidTeoretisk = null,
                        samletTrygdetidNorge = 10,
                        samletTrygdetidTeoretisk = 10,
                        prorataBroek = null,
                        overstyrt = false,
                        yrkesskade = false,
                        beregnetSamletTrygdetidNorge = null,
                    ),
                    null,
                    null,
                    false,
                ),
            )

        private fun byggTrygdetidGrunnlag(
            type: TrygdetidType,
            bosted: String,
            periode: TrygdetidPeriode,
            poengInnAar: Boolean,
            poengUtAar: Boolean,
            medIProrata: Boolean,
        ) = TrygdetidGrunnlag(
            id = UUID.randomUUID(),
            type = type,
            bosted = bosted,
            periode = periode,
            kilde = Grunnlagsopplysning.Saksbehandler("Z12345", Tidspunkt.now()),
            null,
            null,
            poengInnAar,
            poengUtAar,
            medIProrata,
        )
    }
}
