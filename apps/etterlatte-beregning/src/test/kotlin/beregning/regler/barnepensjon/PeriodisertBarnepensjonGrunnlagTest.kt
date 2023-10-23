package beregning.regler.barnepensjon

import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.regler.AnvendtTrygdetid
import no.nav.etterlatte.beregning.regler.barnepensjon.BP_2024_DATO
import no.nav.etterlatte.beregning.regler.barnepensjon.PeriodisertBarnepensjonGrunnlag
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.PeriodisertGrunnlag
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PeriodisertBarnepensjonGrunnlagTest {
    private val soeskenKull = mockk<PeriodisertGrunnlag<FaktumNode<List<Folkeregisteridentifikator>>>>()
    private val avdoedesTrygdetid = mockk<PeriodisertGrunnlag<FaktumNode<List<AnvendtTrygdetid>>>>()
    private val institusjonsopphold = mockk<PeriodisertGrunnlag<FaktumNode<InstitusjonsoppholdBeregningsgrunnlag?>>>()

    @BeforeEach
    fun setUp() {
        listOf(soeskenKull, avdoedesTrygdetid, institusjonsopphold).forEach { grunnlag ->
            every {
                grunnlag.finnAlleKnekkpunkter()
            } returns emptySet()
        }
    }

    @Test
    fun `finnAlleKnekkpunkter skal ekskludere søskenkull-knekkpunkter etter reformdato når nytt regelverk brukes`() {
        every {
            soeskenKull.finnAlleKnekkpunkter()
        } returns
            setOf(
                BP_2024_DATO.minusDays(30),
                BP_2024_DATO.minusDays(1),
                BP_2024_DATO,
                BP_2024_DATO.plusDays(1),
            )
        val barnepensjonGrunnlag =
            PeriodisertBarnepensjonGrunnlag(
                soeskenKull = soeskenKull,
                avdoedesTrygdetid = avdoedesTrygdetid,
                institusjonsopphold = institusjonsopphold,
                brukNyttRegelverk = true,
            )
        barnepensjonGrunnlag.finnAlleKnekkpunkter() shouldContainExactly
            setOf(
                BP_2024_DATO.minusDays(30),
                BP_2024_DATO.minusDays(1),
            )
    }

    @Test
    fun `finnAlleKnekkpunkter skal ta med søskenkull-knekkpunkter etter reformdato når nytt regelverk ikke brukes`() {
        every {
            soeskenKull.finnAlleKnekkpunkter()
        } returns
            setOf(
                BP_2024_DATO.minusDays(30),
                BP_2024_DATO.minusDays(1),
                BP_2024_DATO,
                BP_2024_DATO.plusDays(1),
            )
        val barnepensjonGrunnlag =
            PeriodisertBarnepensjonGrunnlag(
                soeskenKull = soeskenKull,
                avdoedesTrygdetid = avdoedesTrygdetid,
                institusjonsopphold = institusjonsopphold,
                brukNyttRegelverk = false,
            )
        barnepensjonGrunnlag.finnAlleKnekkpunkter() shouldContainExactly
            setOf(
                BP_2024_DATO.minusDays(30),
                BP_2024_DATO.minusDays(1),
                BP_2024_DATO,
                BP_2024_DATO.plusDays(1),
            )
    }
}
