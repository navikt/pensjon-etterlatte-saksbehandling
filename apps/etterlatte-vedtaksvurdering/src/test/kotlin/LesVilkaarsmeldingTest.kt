package no.nav.etterlatte

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.rivers.LagreVilkaarsresultat
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LesVilkaarsmeldingTest {

    companion object {
        val melding = readFile("/melding.json")

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }

    private val vedtaksvurderingServiceMock = mockk<VedtaksvurderingService>()
    private val inspector = TestRapid().apply { LagreVilkaarsresultat(this, vedtaksvurderingServiceMock) }
    private val vilkaarsresultatMock = VilkaarResultat(VurderingsResultat.OPPFYLT, emptyList(), LocalDateTime.parse("2022-03-03 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))

    @Test
    fun `skal lese melding`() {
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør
        every { vedtaksvurderingServiceMock.lagreVilkaarsresultat("6", UUID.fromString("42bd0adf-6d46-43cd-84fa-ff2c06709cf7"), vilkaarsresultatMock) } returns Unit
        print(inspector.message(0))
        // Assertions.assertEquals("BEHANDLING:GRUNNLAGENDRET", inspector.message(0).get("@event").asText())
    }

}