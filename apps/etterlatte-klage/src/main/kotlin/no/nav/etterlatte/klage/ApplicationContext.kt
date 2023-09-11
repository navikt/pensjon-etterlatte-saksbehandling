package no.nav.etterlatte.klage

import no.nav.etterlatte.libs.common.requireEnvValue

internal class ApplicationContext {

    fun init() {
        val env = System.getenv().toMutableMap()
        val klageKafkakonsument = KlageKafkakonsument(
            env = env,
            topic = env.requireEnvValue("KLAGE_TOPIC")
        )
    }
}