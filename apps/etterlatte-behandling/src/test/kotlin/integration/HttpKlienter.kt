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
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakidOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toObjectNode
import java.util.UUID

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

fun grunnlagHttpClient(): HttpClient =
    HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                if (request.url.fullPath.matches(Regex("api/grunnlag/behandling/*"))) {
                    val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    respond(Grunnlag.empty().toJson(), headers = headers)
                } else if (request.url.fullPath.endsWith("/PERSONGALLERI_V1")) {
                    val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    respond(
                        content =
                            Grunnlagsopplysning(
                                id = UUID.randomUUID(),
                                kilde = Grunnlagsopplysning.Privatperson("fnr", Tidspunkt.now()),
                                meta = emptyMap<String, String>().toObjectNode(),
                                opplysningType = Opplysningstype.PERSONGALLERI_V1,
                                opplysning =
                                    Persongalleri(
                                        "soeker",
                                        "innsender",
                                        listOf("soesken"),
                                        listOf("avdoed"),
                                        listOf("gjenlevende"),
                                    ),
                            ).toJson(),
                        headers = headers,
                    )
                } else if (request.url.fullPath.endsWith("/roller")) {
                    val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    respond(
                        PersonMedSakerOgRoller("08071272487", listOf(SakidOgRolle(1, Saksrolle.SOEKER))).toJson(),
                        headers = headers,
                    )
                } else if (request.url.fullPath.endsWith("/saker")) {
                    val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    respond(
                        setOf(1).toJson(),
                        headers = headers,
                    )
                } else if (request.url.fullPath.contains("/grunnlag/sak")) {
                    val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    respond(
                        Grunnlag.empty().toJson(),
                        headers = headers,
                    )
                } else if (request.url.fullPath.endsWith("/oppdater-grunnlag")) {
                    respondOk()
                } else if (request.url.fullPath.endsWith("/opprett-grunnlag")) {
                    respondOk()
                } else if (request.url.fullPath.contains("/laas-til-behandling/")) {
                    respondOk()
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
