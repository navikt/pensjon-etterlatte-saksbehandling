package grunnlag

import kafkameldinger.behandlingOpprettetMelding
import no.nav.etterlatte.grunnlag.BehandlingHendelser
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BehandlingHendelserTest {
    private val inspector = TestRapid().apply { BehandlingHendelser(this) }

    @Test
    fun `skal lese melding om behandling opprettet og lage opplysningsbehov`() {
        val melding = behandlingOpprettetMelding()
        val inspector = inspector.apply { sendTestMessage(melding.toJson()) }.inspektør

        Assertions.assertEquals(Opplysningstype.SOEKER_PDL_V1.name, inspector.message(0).get("@behov").asText())
        Assertions.assertEquals(
            Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1.name,
            inspector.message(1).get("@behov").asText()
        )
        Assertions.assertEquals(
            Opplysningstype.AVDOED_PDL_V1.name,
            inspector.message(2).get("@behov").asText()
        )
        Assertions.assertEquals(3, inspector.size)
    }
}