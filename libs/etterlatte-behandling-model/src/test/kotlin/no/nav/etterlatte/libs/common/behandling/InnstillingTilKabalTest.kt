package no.nav.etterlatte.libs.common.behandling

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class InnstillingTilKabalTest {
    private val objectMapper: ObjectMapper =
        JsonMapper.builder()
            .addModule(JavaTimeModule())
            .addModule(KotlinModule.Builder().build())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
            .build()

    @Test
    fun testMedTekst() {
        val innstillingTilKabal =
            objectMapper.readValue<InnstillingTilKabal>(
                """
            {
                "lovhjemmel": "FTRL_1_3_A",
                "brev": {"brevId": 42},
                "tekst": "b"
            }""",
            )
        innstillingTilKabal.lovhjemmel shouldBe KabalHjemmel.FTRL_1_3_A
        innstillingTilKabal.internKommentar shouldBe "b"
    }

    @Test
    fun testMedInternKommentar() {
        val innstillingTilKabal =
            objectMapper.readValue<InnstillingTilKabal>(
                """
            {
                "internKommentar": "c",
                "lovhjemmel": "FTRL_1_3_A",
                "brev": {"brevId": 42},
                "tekst": "b"
            }""",
            )
        innstillingTilKabal.lovhjemmel shouldBe KabalHjemmel.FTRL_1_3_A
        innstillingTilKabal.internKommentar shouldBe "c"
    }
}
