package grunnlag

import no.nav.etterlatte.grunnlag.BehandlingHendelser
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException



class BehandlingHendelserTest {
    companion object {
        val melding = readFile("/behandlingsmelding.json")
        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }


    private val inspector = TestRapid().apply { BehandlingHendelser(this) }

    @Test
    fun `skal lese melding om behandling opprettet og lage opplysningsbehov`() {
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals(Opplysningstyper.SOEKER_PDL_V1.name, inspector.message(0).get("@behov").asText())
        Assertions.assertEquals(Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1.name, inspector.message(1).get("@behov").asText())
        Assertions.assertEquals(4, inspector.size)
    }
}
