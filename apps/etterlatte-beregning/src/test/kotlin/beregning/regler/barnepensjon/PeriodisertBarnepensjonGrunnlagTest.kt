package beregning.regler.barnepensjon

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.AvdoedForelder
import no.nav.etterlatte.beregning.regler.barnepensjon.BP_2024_DATO
import no.nav.etterlatte.beregning.regler.barnepensjon.PeriodisertBarnepensjonGrunnlag
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.PeriodisertGrunnlag
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PeriodisertBarnepensjonGrunnlagTest {

    private val soeskenKull = mockk<PeriodisertGrunnlag<FaktumNode<List<Folkeregisteridentifikator>>>>()
    private val avdoedForelder = mockk<PeriodisertGrunnlag<FaktumNode<AvdoedForelder>>>()
    private val institusjonsopphold = mockk<PeriodisertGrunnlag<FaktumNode<InstitusjonsoppholdBeregningsgrunnlag?>>>()
    private val avdoedeForeldre = mockk<PeriodisertGrunnlag<FaktumNode<List<Folkeregisteridentifikator>>>>()

    @BeforeEach
    fun setUp() {
        listOf(soeskenKull, avdoedForelder, institusjonsopphold, avdoedeForeldre).forEach { grunnlag ->
            every {
                grunnlag.finnAlleKnekkpunkter()
            } returns emptySet()
        }
    }

    @Test
    fun `finnAlleKnekkpunkter skal ekskludere søskenkull-knekkpunkter etter reformdato når nytt regelverk brukes`() {
        every {
            soeskenKull.finnAlleKnekkpunkter()
        } returns setOf(
            BP_2024_DATO.minusDays(30),
            BP_2024_DATO.minusDays(1),
            BP_2024_DATO,
            BP_2024_DATO.plusDays(1)
        )
        val barnepensjonGrunnlag = PeriodisertBarnepensjonGrunnlag(
            soeskenKull = soeskenKull,
            avdoedForelder = avdoedForelder,
            institusjonsopphold = institusjonsopphold,
            brukNyttRegelverk = true,
            avdoedeForeldre = avdoedeForeldre
        )
        barnepensjonGrunnlag.finnAlleKnekkpunkter() shouldContainExactly
            setOf(
                BP_2024_DATO.minusDays(30),
                BP_2024_DATO.minusDays(1)
            )
    }

    @Test
    fun `finnAlleKnekkpunkter skal ta med søskenkull-knekkpunkter etter reformdato når nytt regelverk ikke brukes`() {
        every {
            soeskenKull.finnAlleKnekkpunkter()
        } returns setOf(
            BP_2024_DATO.minusDays(30),
            BP_2024_DATO.minusDays(1),
            BP_2024_DATO,
            BP_2024_DATO.plusDays(1)
        )
        val barnepensjonGrunnlag = PeriodisertBarnepensjonGrunnlag(
            soeskenKull = soeskenKull,
            avdoedForelder = avdoedForelder,
            institusjonsopphold = institusjonsopphold,
            brukNyttRegelverk = false,
            avdoedeForeldre = avdoedeForeldre
        )
        barnepensjonGrunnlag.finnAlleKnekkpunkter() shouldContainExactly
            setOf(
                BP_2024_DATO.minusDays(30),
                BP_2024_DATO.minusDays(1),
                BP_2024_DATO,
                BP_2024_DATO.plusDays(1)
            )
    }

    @Test
    fun `finnAlleKnekkpunkter skal ekskludere avdøde-knekkpunkter før reformdato når nytt regelverk brukes`() {
        every {
            avdoedeForeldre.finnAlleKnekkpunkter()
        } returns setOf(
            BP_2024_DATO.minusYears(1),
            BP_2024_DATO.plusMonths(1)
        )
        val barnepensjonGrunnlag = PeriodisertBarnepensjonGrunnlag(
            soeskenKull = soeskenKull,
            avdoedForelder = avdoedForelder,
            institusjonsopphold = institusjonsopphold,
            brukNyttRegelverk = true,
            avdoedeForeldre = avdoedeForeldre
        )
        barnepensjonGrunnlag.finnAlleKnekkpunkter() shouldContainExactly
            setOf(
                BP_2024_DATO.plusMonths(1)
            )
    }

    @Test
    fun `finnAlleKnekkpunkter skal ekskludere alle avdøde-knekkpunkter når nytt regelverk ikke brukes`() {
        every {
            avdoedeForeldre.finnAlleKnekkpunkter()
        } returns setOf(
            BP_2024_DATO.minusYears(1),
            BP_2024_DATO.plusMonths(1)
        )
        val barnepensjonGrunnlag = PeriodisertBarnepensjonGrunnlag(
            soeskenKull = soeskenKull,
            avdoedForelder = avdoedForelder,
            institusjonsopphold = institusjonsopphold,
            brukNyttRegelverk = false,
            avdoedeForeldre = avdoedeForeldre
        )
        barnepensjonGrunnlag.finnAlleKnekkpunkter() shouldBe emptyList()
    }
}