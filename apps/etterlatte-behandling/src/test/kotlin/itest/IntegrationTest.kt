package no.nav.etterlatte.itest

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import no.nav.etterlatte.*
import no.nav.etterlatte.behandling.*
import no.nav.etterlatte.sak.Sak
import no.nav.etterlatte.sikkerhet.tokenTestSupportAcceptsAllTokens
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
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
            handleRequest(HttpMethod.Get, "/saker/123"){
                addAuth()
            }.apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            val sak:Sak = handleRequest(HttpMethod.Get, "/personer/$fnr/saker/BP"){
                addAuth()
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }.response.content!!.let { objectMapper.readValue(it) }
            handleRequest(HttpMethod.Get, "/saker/${sak.id}"){
                addAuth()
            }.also {
                assertEquals(HttpStatusCode.OK, it.response.status())
                val lestSak:Sak = objectMapper.readValue(it.response.content!!)
                assertEquals("123", lestSak.ident)
                assertEquals("BP", lestSak.sakType)

            }

            val behandlingId = handleRequest(HttpMethod.Post, "/behandlinger"){
                addAuth()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """{
    "sak": 1
}""")
            }.let {
                assertEquals(HttpStatusCode.OK, it.response.status())
                UUID.fromString(objectMapper.readValue(it.response.content!!))
            }

            handleRequest(HttpMethod.Get, "/sak/1/behandlinger"){
                addAuth()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.let {
                assertEquals(HttpStatusCode.OK, it.response.status())
                objectMapper.readValue<BehandlingSammendragListe>(it.response.content!!)
            }.also {
                assertEquals(1, it.behandlinger.size)
            }

            handleRequest(HttpMethod.Post, "/behandlinger/$behandlingId/grunnlag/trygdetid"){
                addAuth()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """{
    "tt_anv": 22
}""")
            }.also {
                assertEquals(HttpStatusCode.OK, it.response.status())
            }


            handleRequest(HttpMethod.Post, "/behandlinger/$behandlingId/grunnlag/trygdetid"){
                addAuth()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """{
    "tt_anv": 22
}""")
            }.also {
                assertEquals(HttpStatusCode.OK, it.response.status())
            }


            handleRequest(HttpMethod.Post, "/behandlinger/$behandlingId/vilkaarsproeving"){
                addAuth()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                //setBody(objectMapper.writeValueAsString(Vilkårsprøving(emptyList(), objectMapper.createObjectNode(), "Johan")))
            }.also {
                assertEquals(HttpStatusCode.OK, it.response.status())
            }

            handleRequest(HttpMethod.Post, "/behandlinger/$behandlingId/beregning"){
                addAuth()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(objectMapper.writeValueAsString(Beregning(emptyList(), 123)))
            }.also {
                assertEquals(HttpStatusCode.OK, it.response.status())
            }



            handleRequest(HttpMethod.Get, "/behandlinger/$behandlingId"){
                addAuth()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.also {
                assertEquals(HttpStatusCode.OK, it.response.status())
                println(it.response.content?.let { objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                    objectMapper.readTree(it)) })
                val behandling: Behandling = (objectMapper.readValue(it.response.content!!))
                assertEquals("trygdetid",behandling.grunnlag[0].opplysningType)
                assertEquals(22,behandling.grunnlag[0].opplysning["tt_anv"].numberValue().toInt())

            }
        }
        
        postgreSQLContainer.stop()
    }
}

fun TestApplicationRequest.addAuth(){
    addHeader(HttpHeaders.Authorization, "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoiSm9obiBEb2UiLCJpYXQiOjE1MTYyMzkwMjIsIk5BVmlkZW50IjoiU2Frc2JlaGFuZGxlcjAxIn0.GOkpURd2cKRjX5V0lTA-ZApk8E05VOUcAMvJ0RE_2r4")
}


class TestBeanFactory(private val jdbcUrl: String): CommonFactory(){
    override fun datasourceBuilder(): DataSourceBuilder = DataSourceBuilder(mapOf("DB_JDBC_URL" to jdbcUrl))
    override fun vilkaarKlient(): VilkaarKlient = NoOpVilkaarKlient()
    override fun tokenValidering(): Authentication.Configuration.() -> Unit = Authentication.Configuration::tokenTestSupportAcceptsAllTokens
}