package trygdetid.regler

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.FaktiskTrygdetid
import no.nav.etterlatte.libs.common.trygdetid.FremtidigTrygdetid
import no.nav.etterlatte.libs.common.trygdetid.IntBroek
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.trygdetid.LandNormalisert
import no.nav.etterlatte.trygdetid.TrygdetidGrunnlag
import no.nav.etterlatte.trygdetid.TrygdetidPeriode
import no.nav.etterlatte.trygdetid.TrygdetidType
import no.nav.etterlatte.trygdetid.regler.TotalTrygdetidGrunnlag
import no.nav.etterlatte.trygdetid.regler.TrygdetidGrunnlagMedAvdoed
import no.nav.etterlatte.trygdetid.regler.TrygdetidGrunnlagMedAvdoedGrunnlag
import no.nav.etterlatte.trygdetid.regler.TrygdetidPeriodMedPoengAar
import no.nav.etterlatte.trygdetid.regler.TrygdetidPeriodeGrunnlag
import no.nav.etterlatte.trygdetid.regler.beregnAntallAarTotalTrygdetid
import no.nav.etterlatte.trygdetid.regler.beregnDetaljertBeregnetTrygdetid
import no.nav.etterlatte.trygdetid.regler.beregnTrygdetidForPeriode
import no.nav.etterlatte.trygdetid.regler.dagerPrMaanedTrygdetid
import no.nav.etterlatte.trygdetid.regler.maksTrygdetid
import no.nav.etterlatte.trygdetid.regler.totalTrygdetidAvrundet
import no.nav.etterlatte.trygdetid.regler.totalTrygdetidFraPerioder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.Period
import java.util.*
import java.util.stream.Stream

internal class BeregnTrygdetidTest {
    @Test
    fun `beregnTrygdetidMellomToDatoer skal gi en maaned naar periodeFra er dag 1 og periodeTil er siste dag i mnd`() {
        val grunnlag =
            TrygdetidPeriodeGrunnlag(
                periode =
                    FaktumNode(
                        verdi =
                            TrygdetidPeriodMedPoengAar(
                                fra = LocalDate.of(2023, 1, 1),
                                til = LocalDate.of(2023, 1, 31),
                                poengInnAar = false,
                                poengUtAar = false
                            ),
                        kilde = "Z1234",
                        beskrivelse = "Periode"
                    )
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
                            TrygdetidPeriodMedPoengAar(
                                fra = LocalDate.of(2023, 1, 1),
                                til = LocalDate.of(2023, 1, 1),
                                poengInnAar = false,
                                poengUtAar = false
                            ),
                        kilde = "Z1234",
                        beskrivelse = "Periode"
                    )
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
                            TrygdetidPeriodMedPoengAar(
                                fra = LocalDate.of(2023, 1, 1),
                                til = LocalDate.of(2024, 2, 1),
                                poengInnAar = false,
                                poengUtAar = false
                            ),
                        kilde = "Z1234",
                        beskrivelse = "Periode"
                    )
            )

        val resultat = beregnTrygdetidForPeriode.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe Period.of(1, 1, 1)
    }

    @ParameterizedTest(name = "fra {0} til {1} poengInnAar {2} poengUtAar {3} skal gi periode {4}")
    @MethodSource("verdierForPoengAarTest")
    fun `beregnTrygdetidMellomToDatoer skal ta hensyn til poengInnAar og poengUtAar`(
        fra: LocalDate,
        til: LocalDate,
        poengInnAar: Boolean,
        poengUtAar: Boolean,
        forventetPeriode: Period
    ) {
        val grunnlag =
            TrygdetidPeriodeGrunnlag(
                periode =
                    FaktumNode(
                        verdi =
                            TrygdetidPeriodMedPoengAar(
                                fra = fra,
                                til = til,
                                poengInnAar = poengInnAar,
                                poengUtAar = poengUtAar
                            ),
                        kilde = "Z1234",
                        beskrivelse = "Periode"
                    )
            )

        val resultat = beregnTrygdetidForPeriode.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe forventetPeriode
    }

    @Test
    fun `antallDagerForEnMaanedTrygdetid skal returnere 30`() {
        val grunnlag = totalTrygdetidGrunnlag(listOf(Period.of(1, 0, 0)))

        val resultat = dagerPrMaanedTrygdetid.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe 30
    }

    @Test
    fun `totalTrygdetidFraPerioder skal summere perioder `() {
        val grunnlag =
            totalTrygdetidGrunnlag(
                listOf(
                    Period.of(1, 1, 1),
                    Period.of(1, 1, 1)
                )
            )

        val resultat = totalTrygdetidFraPerioder.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe Period.of(2, 2, 2)
    }

    @Test
    fun `totalTrygdetidFraPerioder skal summere perioder og legge til en maaned for hver 30 resterende dager`() {
        val grunnlag =
            totalTrygdetidGrunnlag(
                listOf(
                    Period.ofDays(11),
                    Period.ofDays(21),
                    Period.ofDays(29)
                )
            )

        val resultat = totalTrygdetidFraPerioder.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe Period.of(0, 2, 1)
    }

    @Test
    fun `totalTrygdetidFraPerioder skal summere perioder og normalisere overskytende maaneder til aar`() {
        val grunnlag =
            totalTrygdetidGrunnlag(
                listOf(
                    Period.ofMonths(11),
                    Period.ofMonths(2)
                )
            )

        val resultat = totalTrygdetidFraPerioder.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe Period.of(1, 1, 0)
    }

    @Test
    fun `totalTrygdetidFraPerioder skal normalisere overskytende dager til maaned`() {
        val grunnlag = totalTrygdetidGrunnlag(listOf(Period.ofDays(31)))

        val resultat = totalTrygdetidFraPerioder.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe Period.of(0, 1, 1)
    }

    @Test
    fun `maksTrygdetid skal returnere 40`() {
        val grunnlag = totalTrygdetidGrunnlag(listOf(Period.of(1, 0, 0)))

        val resultat = maksTrygdetid.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe 40
    }

    @Test
    fun `totalTrygdetidAvrundet skal runde opp dersom trygdetid har 6 maaneder eller mer`() {
        val grunnlag = totalTrygdetidGrunnlag(listOf(Period.of(10, 6, 2)))

        val resultat = totalTrygdetidAvrundet.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe 11
    }

    @Test
    fun `totalTrygdetidAvrundet skal runde ned dersom trygdetid har 5 maaneder eller mindre`() {
        val grunnlag = totalTrygdetidGrunnlag(listOf(Period.of(10, 5, 2)))

        val resultat = totalTrygdetidAvrundet.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe 10
    }

    @Test
    fun `beregnAntallAarTrygdetid skal returnere total trygdetid naar denne er mindre enn maks trygdetid`() {
        val grunnlag = totalTrygdetidGrunnlag(listOf(Period.ofYears(39)))

        val resultat = beregnAntallAarTotalTrygdetid.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe 39
    }

    @Test
    fun `beregnAntallAarTrygdetid skal returnere maks trygdetid naar denne er mindre enn total trygdetid`() {
        val grunnlag = totalTrygdetidGrunnlag(listOf(Period.ofYears(41)))

        val resultat = beregnAntallAarTotalTrygdetid.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe 40
    }

    private fun totalTrygdetidGrunnlag(perioder: List<Period>) =
        TotalTrygdetidGrunnlag(
            FaktumNode(perioder, Grunnlagsopplysning.Saksbehandler("Z12345", Tidspunkt.now()), "Trygdetidsperioder")
        )

    @ParameterizedTest(name = "{0}")
    @MethodSource("trygdetidGrunnlag")
    fun `beregnDetaljertBeregnetTrygdetid skal beregne riktig`(
        beskrivelse: String,
        perioder: List<TrygdetidGrunnlag>,
        forventet: DetaljertBeregnetTrygdetidResultat,
        datoer: Pair<LocalDate, LocalDate>?
    ) {
        val grunnlag =
            FaktumNode(
                verdi =
                    TrygdetidGrunnlagMedAvdoed(
                        trygdetidGrunnlagListe = perioder,
                        foedselsDato = datoer?.first ?: LocalDate.of(1981, 2, 21),
                        doedsDato = datoer?.second ?: LocalDate.of(2023, 3, 15)
                    ),
                kilde = Grunnlagsopplysning.Saksbehandler("Z12345", Tidspunkt.now()),
                beskrivelse = "Perioder"
            )

        val trygdetidGrunnlagMedAvdoedGrunnlag = TrygdetidGrunnlagMedAvdoedGrunnlag(grunnlag)

        val resultat =
            beregnDetaljertBeregnetTrygdetid.anvend(trygdetidGrunnlagMedAvdoedGrunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe forventet
    }

    companion object {
        @JvmStatic
        fun verdierForPoengAarTest(): Stream<Arguments> =
            Stream.of(
                Arguments.of(LocalDate.of(2023, 5, 10), LocalDate.of(2023, 5, 30), false, false, Period.ofDays(21)),
                Arguments.of(LocalDate.of(2023, 5, 10), LocalDate.of(2023, 5, 30), false, true, Period.ofYears(1)),
                Arguments.of(LocalDate.of(2023, 5, 10), LocalDate.of(2023, 5, 30), true, false, Period.ofYears(1)),
                Arguments.of(LocalDate.of(2023, 5, 10), LocalDate.of(2023, 5, 30), true, true, Period.ofYears(1)),
                Arguments.of(LocalDate.of(2020, 2, 10), LocalDate.of(2023, 5, 30), false, false, Period.of(3, 3, 21)),
                Arguments.of(LocalDate.of(2020, 2, 10), LocalDate.of(2023, 5, 30), false, true, Period.of(3, 10, 22)),
                Arguments.of(LocalDate.of(2020, 2, 10), LocalDate.of(2023, 5, 30), true, false, Period.of(3, 4, 30)),
                Arguments.of(LocalDate.of(2020, 2, 10), LocalDate.of(2023, 5, 30), true, true, Period.ofYears(4))
            )

        @JvmStatic
        fun trygdetidGrunnlag(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    "Nasjonal ikke poengAar",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2023, 4, 2),
                                til = LocalDate.of(2023, 6, 9)
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true
                        )
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(0, 3, 0),
                                antallMaaneder = 3
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(0, 3, 0),
                                antallMaaneder = 3
                            ),
                        fremtidigTrygdetidNorge = null,
                        fremtidigTrygdetidTeoretisk = null,
                        samletTrygdetidNorge = 0,
                        samletTrygdetidTeoretisk = 0,
                        prorataBroek = null
                    ),
                    null
                ),
                Arguments.of(
                    "Nasjonal poengInnAar",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2023, 4, 2),
                                til = LocalDate.of(2023, 6, 9)
                            ),
                            poengInnAar = true,
                            poengUtAar = false,
                            medIProrata = true
                        )
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(0, 6, 0),
                                antallMaaneder = 6
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(0, 6, 0),
                                antallMaaneder = 6
                            ),
                        fremtidigTrygdetidNorge = null,
                        fremtidigTrygdetidTeoretisk = null,
                        samletTrygdetidNorge = 0,
                        samletTrygdetidTeoretisk = 0,
                        prorataBroek = null
                    ),
                    null
                ),
                Arguments.of(
                    "Nasjonal poengUtAar",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2023, 4, 2),
                                til = LocalDate.of(2023, 6, 9)
                            ),
                            poengInnAar = false,
                            poengUtAar = true,
                            medIProrata = true
                        )
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(0, 9, 0),
                                antallMaaneder = 9
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(0, 9, 0),
                                antallMaaneder = 9
                            ),
                        fremtidigTrygdetidNorge = null,
                        fremtidigTrygdetidTeoretisk = null,
                        samletTrygdetidNorge = 0,
                        samletTrygdetidTeoretisk = 0,
                        prorataBroek = null
                    ),
                    null
                ),
                Arguments.of(
                    "Nasjonal poengInnAar og poengUtAar",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2023, 4, 2),
                                til = LocalDate.of(2023, 6, 9)
                            ),
                            poengInnAar = true,
                            poengUtAar = true,
                            medIProrata = true
                        )
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(1, 0, 0),
                                antallMaaneder = 12
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(1, 0, 0),
                                antallMaaneder = 12
                            ),
                        fremtidigTrygdetidNorge = null,
                        fremtidigTrygdetidTeoretisk = null,
                        samletTrygdetidNorge = 1,
                        samletTrygdetidTeoretisk = 1,
                        prorataBroek = null
                    ),
                    null
                ),
                Arguments.of(
                    "Utland",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(1992, 10, 3),
                                til = LocalDate.of(2004, 12, 12)
                            ),
                            poengInnAar = false,
                            poengUtAar = true,
                            medIProrata = true
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.SVERIGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2005, 1, 1),
                                til = LocalDate.of(2016, 12, 16)
                            ),
                            poengInnAar = false,
                            poengUtAar = true,
                            medIProrata = true
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2017, 1, 1),
                                til = LocalDate.of(2022, 12, 31)
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FREMTIDIG,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2023, 1, 29),
                                til = LocalDate.of(2042, 12, 31)
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true
                        )
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(18, 3, 0),
                                antallMaaneder = 219
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(30, 3, 0),
                                antallMaaneder = 363
                            ),
                        fremtidigTrygdetidNorge =
                            FremtidigTrygdetid(
                                periode = Period.of(15, 10, 0),
                                antallMaaneder = 190,
                                opptjeningstidIMaaneder = 362,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true
                            ),
                        fremtidigTrygdetidTeoretisk =
                            FremtidigTrygdetid(
                                periode = Period.of(20, 0, 0),
                                antallMaaneder = 240,
                                opptjeningstidIMaaneder = 362,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = false
                            ),
                        samletTrygdetidNorge = 34,
                        samletTrygdetidTeoretisk = 40,
                        prorataBroek = IntBroek(219, 363)
                    ),
                    Pair(
                        LocalDate.of(1976, 10, 3),
                        LocalDate.of(2023, 1, 29)
                    )
                ),
                Arguments.of(
                    "Utland 2",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(1997, 2, 21),
                                til = LocalDate.of(2010, 12, 17)
                            ),
                            poengInnAar = false,
                            poengUtAar = true,
                            medIProrata = true
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.SVERIGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2011, 1, 1),
                                til = LocalDate.of(2023, 2, 28)
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FREMTIDIG,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2023, 3, 15),
                                til = LocalDate.of(2047, 12, 31)
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true
                        )
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge =
                            FaktiskTrygdetid(
                                periode = Period.of(13, 11, 0),
                                antallMaaneder = 167
                            ),
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(26, 1, 0),
                                antallMaaneder = 313
                            ),
                        fremtidigTrygdetidNorge =
                            FremtidigTrygdetid(
                                periode = Period.of(19, 2, 0),
                                antallMaaneder = 230,
                                opptjeningstidIMaaneder = 312,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true
                            ),
                        fremtidigTrygdetidTeoretisk =
                            FremtidigTrygdetid(
                                periode = Period.of(24, 10, 0),
                                antallMaaneder = 298,
                                opptjeningstidIMaaneder = 312,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = false
                            ),
                        samletTrygdetidNorge = 33,
                        samletTrygdetidTeoretisk = 40,
                        prorataBroek = IntBroek(167, 313)
                    ),
                    Pair(
                        LocalDate.of(1981, 2, 21),
                        LocalDate.of(2023, 3, 15)
                    )
                ),
                Arguments.of(
                    "Utland 3",
                    listOf(
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FAKTISK,
                            LandNormalisert.SVERIGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2011, 1, 1),
                                til = LocalDate.of(2023, 2, 28)
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true
                        ),
                        byggTrygdetidGrunnlag(
                            TrygdetidType.FREMTIDIG,
                            LandNormalisert.NORGE.isoCode,
                            TrygdetidPeriode(
                                fra = LocalDate.of(2023, 3, 15),
                                til = LocalDate.of(2047, 12, 31)
                            ),
                            poengInnAar = false,
                            poengUtAar = false,
                            medIProrata = true
                        )
                    ),
                    DetaljertBeregnetTrygdetidResultat(
                        faktiskTrygdetidNorge = null,
                        faktiskTrygdetidTeoretisk =
                            FaktiskTrygdetid(
                                periode = Period.of(12, 2, 0),
                                antallMaaneder = 146
                            ),
                        fremtidigTrygdetidNorge =
                            FremtidigTrygdetid(
                                periode = Period.of(19, 2, 0),
                                antallMaaneder = 230,
                                opptjeningstidIMaaneder = 312,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true
                            ),
                        fremtidigTrygdetidTeoretisk =
                            FremtidigTrygdetid(
                                periode = Period.of(19, 2, 0),
                                antallMaaneder = 230,
                                opptjeningstidIMaaneder = 312,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = true
                            ),
                        samletTrygdetidNorge = 19,
                        samletTrygdetidTeoretisk = 31,
                        prorataBroek = IntBroek(0, 146)
                    ),
                    Pair(
                        LocalDate.of(1981, 2, 21),
                        LocalDate.of(2023, 3, 15)
                    )
                )
            )

        private fun byggTrygdetidGrunnlag(
            type: TrygdetidType,
            bosted: String,
            periode: TrygdetidPeriode,
            poengInnAar: Boolean,
            poengUtAar: Boolean,
            medIProrata: Boolean
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
            medIProrata
        )
    }
}