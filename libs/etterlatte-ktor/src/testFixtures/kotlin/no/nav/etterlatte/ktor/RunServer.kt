package no.nav.etterlatte.ktor

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.log
import io.ktor.server.routing.Route
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.security.mock.oauth2.MockOAuth2Server
import testsupport.buildTestApplicationConfigurationForOauth

fun ApplicationTestBuilder.runServer(
    server: MockOAuth2Server,
    routePrefix: String = "",
    routes: Route.() -> Unit,
): HttpClient {
    environment {
        config = buildTestApplicationConfigurationForOauth(server.config.httpServer.port(), AZURE_ISSUER, CLIENT_ID)
    }
    application {
        restModule(this.log, routePrefix = routePrefix) {
            routes()
        }
    }

    return createClient {
        install(ContentNegotiation) {
            jackson { registerModule(JavaTimeModule()) }
        }
    }
}
