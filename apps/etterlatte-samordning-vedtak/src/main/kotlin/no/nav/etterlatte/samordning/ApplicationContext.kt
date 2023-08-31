package no.nav.etterlatte.samordning

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

class ApplicationContext(env: Map<String, String>) {
    val config: Config = ConfigFactory.load()
    val httpPort = env.getOrDefault("HTTP_PORT", "8080").toInt()
}