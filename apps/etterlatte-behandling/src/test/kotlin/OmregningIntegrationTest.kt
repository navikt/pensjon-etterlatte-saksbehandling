package no.nav.etterlatte

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import no.nav.etterlatte.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.sak.Sak
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OmregningIntegrationTest : BehandlingIntegrationTest() {

    @BeforeAll
    fun start() = startServer()

    @AfterAll
    fun shutdown() = afterAll()

    @AfterEach
    fun beforeEach() {
        beanFactory.resetDatabase()
    }

    @ParameterizedTest(name = "Kan opprette {0} omregningstype paa sak")
    @EnumSource(Prosesstype::class)
    fun `kan opprette omregning paa sak som har foerstegangsbehandling`(prosesstype: Prosesstype) {
        val fnr = "234"
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val client = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
            }
            application { module(beanFactory) }

            client.get("/saker/$fnr") {
                addAuthToken(tokenSaksbehandler)
            }.apply {
                Assertions.assertEquals(HttpStatusCode.NotFound, status)
            }
            val sak: Sak = client.get("/personer/$fnr/saker/BARNEPENSJON") {
                addAuthToken(tokenSaksbehandler)
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, status)
            }.body()

            client.get("/saker/${sak.id}") {
                addAuthToken(tokenSaksbehandler)
            }.also {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                val lestSak: Sak = it.body()
                Assertions.assertEquals(fnr, lestSak.ident)
                Assertions.assertEquals(SakType.BARNEPENSJON, lestSak.sakType)
            }
            val foerstegangsbehandling = client.post("/behandlinger/foerstegangsbehandling") {
                addAuthToken(tokenServiceUser)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    BehandlingsBehov(
                        1,
                        Persongalleri("søker", "innsender", emptyList(), emptyList(), emptyList()),
                        Tidspunkt.now().toLocalDatetimeUTC().toString()
                    )
                )
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                UUID.fromString(it.body())
            }

            val omregning = client.post("/omregning") {
                addAuthToken(tokenServiceUser)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Omregningshendelse(
                        1,
                        LocalDate.now(),
                        null,
                        prosesstype
                    )
                )
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                it.body<UUID>()
            }

            client.get("/sak/1/behandlinger") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                it.body<BehandlingListe>().also { liste ->
                    Assertions.assertEquals(foerstegangsbehandling, liste.behandlinger[0].id)
                    Assertions.assertEquals(omregning, liste.behandlinger[1].id)
                }
            }.also {
                Assertions.assertEquals(2, it.behandlinger.size)
            }
        }
    }
}