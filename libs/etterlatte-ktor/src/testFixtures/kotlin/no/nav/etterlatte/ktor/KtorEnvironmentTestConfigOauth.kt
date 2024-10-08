package no.nav.etterlatte.ktor

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.ktor.token.CLIENT_ID
import no.nav.security.mock.oauth2.MockOAuth2Server

internal fun buildTestApplicationConfigurationForOauth(
    port: Int,
    issuerId: String,
): HoconApplicationConfig =
    HoconApplicationConfig(
        ConfigFactory.parseMap(
            mapOf(
                "no.nav.security.jwt.issuers" to
                    listOf(
                        mapOf(
                            "discoveryurl" to "http://localhost:$port/$issuerId/.well-known/openid-configuration",
                            "issuer_name" to issuerId,
                            "accepted_audience" to CLIENT_ID,
                        ),
                    ),
            ),
        ),
    )

fun MockOAuth2Server.startRandomPort() = this.start()
