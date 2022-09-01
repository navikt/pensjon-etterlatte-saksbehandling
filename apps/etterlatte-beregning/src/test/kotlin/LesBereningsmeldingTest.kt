import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultatType
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.Endringskode
import no.nav.etterlatte.model.BeregningService
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LesBereningsmeldingTest {
    companion object {
        val melding = readFile("/Nyere.json")

        fun readFile(file: String) =
            Companion::class.java.getResource(file)?.readText() ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val beregningServiceMock = mockk<BeregningService>()
    private val inspector = spyk(TestRapid().apply { LesBeregningsmelding(this, beregningServiceMock) })

    @Test
    fun `skal beregne en melding som er vilkaarsvurdert og legge melding med beregning paa kafka`() {
        val mockedBeregningsResultat = BeregningsResultat(
            id = UUID.randomUUID(),
            type = Beregningstyper.GP,
            endringskode = Endringskode.NY,
            resultat = BeregningsResultatType.BEREGNET,
            beregningsperioder = listOf(),
            beregnetDato = LocalDateTime.now(),
            grunnlagVersjon = 0
        )
        every {
            beregningServiceMock.beregnResultat(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns mockedBeregningsResultat
        inspector.apply { sendTestMessage(melding) }
        verify(timeout = 5000) {
            inspector.publish(
                match {
                    it.contains("beregning")
                }
            )
        }
    }
}