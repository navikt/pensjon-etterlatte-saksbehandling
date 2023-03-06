package no.nav.etterlatte.adressebeskyttelse

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
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdlhendelse.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.module
import no.nav.etterlatte.sak.Sak
import no.nav.etterlatte.sak.Saker
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdressebeskyttelseTest : BehandlingIntegrationTest() {

    @BeforeAll
    fun start() = startServer()

    @AfterAll
    fun shutdown() = afterAll()

    @Test
    fun `Skal kunne se på en vanlig behandling før adressebeskyttelse men ikke etter`() {
        val fnr = Foedselsnummer.of("08071272487").value

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val client = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
            }
            application {
                module(beanFactory)
            }

            val sak: Sak = client.get("personer/$fnr/saker/${SakType.BARNEPENSJON}") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                it.body()
            }

            val saker: Saker = client.get("personer/$fnr/saker") {
                addAuthToken(tokenSaksbehandler)
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                it.body()
            }
            Assertions.assertEquals(1, saker.saker.size)
            val behandlingId = client.post("/behandlinger/foerstegangsbehandling") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    BehandlingsBehov(
                        sak.id,
                        Persongalleri("søker", "innsender", emptyList(), emptyList(), emptyList()),
                        Tidspunkt.now().toLocalDatetimeUTC().toString()
                    )
                )
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                UUID.fromString(it.body())
            }

            client.get("/behandlinger/foerstegangsbehandling/$behandlingId") {
                addAuthToken(systemBruker)
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get("/behandlinger/foerstegangsbehandling/$behandlingId") {
                addAuthToken(tokenSaksbehandler)
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
            }

            beanFactory.sakServiceAdressebeskyttelse().oppdaterAdressebeskyttelse(
                sak.id,
                AdressebeskyttelseGradering.STRENGT_FORTROLIG
            )

            client.get("/behandlinger/foerstegangsbehandling/$behandlingId") {
                addAuthToken(tokenSaksbehandler)
            }.let {
                Assertions.assertEquals(HttpStatusCode.NotFound, it.status)
            }
            // smoketest
            client.get("/fakeurlwithbehandlingsid/$behandlingId") {
                addAuthToken(tokenSaksbehandler)
            }.let {
                Assertions.assertEquals(HttpStatusCode.NotFound, it.status)
            }
            // systemBruker skal alltid ha tilgang
            client.get("/behandlinger/foerstegangsbehandling/$behandlingId") {
                addAuthToken(systemBruker)
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
            }
        }
    }
}