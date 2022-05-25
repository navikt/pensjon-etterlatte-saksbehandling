package grunnlag

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.grunnlag.*
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import testutils.TestDbKontekst
import java.io.FileNotFoundException
import java.util.*



class BehandlingHendelserTest {
    companion object {
        val melding = readFile("/behandlingsmelding.json")
        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }


    private val inspector = TestRapid().apply { BehandlingHendelser(this) }

    @Test
    fun `skal lese melding om behandling opprettet og lage opplysningsbehov`() {
        Kontekst.set(Context(Self("testApp"), TestDbKontekst))
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør

        Assertions.assertEquals(Opplysningstyper.SOEKER_PDL_V1.name, inspector.message(0).get("@behov").asText())
        Assertions.assertEquals(Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1.name, inspector.message(1).get("@behov").asText())
        Assertions.assertEquals(4, inspector.size)
    }
}
