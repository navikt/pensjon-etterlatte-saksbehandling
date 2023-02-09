package no.nav.etterlatte

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
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
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Omberegningshendelse
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.sak.Sak
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OmberegningIntegrationTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:15")
    private val server: MockOAuth2Server = MockOAuth2Server()
    private lateinit var beanFactory: TestBeanFactory

    @BeforeAll
    fun startServer() {
        server.start(1234)

        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        beanFactory = TestBeanFactory(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password,
            azureAdAttestantClaim = azureAdAttestantClaim,
            azureAdSaksbehandlerClaim = azureAdSaksbehandlerClaim
        ).apply { dataSource().migrate() }

        beanFactory.behandlingHendelser().start()
    }

    @AfterAll
    fun afterAll() {
        server.shutdown()
        postgreSQLContainer.stop()
    }

    private val azureAdAttestantClaim: String by lazy {
        "0af3955f-df85-4eb0-b5b2-45bf2c8aeb9e"
    }

    private val azureAdSaksbehandlerClaim: String by lazy {
        "63f46f74-84a8-4d1c-87a8-78532ab3ae60"
    }

    private fun HttpRequestBuilder.addAuthToken(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private val tokenSaksbehandler: String by lazy {
        issueToken(
            mapOf(
                "navn" to "John Doe",
                "NAVident" to "Saksbehandler01"
            )
        )
    }

    private val tokenServiceUser: String by lazy {
        issueToken(
            mapOf(
                "NAVident" to "Saksbehandler01",
                "roles" to listOf("kan-sette-kilde") // TODO hva brukes dette til?
            )
        )
    }

    private fun issueToken(claims: Map<String, Any>) =
        server.issueToken(
            issuerId = ISSUER_ID,
            audience = CLIENT_ID,
            claims = claims
        ).serialize()

    private companion object {
        const val ISSUER_ID = "azure"
        const val CLIENT_ID = "mock-client-id"
    }

    @Test
    fun omberegn() {
        val fnr = "234"
        testApplication {
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
                        LocalDateTime.now().toString()
                    )
                )
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                UUID.fromString(it.body())
            }

            val omberegning = client.post("/omberegning") {
                addAuthToken(tokenServiceUser)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Omberegningshendelse(
                        1,
                        LocalDate.now(),
                        BehandlingType.REVURDERING,
                        RevurderingAarsak.GRUNNBELOEPREGULERING
                    )
                )
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                UUID.fromString(it.body())
            }

            client.get("/sak/1/behandlinger") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                it.body<BehandlingListe>().also { liste ->
                    Assertions.assertEquals(foerstegangsbehandling, liste.behandlinger[0].id)
                    Assertions.assertEquals(omberegning, liste.behandlinger[1].id)
                }
            }.also {
                Assertions.assertEquals(2, it.behandlinger.size)
            }
        }
    }
}