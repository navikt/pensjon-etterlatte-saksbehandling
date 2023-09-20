package trygdetid

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.trygdetid.OverlappendePeriodeException
import no.nav.etterlatte.trygdetid.TrygdetidPeriode
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
        oppdatert.trygdetidGrunnlag.size shouldBe 3
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
}
