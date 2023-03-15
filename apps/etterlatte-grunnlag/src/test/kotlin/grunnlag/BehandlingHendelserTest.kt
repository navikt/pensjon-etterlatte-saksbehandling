package no.nav.etterlatte.grunnlag

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.event.BehandlingRiverKey
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BehandlingHendelserTest {
    private val mockService = mockk<GrunnlagService>()

    private val inspector = TestRapid().apply { BehandlingHendelser(this, mockService) }

    @Test
    fun `skal lese melding om behandling gyldig fremsatt og lage opplysningsbehov`() {
        every {
            mockService.hentGrunnlagAvType(any(), Opplysningstype.PERSONGALLERI_V1)
        } returns mockk {
            every { opplysning } returns Persongalleri(
                soeker = "soeker",
                gjenlevende = listOf("gjenlevende"),
                avdoed = listOf("avdoed")
            ).toJsonNode()
        }

        val melding = behandlingOpprettetMelding()
        val inspector = inspector.apply { sendTestMessage(melding.toJson()) }.inspektør

        Assertions.assertEquals(
            Opplysningstype.SOEKER_PDL_V1.name,
            inspector.message(0).get("@behov").asText()
        )
        Assertions.assertEquals(
            Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1.name,
            inspector.message(1).get("@behov").asText()
        )
        Assertions.assertEquals(
            Opplysningstype.AVDOED_PDL_V1.name,
            inspector.message(2).get("@behov").asText()
        )
        Assertions.assertEquals(3, inspector.size)

        verify(exactly = 1) { mockService.hentGrunnlagAvType(1, Opplysningstype.PERSONGALLERI_V1) }
    }

    private fun behandlingOpprettetMelding() = JsonMessage.newMessage(
        mapOf(
            EVENT_NAME_KEY to "BEHANDLING:GYLDIG_FREMSATT",
            BehandlingRiverKey.sakIdKey to 1
        )
    )
}