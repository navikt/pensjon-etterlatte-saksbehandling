package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.etterlatte.libs.common.objectMapper

fun klageHttpClientTest() =
    HttpClient(MockEngine) {
        engine {
            addHandler {
                respondOk()
            }
        }
        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper),
            )
        }
    }

fun migreringHttpClientTest() =
    HttpClient(MockEngine) {
        engine {
            addHandler {
                respondOk()
            }
        }
        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper),
            )
        }
    }
