package no.nav.etterlatte.ktor

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.routing.Route
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.Issuer
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.security.mock.oauth2.MockOAuth2Server

fun ApplicationTestBuilder.runServerWithModule(
    server: MockOAuth2Server,
    function: Application.() -> Unit,
): HttpClient {
    environment {
        config = buildTestApplicationConfigurationForOauth(server.config.httpServer.port(), Issuer.AZURE.issuerName)
    }
    application {
        function()
    }
    return createClient {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }
    }
}

fun ApplicationTestBuilder.runServer(
    server: MockOAuth2Server,
    routePrefix: String = "",
    withMetrics: Boolean = false,
    routes: Route.() -> Unit,
): HttpClient {
    environment {
        config = buildTestApplicationConfigurationForOauth(server.config.httpServer.port(), Issuer.AZURE.issuerName)
    }
    application {
        restModule(this.log, routePrefix = routePrefix, withMetrics = withMetrics) {
            routes()
        }
    }

    return createClient {
        install(ContentNegotiation) {
            jackson { registerModule(JavaTimeModule()) }
        }
    }
}
