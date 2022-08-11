package rapidsandrivers.vedlikehold

import com.fasterxml.jackson.databind.node.TextNode
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class VedlikeholdsriverTest {

    @Test
    fun `river skal lese medlinger om sletteing av sak`(){
        val service: VedlikeholdService = mockk()
        val rapid = TestRapid().apply {
            registrerVedlikeholdsriver(service)
        }

        every { service.slettSak(34) } returns Unit
        rapid.sendTestMessage(JsonMessage.newMessage(Vedlikeholdsriver.slettSakEventName, mapOf(
            "sakId" to 34,
        )).toJson())

        Assertions.assertEquals(1, rapid.inspektør.size)
        Assertions.assertNull(rapid.inspektør.message(0)["@feil"])
        Assertions.assertEquals("Sak slettet", rapid.inspektør.message(0)["@resultat"].textValue())
    }

    @Test
    fun `river skal poste feilmelding ved feil`(){
        val service: VedlikeholdService = mockk()
        val rapid = TestRapid().apply {
            registrerVedlikeholdsriver(service)
        }

        every { service.slettSak(34) } throws IllegalArgumentException()
        rapid.sendTestMessage(JsonMessage.newMessage(Vedlikeholdsriver.slettSakEventName, mapOf(
            "sakId" to 34,
        )).toJson())

        Assertions.assertEquals(1, rapid.inspektør.size)
        Assertions.assertNull(rapid.inspektør.message(0)["@resultat"])
        Assertions.assertTrue(rapid.inspektør.message(0)["@feil"] is TextNode)
    }
}