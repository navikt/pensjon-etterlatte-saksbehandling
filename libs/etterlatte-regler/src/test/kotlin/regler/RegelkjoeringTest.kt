package no.nav.etterlatte.libs.regler

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.containADigit
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class RegelkjoeringTest {
    data class Grunnlag(
        val testVerdi2021: FaktumNode<Int>,
        val testVerdi2022: FaktumNode<Int>,
        val testVerdi2023: FaktumNode<Int>,
    )

    private val grunnlag =
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
    fun `Skal periodisere resultatet basert paa alle knekkpunkter i grafen`() {
        when (
            val perioder =
                velgNyesteGyldigeRegel.eksekver(KonstantGrunnlag(grunnlag), RegelPeriode(gjelderFra2021))
        ) {
            is RegelkjoeringResultat.Suksess -> {
                perioder.periodiserteResultater shouldHaveSize 3
                perioder.periodiserteResultater.map { it.periode } shouldContainExactly
                    setOf(
                        RegelPeriode(gjelderFra2021, gjelderFra2022.minusDays(1)),
                        RegelPeriode(gjelderFra2022, gjelderFra2023.minusDays(1)),
                        RegelPeriode(gjelderFra2023),
                    )
                perioder.reglerVersjon should containADigit()
            }

            is RegelkjoeringResultat.UgyldigPeriode -> throw Exception("Skal ikke ha ugyldige perioder")
        }
    }

    @Test
    fun `Skal returnere med ugyldige regler for perioden hvis man eksekverer en regel utenfor gyldig periode`() {
        when (
            val resultat =
                velgNyesteGyldigeRegel.eksekver(KonstantGrunnlag(grunnlag), RegelPeriode(gjelderFra1900))
        ) {
            is RegelkjoeringResultat.Suksess -> throw Exception("Skal ha ugyldige perioder")
            is RegelkjoeringResultat.UgyldigPeriode -> {
                resultat.ugyldigeReglerForPeriode shouldHaveSize 1
                resultat.ugyldigeReglerForPeriode[0] shouldBe velgNyesteGyldigeRegel
                resultat.reglerVersjon should containADigit()
            }
        }
    }
}
