package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.klienter.Vedtak
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.token.Bruker
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import testsupport.buildTestApplicationConfigurationForOauth
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

abstract class BehandlingIntegrationTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:15")
    private val server: MockOAuth2Server = MockOAuth2Server()
    protected lateinit var beanFactory: TestBeanFactory
    protected lateinit var hoconApplicationConfig: HoconApplicationConfig

    protected fun startServer() {
        server.start()

        val httpServer = server.config.httpServer
        hoconApplicationConfig = buildTestApplicationConfigurationForOauth(httpServer.port(), ISSUER_ID, CLIENT_ID)
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

    protected fun afterAll() {
        server.shutdown()
        postgreSQLContainer.stop()
    }

    private val azureAdAttestantClaim: String by lazy {
        "0af3955f-df85-4eb0-b5b2-45bf2c8aeb9e"
    }

    private val azureAdSaksbehandlerClaim: String by lazy {
        "63f46f74-84a8-4d1c-87a8-78532ab3ae60"
    }

    protected fun HttpRequestBuilder.addAuthToken(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    protected val tokenSaksbehandler: String by lazy {
        issueToken(
            mapOf(
                "navn" to "John Doe",
                "NAVident" to "Saksbehandler01"

            )
        )
    }

    protected val tokenAttestant: String by lazy {
        issueToken(
            mapOf(
                "navn" to "John Doe",
                "NAVident" to "Saksbehandler01",
                "groups" to listOf(
                    azureAdAttestantClaim,
                    azureAdSaksbehandlerClaim
                )
            )
        )
    }

    protected val tokenServiceUser: String by lazy {
        issueToken(
            mapOf(
                "NAVident" to "Saksbehandler01",
                "roles" to listOf("kan-sette-kilde") // TODO hva brukes dette til?
            )
        )
    }

    protected val systemBruker: String by lazy {
        val mittsystem = UUID.randomUUID().toString()
        issueToken(
            mapOf(
                "sub" to mittsystem,
                "oid" to mittsystem
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
}

class VedtakKlientTest : VedtakKlient {
    override suspend fun hentVedtak(behandlingId: String, bruker: Bruker): Vedtak? {
        TODO("Not yet implemented")
    }
}

class GrunnlagKlientTest : GrunnlagKlient {
    override suspend fun finnPersonOpplysning(
        sakId: Long,
        opplysningsType: Opplysningstype,
        bruker: Bruker
    ): Grunnlagsopplysning<Person>? {
        val personopplysning = personOpplysning(doedsdato = LocalDate.parse("2022-01-01"))
        return grunnlagsOpplysningMedPersonopplysning(personopplysning)
    }
}

class TestBeanFactory(
    private val jdbcUrl: String,
    private val username: String,
    private val password: String,
    private val azureAdSaksbehandlerClaim: String,
    private val azureAdAttestantClaim: String
) : CommonFactory() {
    override fun getSaksbehandlerGroupIdsByKey(): Map<String, String> =
        mapOf(
            "AZUREAD_ATTESTANT_GROUPID" to azureAdAttestantClaim,
            "AZUREAD_SAKSBEHANDLER_GROUPID" to azureAdSaksbehandlerClaim
        )

    val rapidSingleton: TestProdusent<String, String> by lazy() { TestProdusent() }

    private val dataSource: DataSource by lazy { DataSourceBuilder.createDataSource(jdbcUrl, username, password) }
    override fun dataSource(): DataSource = dataSource

    override fun rapid(): KafkaProdusent<String, String> = rapidSingleton

    override fun vedtakKlient(): VedtakKlient {
        return VedtakKlientTest()
    }

    override fun grunnlagKlient(): GrunnlagKlient {
        return GrunnlagKlientTest()
    }

    override fun pdlHttpClient(): HttpClient = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                if (request.url.fullPath.startsWith("/")) {
                    val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    val json = javaClass.getResource("")!!.readText() // TODO: endre name
                    respond(json, headers = headers)
                } else {
                    error(request.url.fullPath)
                }
            }
        }
        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper)
            )
        }
    }

    override fun grunnlagHttpClient(): HttpClient = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                if (request.url.fullPath.startsWith("/")) {
                    val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    respond(Grunnlag.empty().toJson(), headers = headers)
                } else {
                    error(request.url.fullPath)
                }
            }
        }
        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper)
            )
        }
    }

    override fun leaderElection() = LeaderElection(
        electorPath = "electorPath",
        httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { req ->
                    if (req.url.fullPath == "electorPath") {
                        respond("me")
                    } else {
                        error(req.url.fullPath)
                    }
                }
            }
        },
        me = "me"
    )
}