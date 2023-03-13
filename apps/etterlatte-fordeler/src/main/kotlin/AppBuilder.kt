package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.pdltjenester.PdlTjenesterKlient
import no.nav.etterlatte.security.ktor.clientCredential

class AppBuilder(val env: Map<String, String>) {

    internal fun pdlTjenesterKlient() = PdlTjenesterKlient(
        client = pdlTjenesterHttpClient(),
        apiUrl = requireNotNull(env["PDL_URL"])
    )

    private fun pdlTjenesterHttpClient() = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }
        install(Auth) {
            clientCredential {
                config =
                    env.toMutableMap().apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("PDL_AZURE_SCOPE"))) }
            }
        }
        defaultRequest {
            header(X_CORRELATION_ID, getCorrelationId())
        }
    }.also {
        Runtime.getRuntime().addShutdownHook(Thread { it.close() })
    }
}