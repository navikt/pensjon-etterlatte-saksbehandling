package no.nav.etterlatte.ktor

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig

internal fun buildTestApplicationConfigurationForOauth(
    port: Int,
    issuerId: String,
    clientId: String,
): HoconApplicationConfig =
    HoconApplicationConfig(
        ConfigFactory.parseMap(
            mapOf(
                "no.nav.security.jwt.issuers" to
                    listOf(
                        mapOf(
                            "discoveryurl" to "http://localhost:$port/$issuerId/.well-known/openid-configuration",
                            "issuer_name" to issuerId,
                            "accepted_audience" to clientId,
                        ),
                    ),
            ),
        ),
    )
