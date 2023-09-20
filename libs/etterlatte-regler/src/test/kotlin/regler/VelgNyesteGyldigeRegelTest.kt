package no.nav.etterlatte.libs.regler

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VelgNyesteGyldigeRegelTest {
    data class Grunnlag(
        val testVerdi2021: FaktumNode<Int>,
        val testVerdi2022: FaktumNode<Int>,
        val testVerdi2023: FaktumNode<Int>,
    )

    private fun grunnlag() =
        Grunnlag(
            testVerdi2021 = FaktumNode(2021, saksbehandler, "Verdi for test"),
            testVerdi2022 = FaktumNode(2022, saksbehandler, "Verdi for test"),
            testVerdi2023 = FaktumNode(2023, saksbehandler, "Verdi for test"),
        )

    private val gjelderFra1900: LocalDate = LocalDate.of(1900, 1, 1)
    private val gjelderFra2021: LocalDate = LocalDate.of(2021, 1, 1)
    private val gjelderFra2022: LocalDate = LocalDate.of(2022, 1, 1)
    private val gjelderFra2023: LocalDate = LocalDate.of(2023, 1, 1)

    private val regel2021: Regel<Grunnlag, Int> =
        finnFaktumIGrunnlag(
            gjelderFra = gjelderFra2021,
            beskrivelse = "Finner testverdi for 2021",
            finnFaktum = Grunnlag::testVerdi2021,
            finnFelt = { it },
        )

    private val regel2022: Regel<Grunnlag, Int> =
        finnFaktumIGrunnlag(
            gjelderFra = gjelderFra2022,
            beskrivelse = "Finner testverdi for 2022",
            finnFaktum = Grunnlag::testVerdi2022,
            finnFelt = { it },
        )

    private val regel2023: Regel<Grunnlag, Int> =
        finnFaktumIGrunnlag(
            gjelderFra = gjelderFra2023,
            beskrivelse = "Finner testverdi for 2023",
            finnFaktum = Grunnlag::testVerdi2023,
            finnFelt = { it },
        )

    private val velgNyesteGyldigeRegel =
        RegelMeta(
            gjelderFra = gjelderFra1900,
            beskrivelse = "Finner testverdi for rett virkningstidspunkt",
            regelReferanse = regelReferanse,
        ) velgNyesteGyldige (regel2021 og regel2022 og regel2023)

    @Test
    fun `Skal velge regel med senest gjelderFra hvis virk er senere enn seneste regel`() {
        val resultat = velgNyesteGyldigeRegel.anvend(grunnlag(), RegelPeriode(LocalDate.of(2025, 1, 1)))

        resultat.verdi shouldBe 2023
        resultat.noder.size shouldBe 1
        when (val node = resultat.noder.first()) {
            is SubsumsjonsNode -> {
                node.verdi shouldBe 2023
                node.regel shouldBe regel2023
            }
            else -> throw Exception("Feil resultat")
        }
    }

    @Test
    fun `Skal velge nyeste regel som er gyldig`() {
        val resultat = velgNyesteGyldigeRegel.anvend(grunnlag(), RegelPeriode(LocalDate.of(2022, 5, 1)))

        resultat.verdi shouldBe 2022
        resultat.noder.size shouldBe 1
        when (val node = resultat.noder.first()) {
            is SubsumsjonsNode -> {
                node.verdi shouldBe 2022
                node.regel shouldBe regel2022
            }
            else -> throw Exception("Feil resultat")
        }
    }

    @Test
    fun `Skal velge den eneste potensielt gyldige regelen`() {
        val resultat = velgNyesteGyldigeRegel.anvend(grunnlag(), RegelPeriode(LocalDate.of(2021, 1, 1)))

        resultat.verdi shouldBe 2021
        resultat.noder.size shouldBe 1
        when (val node = resultat.noder.first()) {
            is SubsumsjonsNode -> {
                node.verdi shouldBe 2021
                node.regel shouldBe regel2021
            }
            else -> throw Exception("Feil resultat")
        }
    }

    @Test
    fun `Skal kaste exception hvis det ikke finnes gyldige reger paa et gitt tidspunkt`() {
        shouldThrow<IngenGyldigeReglerForTidspunktException> {
            velgNyesteGyldigeRegel.anvend(grunnlag(), RegelPeriode(LocalDate.of(2020, 1, 1)))
        }
    }
}
