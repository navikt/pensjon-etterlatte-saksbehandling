package no.nav.etterlatte.libs.regler

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class VisitorTest {
    private object TestGrunnlag

    private val regel1 = definerKonstant<TestGrunnlag, Int>(GJELDER_FRA_1, "Tallet 1", regelReferanse, 1)
    private val regel2 = definerKonstant<TestGrunnlag, Int>(GJELDER_FRA_2, "Tallet 2", regelReferanse, 2)
    private val regel3 = definerKonstant<TestGrunnlag, Long>(GJELDER_FRA_2, "Tallet 3", regelReferanse, 3)
    private val regel4 = definerKonstant<TestGrunnlag, Int>(GJELDER_FRA_3, "Tallet 4", regelReferanse, 4)

    private val regelSomBrukerVerdienFraTreAndreRegler =
        RegelMeta(
            gjelderFra = GJELDER_FRA_3,
            beskrivelse = "Regel som bruker resultatet av tre andre regler",
            regelReferanse = regelReferanse,
        ) benytter regel1 og regel2 og regel3 med { verdi1, verdi2, verdi3 ->
            verdi1 + verdi2 + verdi3
        }
    private val regelSomKombinererToRegler =
        RegelMeta(
            gjelderFra = GJELDER_FRA_3,
            beskrivelse = "Regel som kombinerer to regler",
            regelReferanse = regelReferanse,
        ) benytter regelSomBrukerVerdienFraTreAndreRegler og regel4 med { verdi1, verdi2 ->
            verdi1 + verdi2
        }

    @Test
    fun `Skal finne alle unike knekkpunkter i grafen`() {
        val knekkpunkter = regelSomKombinererToRegler.finnAlleKnekkpunkter()

        knekkpunkter.size shouldBe 3
        knekkpunkter shouldContainExactlyInAnyOrder listOf(GJELDER_FRA_1, GJELDER_FRA_2, GJELDER_FRA_3)
    }

    @Test
    fun `Skal finne ugyldige regler for en periode `() {
        val ugyldigPeriode = RegelPeriode(GJELDER_FRA_2)

        val ugyldigeReglerForPeriode = regelSomKombinererToRegler.finnUgyldigePerioder(ugyldigPeriode)

        ugyldigeReglerForPeriode.size shouldBe 3
        ugyldigeReglerForPeriode shouldContainExactlyInAnyOrder
            listOf(
                regel4,
                regelSomKombinererToRegler,
                regelSomBrukerVerdienFraTreAndreRegler,
            )
    }

    @Test
    fun `Skal ikke finne noen ugyldige regler dersom fraDato i perioden er senere eller lik alle reglers gjelderFra`() {
        val ugyldigPeriode = RegelPeriode(GJELDER_FRA_3)

        val ugyldigeReglerForPeriode = regelSomKombinererToRegler.finnUgyldigePerioder(ugyldigPeriode)

        ugyldigeReglerForPeriode shouldHaveSize 0
    }

    @Test
    fun `Skal finne ugyldige periode i VelgNyesteGyldigeRegel kun dersom ingen perioder matcher`() {
        val velgNyesteGyldigeRegler =
            RegelMeta(
                gjelderFra = GJELDER_FRA_3,
                beskrivelse = "Regel som velger nyeste regel",
                regelReferanse = regelReferanse,
            ) velgNyesteGyldige (regel1 og regel2)

        velgNyesteGyldigeRegler.finnUgyldigePerioder(RegelPeriode(GJELDER_FRA_1.minusMonths(1))) shouldHaveSize 1
        velgNyesteGyldigeRegler.finnUgyldigePerioder(RegelPeriode(GJELDER_FRA_1)) shouldHaveSize 0
        velgNyesteGyldigeRegler.finnUgyldigePerioder(RegelPeriode(GJELDER_FRA_2)) shouldHaveSize 0
        velgNyesteGyldigeRegler.finnUgyldigePerioder(RegelPeriode(GJELDER_FRA_3)) shouldHaveSize 0
    }

    private companion object {
        private val GJELDER_FRA_1 = LocalDate.of(2030, 1, 1)
        private val GJELDER_FRA_2 = LocalDate.of(2040, 1, 1)
        private val GJELDER_FRA_3 = LocalDate.of(2050, 1, 1)
    }
}
