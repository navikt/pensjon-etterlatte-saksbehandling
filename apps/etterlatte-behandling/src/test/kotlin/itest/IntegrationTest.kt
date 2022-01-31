package no.nav.etterlatte.itest

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import no.nav.etterlatte.*
import no.nav.etterlatte.behandling.*
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.sak.Sak
import no.nav.etterlatte.sikkerhet.tokenTestSupportAcceptsAllTokens
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant
import java.util.*


class ApplicationTest {
    @Test
    fun verdikjedetest() {
        val fnr = "123"
        val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:12")
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)


        withTestApplication({
            module(TestBeanFactory(postgreSQLContainer.jdbcUrl))
        }) {
            handleRequest(HttpMethod.Get, "/saker/123") {
                addAuthSaksbehandler()
            }.apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            val sak: Sak = handleRequest(HttpMethod.Get, "/personer/$fnr/saker/BP") {
                addAuthSaksbehandler()
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }.response.content!!.let { objectMapper.readValue(it) }
            handleRequest(HttpMethod.Get, "/saker/${sak.id}") {
                addAuthSaksbehandler()
            }.also {
                assertEquals(HttpStatusCode.OK, it.response.status())
                val lestSak: Sak = objectMapper.readValue(it.response.content!!)
                assertEquals("123", lestSak.ident)
                assertEquals("BP", lestSak.sakType)

            }

            val behandlingId = handleRequest(HttpMethod.Post, "/behandlinger") {
                addAuthServiceBruker()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    no.nav.etterlatte.libs.common.objectMapper.writeValueAsString(
                        BehandlingsBehov(
                            1,
                            listOf(
                                Behandlingsopplysning(
                                    UUID.randomUUID(),
                                    Behandlingsopplysning.Privatperson("1234", Instant.now()),
                                    "dato_mottatt",
                                    objectMapper.createObjectNode(),
                                    objectMapper.readTree("""{"dato": "2022-05-14"}""") as ObjectNode
                                )
                            )
                        )
                    )
                )
            }.let {
                assertEquals(HttpStatusCode.OK, it.response.status())
                UUID.fromString(it.response.content)
            }

            handleRequest(HttpMethod.Get, "/sak/1/behandlinger") {
                addAuthSaksbehandler()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.let {
                assertEquals(HttpStatusCode.OK, it.response.status())
                objectMapper.readValue<BehandlingSammendragListe>(it.response.content!!)
            }.also {
                assertEquals(1, it.behandlinger.size)
            }

            handleRequest(HttpMethod.Post, "/behandlinger/$behandlingId/grunnlag/trygdetid") {
                addAuthSaksbehandler()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """{
    "tt_anv": 22
}"""
                )
            }.also {
                assertEquals(HttpStatusCode.OK, it.response.status())
            }


            handleRequest(HttpMethod.Post, "/behandlinger/$behandlingId/vilkaarsproeving") {
                addAuthSaksbehandler()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.also {
                assertEquals(HttpStatusCode.OK, it.response.status())
            }

            handleRequest(HttpMethod.Post, "/behandlinger/$behandlingId/beregning") {
                addAuthSaksbehandler()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(objectMapper.writeValueAsString(Beregning(emptyList(), 123)))
            }.also {
                assertEquals(HttpStatusCode.OK, it.response.status())
            }

            handleRequest(HttpMethod.Get, "/behandlinger/$behandlingId") {
                addAuthSaksbehandler()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.also {
                assertEquals(HttpStatusCode.OK, it.response.status())
                println(it.response.content?.let {
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                        objectMapper.readTree(it)
                    )
                })
                val behandling: Behandling = (objectMapper.readValue(it.response.content!!))
                assertNotNull(behandling.grunnlag.find { it.opplysningType == "dato_mottatt" })
                assertEquals(
                    22,
                    behandling.grunnlag.find { it.opplysningType == "trygdetid" }!!.opplysning["tt_anv"].numberValue()
                        .toInt()
                )

            }
        }

        postgreSQLContainer.stop()
    }
}

val clientCredentialTokenMedKanSetteKildeRolle =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImVuLWFwcCIsIm9pZCI6ImVuLWFwcCIsIm5hbWUiOiJKb2huIERvZSIsImlhdCI6MTUxNjIzOTAyMiwiTkFWaWRlbnQiOiJTYWtzYmVoYW5kbGVyMDEiLCJyb2xlcyI6WyJrYW4tc2V0dGUta2lsZGUiXX0.2ftwnoZiUfUa_J6WUkqj_Wdugb0CnvVXsEs-JYnQw_g"
val saksbehandlerToken =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImF6dXJlLWlkIGZvciBzYWtzYmVoYW5kbGVyIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJOQVZpZGVudCI6IlNha3NiZWhhbmRsZXIwMSJ9.271mDij4YsO4Kk8w8AvX5BXxlEA8U-UAOtdG1Ix_kQY"

fun TestApplicationRequest.addAuthSaksbehandler() {
    addHeader(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
}

fun TestApplicationRequest.addAuthServiceBruker() {
    addHeader(HttpHeaders.Authorization, "Bearer $clientCredentialTokenMedKanSetteKildeRolle")
}


class TestBeanFactory(private val jdbcUrl: String) : CommonFactory() {
    override fun datasourceBuilder(): DataSourceBuilder = DataSourceBuilder(mapOf("DB_JDBC_URL" to jdbcUrl))
    override fun vilkaarKlient(): VilkaarKlient = NoOpVilkaarKlient()
    override fun tokenValidering(): Authentication.Configuration.() -> Unit =
        Authentication.Configuration::tokenTestSupportAcceptsAllTokens
}