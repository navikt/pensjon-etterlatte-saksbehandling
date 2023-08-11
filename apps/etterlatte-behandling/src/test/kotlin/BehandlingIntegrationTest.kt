package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.SaksbehandlerEnhet
import no.nav.etterlatte.behandling.domain.SaksbehandlerTema
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakOgRolle
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.oppgave.GosysOppgaveKlient
import no.nav.etterlatte.oppgave.GosysOppgaver
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Claims
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import testsupport.buildTestApplicationConfigurationForOauth
import java.time.LocalDate
import java.util.*

abstract class BehandlingIntegrationTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private val server: MockOAuth2Server = MockOAuth2Server()
    protected lateinit var applicationContext: ApplicationContext
    protected lateinit var hoconApplicationConfig: HoconApplicationConfig

    protected fun startServer(norg2Klient: Norg2Klient? = null) {
        server.start()

        val httpServer = server.config.httpServer
        hoconApplicationConfig = buildTestApplicationConfigurationForOauth(httpServer.port(), AZURE_ISSUER, CLIENT_ID)
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        applicationContext = ApplicationContext(
            env = System.getenv().toMutableMap().apply {
                put("KAFKA_RAPID_TOPIC", "test")
                put("DB_HOST", postgreSQLContainer.host)
                put("DB_USERNAME", postgreSQLContainer.username)
                put("DB_PASSWORD", postgreSQLContainer.password)
                put("DB_PORT", postgreSQLContainer.firstMappedPort.toString())
                put("DB_DATABASE", postgreSQLContainer.databaseName)
                put("AZUREAD_ATTESTANT_GROUPID", azureAdAttestantClaim)
                put("AZUREAD_SAKSBEHANDLER_GROUPID", azureAdSaksbehandlerClaim)
                put("AZUREAD_STRENGT_FORTROLIG_GROUPID", azureAdStrengtFortroligClaim)
                put("AZUREAD_FORTROLIG_GROUPID", "ea930b6b-9397-44d9-b9e6-f4cf527a632a")
                put("AZUREAD_NASJONAL_TILGANG_UTEN_LOGG_GROUPID", "753805ea-65a7-4855-bdc3-e6130348df9f")
                put("AZUREAD_NASJONAL_TILGANG_MED_LOGG_GROUPID", "ea7411eb-8b48-41a0-bc56-7b521fbf0c25")
                put("AZUREAD_EGEN_ANSATT_GROUPID", "1")
                put("HENDELSE_JOB_FREKVENS", "1")
                put("HENDELSE_MINUTTER_GAMLE_HENDELSER", "1")
                put("NORG2_URL", "http://localhost")
                put("ELECTOR_PATH", "http://localhost")
                put("NAVANSATT_URL", "http://localhost")
                put("SKJERMING_URL", "http://localhost")
                put("KAN_BRUKE_NY_OPPGAVELISTE", "true")
                put("OPPGAVE_URL", "http://localhost")
                put("OPPGAVE_SCOPE", "scope")
            }.let { Miljoevariabler(it) },
            config = ConfigFactory.parseMap(hoconApplicationConfig.toMap()),
            rapid = TestProdusent(),
            featureToggleService = DummyFeatureToggleService(),
            pdlHttpClient = pdlHttpClient(),
            skjermingHttpKlient = skjermingHttpClient(),
            grunnlagHttpClient = grunnlagHttpClient(),
            leaderElectionHttpClient = leaderElection(),
            navAnsattKlient = NavAnsattKlientTest(),
            norg2Klient = norg2Klient ?: Norg2KlientTest(),
            grunnlagKlientObo = GrunnlagKlientTest(),
            gosysOppgaveKlient = GosysOppgaveKlientTest()
        ).also {
            it.dataSource.migrate()
        }
    }

    fun skjermingHttpClient(): HttpClient = HttpClient(MockEngine) {
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
                JacksonConverter(objectMapper)
            )
        }
    }

    fun pdlHttpClient(): HttpClient = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                if (request.url.fullPath.contains("geografisktilknytning")) {
                    val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    val json = GeografiskTilknytning(kommune = "0301").toJson()
                    respond(json, headers = headers)
                } else if (request.url.fullPath.contains("folkeregisteridenter")) {
                    val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    val json = emptyMap<String, String>().toJson()
                    respond(json, headers = headers)
                } else if (request.url.fullPath.startsWith("/")) {
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

    fun grunnlagHttpClient(): HttpClient = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                if (request.url.fullPath.matches(Regex("api/grunnlag/[0-9]{11}"))) {
                    val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    respond(Grunnlag.empty().toJson(), headers = headers)
                } else if (request.url.fullPath.endsWith("/PERSONGALLERI_V1")) {
                    val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    respond(
                        content = Grunnlagsopplysning(
                            id = UUID.randomUUID(),
                            kilde = Grunnlagsopplysning.Privatperson("fnr", Tidspunkt.now()),
                            meta = emptyMap<String, String>().toObjectNode(),
                            opplysningType = Opplysningstype.PERSONGALLERI_V1,
                            opplysning = Persongalleri(
                                "soeker",
                                "innsender",
                                listOf("soesken"),
                                listOf("avdoed"),
                                listOf("gjenlevende")
                            )
                        ).toJson(),
                        headers = headers
                    )
                } else if (request.url.fullPath.endsWith("/roller")) {
                    val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    respond(
                        PersonMedSakerOgRoller("08071272487", listOf(SakOgRolle(1, Saksrolle.SOEKER))).toJson(),
                        headers = headers
                    )
                } else if (request.url.fullPath.endsWith("/saker")) {
                    val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    respond(
                        setOf(1).toJson(),
                        headers = headers
                    )
                } else if (request.url.fullPath.endsWith("/oppdater-grunnlag")) {
                    respondOk()
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

    fun leaderElection() = HttpClient(MockEngine) {
        engine {
            addHandler { req ->
                if (req.url.fullPath == "electorPath") {
                    respond("me")
                } else {
                    error(req.url.fullPath)
                }
            }
        }
    }

    fun resetDatabase() {
        applicationContext.dataSource.connection.use {
            it.prepareStatement(
                """
                TRUNCATE behandling CASCADE;
                TRUNCATE behandlinghendelse CASCADE;
                TRUNCATE grunnlagsendringshendelse CASCADE;
                TRUNCATE sak CASCADE;
                
                ALTER SEQUENCE behandlinghendelse_id_seq RESTART WITH 1;
                ALTER SEQUENCE sak_id_seq RESTART WITH 1;
                """.trimIndent()
            ).execute()
        }
    }

    protected fun afterAll() {
        server.shutdown()
        postgreSQLContainer.stop()
    }

    private val azureAdStrengtFortroligClaim: String by lazy {
        "5ef775f2-61f8-4283-bf3d-8d03f428aa14"
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
                Claims.NAVident.toString() to "Saksbehandler01",
                "groups" to listOf(azureAdSaksbehandlerClaim)
            )
        )
    }

    protected val tokenAttestant: String by lazy {
        issueToken(
            mapOf(
                "navn" to "John Doe",
                Claims.NAVident.toString() to "Saksbehandler02",
                "groups" to listOf(
                    azureAdAttestantClaim,
                    azureAdSaksbehandlerClaim
                )
            )
        )
    }

    protected val tokenSaksbehandlerMedStrengtFortrolig: String by lazy {
        issueToken(
            mapOf(
                "navn" to "John Doe",
                Claims.NAVident.toString() to "saksebehandlerstrengtfortrolig",
                "groups" to listOf(
                    azureAdAttestantClaim,
                    azureAdSaksbehandlerClaim,
                    azureAdStrengtFortroligClaim
                )
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
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = claims
        ).serialize()

    private companion object {
        const val CLIENT_ID = "mock-client-id"
    }
}

class GrunnlagKlientTest : GrunnlagKlient {
    override suspend fun finnPersonOpplysning(
        sakId: Long,
        opplysningsType: Opplysningstype,
        brukerTokenInfo: BrukerTokenInfo
    ): Grunnlagsopplysning<Person> {
        val personopplysning = personOpplysning(doedsdato = LocalDate.parse("2022-01-01"))
        return grunnlagsOpplysningMedPersonopplysning(personopplysning)
    }

    override suspend fun hentPersongalleri(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo
    ): Grunnlagsopplysning<Persongalleri>? {
        return Grunnlagsopplysning(
            id = UUID.randomUUID(),
            kilde = Grunnlagsopplysning.Privatperson("fnr", Tidspunkt.now()),
            meta = emptyMap<String, String>().toObjectNode(),
            opplysningType = Opplysningstype.PERSONGALLERI_V1,
            opplysning = Persongalleri(
                "soeker",
                "innsender",
                listOf("soesken"),
                listOf("avdoed"),
                listOf("gjenlevende")
            )
        )
    }
}

class GosysOppgaveKlientTest : GosysOppgaveKlient {
    override suspend fun hentOppgaver(tema: String, enhetsnr: String, brukerTokenInfo: BrukerTokenInfo): GosysOppgaver {
        return GosysOppgaver(0, emptyList())
    }
}

class Norg2KlientTest : Norg2Klient {
    override fun hentEnheterForOmraade(tema: String, omraade: String): List<ArbeidsFordelingEnhet> {
        return listOf(ArbeidsFordelingEnhet("NAV Familie- og pensjonsytelser Steinkjer", "4817"))
    }
}

class NavAnsattKlientTest : NavAnsattKlient {
    override suspend fun hentSaksbehandlerEnhet(ident: String): List<SaksbehandlerEnhet> {
        return listOf(
            SaksbehandlerEnhet(Enheter.defaultEnhet.enhetNr, Enheter.defaultEnhet.navn),
            SaksbehandlerEnhet(Enheter.STEINKJER.enhetNr, Enheter.STEINKJER.navn)
        )
    }

    override suspend fun hentSaksbehandlerTema(ident: String): List<SaksbehandlerTema> {
        return listOf(SaksbehandlerTema(SakType.BARNEPENSJON.name), SaksbehandlerTema(SakType.OMSTILLINGSSTOENAD.name))
    }
}