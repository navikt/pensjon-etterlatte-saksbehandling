package no.nav.etterlatte.libs.common.sak

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SakIdTest {
    @Test
    fun `SakId brukt som JSON key kan deserialiseres`() {
        val sakIdKeyMap =
            mapOf(
                SakId(1234) to "1234",
                SakId(4567) to "1234",
            )

        val serialisertMap = sakIdKeyMap.toJson()
        val deserialisertMap = objectMapper.readValue<Map<SakId, String>>(serialisertMap)
        assertEquals(sakIdKeyMap, deserialisertMap)
    }

    @Test
    fun `Long sakid brukt som JSON key kan deserialiseres til saKid value class`() {
        val sakIdValue = "1123234"
        val sakIdKey = 456713L
        val sakIdKeyMap = mapOf(sakIdKey to sakIdValue) // Just to be clear så må det ikke være SakId for å bli det)

        val serialisertMap = sakIdKeyMap.toJson()
        val deserialisertMap = objectMapper.readValue<Map<SakId, String>>(serialisertMap)
        deserialisertMap.keys.size shouldBe 1
        deserialisertMap.entries.first().value shouldBe sakIdValue
        deserialisertMap.entries.first().key shouldBe SakId(sakIdKey)
    }

    @Test
    fun `SakIdKeyDeserializer ødelegger ikke for andre keys i maps`() {
        val longKeyMap = mapOf(123L to "123", 456L to "456")
        val serialisertMap = longKeyMap.toJson()
        val deserialisertMap = objectMapper.readValue<Map<Long, String>>(serialisertMap)
        assertEquals(longKeyMap, deserialisertMap)
    }

    @Test
    fun `SakId brukt som verdi kan deserialiseres`() {
        val sakId = SakId(1337)
        val serialisert = sakId.toJson()
        val deserialisert = objectMapper.readValue<SakId>(serialisert)
        assertEquals(sakId, deserialisert)
    }
}
