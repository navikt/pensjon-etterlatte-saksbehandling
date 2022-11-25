import io.mockk.mockk
import io.mockk.spyk
import no.nav.etterlatte.BeregningRepository
import no.nav.etterlatte.LesBeregningsmelding
import no.nav.etterlatte.model.BeregningService
import no.nav.etterlatte.model.VilkaarsvurderingKlient
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LesBeregningsmeldingTest {
    companion object {
        private fun readFile(file: String) =
            Companion::class.java.getResource(file)?.readText() ?: throw FileNotFoundException("Fant ikke filen $file")
    }
    private val vilkaarsvurderingKlientImpl = mockk<VilkaarsvurderingKlient>()
    private val beregningRepository = mockk<BeregningRepository>()
    private val inspector = spyk(
        TestRapid().apply {
            LesBeregningsmelding(
                this,
                BeregningService(beregningRepository, vilkaarsvurderingKlientImpl, mockk(), mockk())
            )
        }
    )

    @Test
    fun `skal beregne en melding som er vilkaarsvurdert og legge melding med beregning paa kafka`() {
        val melding = readFile("/Ny_soeknad.json")
        inspector.sendTestMessage(melding)

        Assertions.assertNotNull(inspector.inspektør.message(0).get("beregning"))
    }

    @Test
    fun `kan returnere en beregning naar saksbehandler har sendt in egne opplysninger`() {
        val melding = readFile("/Soesken_i_beregning_override.json")
        inspector.sendTestMessage(melding)

        Assertions.assertNotNull(inspector.inspektør.message(0).get("beregning"))
    }
}