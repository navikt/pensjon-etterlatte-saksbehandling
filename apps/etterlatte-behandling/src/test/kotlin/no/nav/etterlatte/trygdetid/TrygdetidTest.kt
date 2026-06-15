package no.nav.etterlatte.trygdetid

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.trygdetid.land.LandNormalisert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class TrygdetidTest {
    private val trygdetid =
        trygdetid(
            trygdetidGrunnlag =
                listOf(
                    trygdetidGrunnlag(
                        periode =
                            TrygdetidPeriode(
                                fra = LocalDate.of(2017, 1, 1),
                                til = LocalDate.of(2017, 6, 30),
                            ),
                    ),
                    trygdetidGrunnlag(
                        periode =
                            TrygdetidPeriode(
                                fra = LocalDate.of(2020, 1, 1),
                                til = LocalDate.of(2020, 12, 31),
                            ),
                    ),
                    trygdetidGrunnlag(
                        periode =
                            TrygdetidPeriode(
                                fra = LocalDate.of(2022, 1, 1),
                                til = LocalDate.of(2022, 12, 31),
                            ),
                    ),
                ),
        )

    @Test
    fun `Skal kunne legge til gyldige trygdetidsperioder`() {
        val oppdatert =
            trygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(
                trygdetidGrunnlag(
                    periode =
                        TrygdetidPeriode(
                            fra = LocalDate.of(2018, 1, 1),
                            til = LocalDate.of(2019, 12, 31),
                        ),
                ),
            )
        oppdatert.trygdetidGrunnlag.size shouldBe 4
        oppdatert.trygdetidGrunnlag.any { it.periode.fra == LocalDate.of(2018, 1, 1) } shouldBe true
    }

    @Test
    fun `Skal kaste feil ved overlapp av trygdetidsperiode paa slutten av perioden`() {
        val overlappendePeriode =
            trygdetidGrunnlag(
                periode =
                    TrygdetidPeriode(
                        fra = LocalDate.of(2019, 5, 1),
                        til = LocalDate.of(2020, 5, 15),
                    ),
            )

        assertThrows<OverlappendePeriodeException> {
            trygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(overlappendePeriode)
        }
    }

    @Test
    fun `Skal kaste feil ved overlapp av trygdetidsperiode paa starten av perioden`() {
        val overlappendePeriode =
            trygdetidGrunnlag(
                periode =
                    TrygdetidPeriode(
                        fra = LocalDate.of(2020, 5, 20),
                        til = LocalDate.of(2021, 1, 1),
                    ),
            )

        assertThrows<OverlappendePeriodeException> {
            trygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(overlappendePeriode)
        }
    }

    @Test
    fun `Skal kaste feil ved overlapp i midten av en periode`() {
        val overlappendePeriode =
            trygdetidGrunnlag(
                periode =
                    TrygdetidPeriode(
                        fra = LocalDate.of(2020, 3, 20),
                        til = LocalDate.of(2020, 6, 1),
                    ),
            )

        assertThrows<OverlappendePeriodeException> {
            trygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(overlappendePeriode)
        }
    }

    @Test
    fun `Skal kaste feil ved overlapp over en periode`() {
        val overlappendePeriode =
            trygdetidGrunnlag(
                periode =
                    TrygdetidPeriode(
                        fra = LocalDate.of(2019, 3, 20),
                        til = LocalDate.of(2023, 6, 1),
                    ),
            )

        assertThrows<OverlappendePeriodeException> {
            trygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(overlappendePeriode)
        }
    }

    @Test
    fun `Skal kaste feil ved overlapp over en periode med poeng aar`() {
        val overlappendePeriode =
            trygdetidGrunnlag(
                poengInnAar = true,
                periode =
                    TrygdetidPeriode(
                        fra = LocalDate.of(2017, 8, 31),
                        til = LocalDate.of(2017, 12, 31),
                    ),
            )

        assertThrows<OverlappendePeriodeException> {
            trygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(overlappendePeriode)
        }
    }

    @Test
    fun `Skal kunne legge til uansett input sorteringsrekkefølge`() {
        val usortertTrygdetid =
            trygdetid(
                trygdetidGrunnlag =
                    listOf(
                        trygdetidGrunnlag(
                            periode = TrygdetidPeriode(LocalDate.of(2014, 5, 20), LocalDate.of(2027, 12, 31)),
                            trygdetidType = TrygdetidType.FREMTIDIG,
                        ),
                        trygdetidGrunnlag(
                            periode = TrygdetidPeriode(LocalDate.of(2006, 1, 1), LocalDate.of(2014, 4, 30)),
                            poengInnAar = true,
                            trygdetidType = TrygdetidType.FAKTISK,
                        ),
                        trygdetidGrunnlag(
                            periode = TrygdetidPeriode(LocalDate.of(2002, 1, 1), LocalDate.of(2005, 12, 31)),
                            trygdetidType = TrygdetidType.FAKTISK,
                            bosted = LandNormalisert.POLEN.isoCode,
                        ),
                    ),
            )

        val oppdatert =
            usortertTrygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(
                trygdetidGrunnlag(
                    periode = TrygdetidPeriode(LocalDate.of(2000, 6, 1), LocalDate.of(2000, 6, 18)),
                    trygdetidType = TrygdetidType.FAKTISK,
                ),
            )

        oppdatert.trygdetidGrunnlag.size shouldBe 4
    }

    @Test
    fun `Skal kunne legge til grunnlag i vilkaarlig rekkefoelge ogsaa hvor det er poengaar som trigger endring av periode`() {
        val usortertTrygdetid =
            trygdetid(
                trygdetidGrunnlag =
                    listOf(
                        trygdetidGrunnlag(
                            periode = TrygdetidPeriode(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 9, 1)),
                            trygdetidType = TrygdetidType.FAKTISK,
                        ),
                        trygdetidGrunnlag(
                            periode = TrygdetidPeriode(LocalDate.of(2022, 5, 1), LocalDate.of(2022, 8, 31)),
                            trygdetidType = TrygdetidType.FAKTISK,
                        ),
                        trygdetidGrunnlag(
                            periode = TrygdetidPeriode(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 11, 30)),
                            poengInnAar = true,
                            trygdetidType = TrygdetidType.FAKTISK,
                        ),
                        trygdetidGrunnlag(
                            periode = TrygdetidPeriode(LocalDate.of(2019, 12, 1), LocalDate.of(2020, 3, 28)),
                            trygdetidType = TrygdetidType.FAKTISK,
                        ),
                    ),
            )

        val oppdatert =
            usortertTrygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(
                trygdetidGrunnlag(
                    periode = TrygdetidPeriode(LocalDate.of(2021, 5, 29), LocalDate.of(2021, 7, 27)),
                    poengInnAar = true,
                    trygdetidType = TrygdetidType.FAKTISK,
                ),
            )

        oppdatert.trygdetidGrunnlag.size shouldBe 5
    }
}
