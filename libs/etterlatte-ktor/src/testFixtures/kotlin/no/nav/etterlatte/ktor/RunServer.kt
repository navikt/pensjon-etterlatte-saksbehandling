package no.nav.etterlatte.ktor

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.routing.Route
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.token.Issuer
import no.nav.security.mock.oauth2.MockOAuth2Server

fun ApplicationTestBuilder.runServerWithConfig(
    applicationConfig: Config,
    routePrefix: String = "",
    routes: Route.() -> Unit,
): HttpClient {
    environment {
        config = HoconApplicationConfig(applicationConfig)
    }

    application {
        restModule(this.log, routePrefix = routePrefix) {
            routes()
        }
    }
    return createClient {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }
    }
}

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

/*
Obs krever at du intercepter og legger på egen context hvis du ikke bruker Application.module i behandling appen som kaller settOppApplikasjonen()
Se TestHelper.kt i behandling
 */
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
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper),
            )
        }
    }
}
