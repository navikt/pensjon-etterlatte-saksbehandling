import no.nav.etterlatte.AppEventRiver
import no.nav.etterlatte.appEventTypes
import no.nav.etterlatte.appMap
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AppEventRiverTest {


    @Test
    fun skalReagerePaaAppEvents() {
        val rapid = TestRapid()
        appMap.clear()
        AppEventRiver(rapid)
        appEventTypes.forEach {
            rapid.sendTestMessage(JsonMessage.newMessage(mapOf(
                eventNameKey to it,
                "opprettet" to LocalDate.of(2021, 11, 12).atTime(13, 32),
                "app_name" to "app",
                "instance_id" to "instance"
            )).toJson())
        }

        assertEquals(appEventTypes.last(), appMap["app"]?.get("instance")?.first?.type)
    }

    @Test
    fun skalIkkeReagerePaaAndreEvents() {
        val rapid = TestRapid()
        appMap.clear()
        AppEventRiver(rapid)
        rapid.sendTestMessage(JsonMessage.newMessage(mapOf(eventNameKey to "søknad sendt")).toJson())
        rapid.sendTestMessage(JsonMessage.newMessage(mapOf(eventNameKey to "søknad arkivert")).toJson())
        rapid.sendTestMessage(JsonMessage.newMessage(mapOf(eventNameKey to "noe skjedde")).toJson())

        assertEquals(0, appMap.size)
    }
}
