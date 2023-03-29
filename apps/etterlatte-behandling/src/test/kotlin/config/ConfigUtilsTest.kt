package no.nav.etterlatte.config

import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ConfigUtilsTest {

    @Test
    fun `samler verdier fra baade applicationconf og miljoevariabler`() {
        val conf = ConfigFactory.load()
        val env = mapOf(
            "key1" to "value1",
            "funksjonsbrytere.enabled" to "false",
            "ETTERLATTE_GRUNNLAG_CLIENT_ID" to "jwkt"
        )
        val samla = samle(conf, env)
        Assertions.assertEquals("false", samla["funksjonsbrytere.enabled"])
        Assertions.assertEquals("https://unleash.nais.io/api/", samla["funksjonsbrytere.unleash.uri"])
        Assertions.assertEquals("value1", samla["key1"])
    }
}