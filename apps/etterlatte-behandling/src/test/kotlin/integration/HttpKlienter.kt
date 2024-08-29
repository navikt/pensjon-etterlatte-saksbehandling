package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson

fun skjermingHttpClient(): HttpClient =
    HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                if (request.url.fullPath.contains("skjermet")) {
                    val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    respond(false.toJson(), headers = headers)
                } else {
                    error(request.url.fullPath)
                }
            }
        }
        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper),
            )
        }
    }

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
