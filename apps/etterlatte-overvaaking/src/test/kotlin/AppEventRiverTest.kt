import no.nav.etterlatte.AppEventRiver
import no.nav.etterlatte.appEventTypes
import no.nav.etterlatte.appEvents
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class AppEventRiverTest{


    @Test
    fun skalReagerePaaAppEvents(){
        val rapid = TestRapid()
        appEvents = emptyList()
        AppEventRiver(rapid)
        appEventTypes.forEach{
            rapid.sendTestMessage(JsonMessage.newMessage(mapOf("@event_name" to it)).toJson())
        }

        assertEquals(appEventTypes.size, appEvents.size)
    }

    @Test
    fun skalIkkeReagerePaaAndreEvents(){
        val rapid = TestRapid()
        appEvents = emptyList()
        AppEventRiver(rapid)
        rapid.sendTestMessage(JsonMessage.newMessage(mapOf("@event_name" to "søknad sendt")).toJson())
        rapid.sendTestMessage(JsonMessage.newMessage(mapOf("@event_name" to "søknad arkivert")).toJson())
        rapid.sendTestMessage(JsonMessage.newMessage(mapOf("@event_name" to "noe skjedde")).toJson())

        assertEquals(0, appEvents.size)
    }

    @Test
    fun eventListeSkalBegrensesTil50Innslag(){
        val rapid = TestRapid()
        appEvents = emptyList()

        AppEventRiver(rapid)
        val event = JsonMessage.newMessage(mapOf("@event_name" to appEventTypes[0])).toJson()
        repeat(49) {
            rapid.sendTestMessage(event)
        }

        assertEquals(49, appEvents.size)
        rapid.sendTestMessage(event)
        assertEquals(50, appEvents.size)
        rapid.sendTestMessage(event)
        assertEquals(50, appEvents.size)
        rapid.sendTestMessage(event)
        assertEquals(50, appEvents.size)
        rapid.sendTestMessage(event)
        assertEquals(50, appEvents.size)
        rapid.sendTestMessage(event)
        assertEquals(50, appEvents.size)
    }
}