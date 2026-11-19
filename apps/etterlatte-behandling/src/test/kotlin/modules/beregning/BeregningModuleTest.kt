package no.nav.etterlatte.modules.beregning

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.EnvKey.BRUK_INTERN_BEREGNING
import no.nav.etterlatte.attachMockContext
import no.nav.etterlatte.config.modules.BeregningDatabaseKey.BRUK_EGEN_DATABASE_FOR_BEREGNING
import no.nav.etterlatte.config.modules.BeregningModule
import no.nav.etterlatte.config.modules.BeregningRepositoryModule
import no.nav.etterlatte.config.modules.ServiceModule
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.token.issueSystembrukerToken
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.beregning.Sanksjon
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.mockSaksbehandler
import no.nav.etterlatte.modules.beregning.klienter.BeregningCrudKlient
import no.nav.etterlatte.modules.beregning.regler.behandling
import no.nav.etterlatte.modules.beregning.regler.lagreSanksjon
import no.nav.etterlatte.modules.beregning.regler.sanksjon
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource

internal class BeregningModuleTest {
    private val server = MockOAuth2Server()
    private val dataSource = mockk<DataSource>()
    private val crudKlient = mockk<BeregningCrudKlient>()
    private val serviceModule = mockk<ServiceModule>(relaxed = true)
    private val featureToggleService = mockk<FeatureToggleService>(relaxed = true)
    private var antallCrudKlienter = 0

    @BeforeEach
    fun setup() {
        server.start()
    }

    @AfterEach
    fun teardown() {
        server.shutdown()
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["false"])
    fun `uten aktivering registreres ingen ruter eller tjenester`(aktivert: String?) {
        val modul = modul(aktivert = aktivert)

        testApplication {
            val client = runServer(server) { modul.registrerRoutes(this) }
            teststier.forEach { sti ->
                client
                    .get(sti) {
                        header(HttpHeaders.Authorization, "Bearer ${server.issueSystembrukerToken()}")
                    }.status shouldBe HttpStatusCode.NotFound
            }
        }

        antallCrudKlienter shouldBe 0
        verify {
            serviceModule wasNot Called
            dataSource wasNot Called
            crudKlient wasNot Called
        }
    }

    @Test
    fun `ugyldig aktivering stopper oppstart`() {
        assertThrows<IllegalArgumentException> { modul(aktivert = "tru") }
        antallCrudKlienter shouldBe 0
        verify { serviceModule wasNot Called }
    }

    @Test
    fun `aktivering registrerer alle rutefamiliene`() {
        val modul = modul()

        testApplication {
            val client = runServer(server) { modul.registrerRoutes(this) }
            teststier.forEach { sti ->
                val forventetStatus =
                    if (sti.endsWith("/grunnbeloep")) HttpStatusCode.OK else HttpStatusCode.BadRequest
                client
                    .get(sti) {
                        header(HttpHeaders.Authorization, "Bearer ${server.issueSystembrukerToken()}")
                    }.status shouldBe forventetStatus
            }
        }

        antallCrudKlienter shouldBe 1
        verify { dataSource wasNot Called }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["false"])
    fun `rutene bruker CRUD-klienten gjennom tjenestene som standard og med databasebryteren av`(egenDatabase: String?) {
        val behandlingId = UUID.randomUUID()
        val lagret = sanksjon(behandlingId = behandlingId)
        val sanksjonId = requireNotNull(lagret.id)
        val ny = lagreSanksjon()
        val oppdatert = ny.copy(id = sanksjonId, beskrivelse = "Oppdatert")
        every { serviceModule.behandlingService.hentDetaljertBehandling(behandlingId, any()) } returns behandling(id = behandlingId)
        justRun { crudKlient.opprettSanksjon(behandlingId, ny.sakId, any(), ny) }
        every { crudKlient.hentSanksjon(behandlingId) } returns listOf(lagret)
        every { crudKlient.hentSanksjonMedId(sanksjonId) } returns lagret
        justRun { crudKlient.oppdaterSanksjon(oppdatert, any()) }
        every { crudKlient.slettSanksjon(sanksjonId) } returns 1
        val modul = modul(egenDatabase = egenDatabase)

        testApplication {
            val client =
                runServer(server) {
                    attachMockContext()
                    modul.registrerRoutes(this)
                }
            val token = server.issueSystembrukerToken()
            val sti = "/api/beregning/sanksjon/$behandlingId"

            listOf(ny, oppdatert).forEach { sanksjon ->
                client
                    .post(sti) {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(sanksjon.toJson())
                    }.status shouldBe HttpStatusCode.OK
            }

            val response = client.get(sti) { header(HttpHeaders.Authorization, "Bearer $token") }
            response.status shouldBe HttpStatusCode.OK
            response.body<List<Sanksjon>>() shouldBe listOf(lagret)

            client
                .delete("$sti/$sanksjonId") { header(HttpHeaders.Authorization, "Bearer $token") }
                .status shouldBe HttpStatusCode.OK
        }

        verify(exactly = 1) {
            crudKlient.opprettSanksjon(behandlingId, ny.sakId, any(), ny)
            crudKlient.oppdaterSanksjon(oppdatert, any())
            crudKlient.hentSanksjon(behandlingId)
            crudKlient.slettSanksjon(sanksjonId)
        }
        verify { dataSource wasNot Called }
    }

    @Test
    fun `rutene bruker lokal JDBC naar databasebryteren er paa uten aa opprette CRUD-klienten`() {
        val behandlingId = UUID.randomUUID()
        val connection = mockk<Connection>(relaxed = true)
        val statement = mockk<PreparedStatement>(relaxed = true)
        val resultSet = mockk<ResultSet>()
        every { dataSource.connection } returns connection
        every { connection.autoCommit } returns true
        every { connection.prepareStatement(any<String>()) } returns statement
        every { statement.executeQuery() } returns resultSet
        every { resultSet.next() } returns false
        every { resultSet.isClosed } returns false
        justRun { resultSet.close() }
        val modul = modul(egenDatabase = "true")

        testApplication {
            val client = runServer(server) { modul.registrerRoutes(this) }
            client
                .get("/api/beregning/sanksjon/$behandlingId") {
                    header(HttpHeaders.Authorization, "Bearer ${server.issueSystembrukerToken()}")
                }.status shouldBe HttpStatusCode.NoContent
        }

        antallCrudKlienter shouldBe 0
        verify(exactly = 1) {
            connection.prepareStatement("SELECT * FROM sanksjon WHERE behandling_id = ?")
            connection.commit()
            connection.close()
        }
        verify { crudKlient wasNot Called }
    }

    @Test
    fun `rutene avviser saksbehandler uten skrivetilgang foer data endres`() {
        val modul = modul()
        val behandlingId = UUID.randomUUID()
        val saksbehandler = mockSaksbehandler(enheter = emptyList())

        testApplication {
            val client =
                runServer(server) {
                    attachMockContext(saksbehandler)
                    modul.registrerRoutes(this)
                }
            listOf("$behandlingId", "sanksjon/$behandlingId", "beregningsgrunnlag/$behandlingId", "avkorting/$behandlingId/liste")
                .forEach { sti ->
                    client
                        .post("/api/beregning/$sti") {
                            header(HttpHeaders.Authorization, "Bearer ${server.issueSaksbehandlerToken()}")
                        }.status shouldBe HttpStatusCode.Forbidden
                }
        }

        verify { crudKlient wasNot Called }
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingStatus::class, names = ["ATTESTERT", "IVERKSATT"])
    fun `rutene tillater ikke endring av ferdig behandling`(status: BehandlingStatus) {
        val behandlingId = UUID.randomUUID()
        every { serviceModule.behandlingService.hentDetaljertBehandling(behandlingId, any()) } returns
            behandling(id = behandlingId, status = status)
        val modul = modul()

        testApplication {
            val client =
                runServer(server) {
                    attachMockContext()
                    modul.registrerRoutes(this)
                }
            client
                .post("/api/beregning/sanksjon/$behandlingId") {
                    header(HttpHeaders.Authorization, "Bearer ${server.issueSystembrukerToken()}")
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(lagreSanksjon().toJson())
                }.status shouldBe HttpStatusCode.Forbidden
        }

        verify { crudKlient wasNot Called }
    }

    @Test
    fun `manifestene holder flyttet beregning deaktivert og beholder CRUD som standard`() {
        val prosjektrot =
            generateSequence(File(".").canonicalFile) { it.parentFile }
                .first { it.resolve("settings.gradle.kts").isFile }
        listOf("dev", "prod").forEach { miljoe ->
            listOf("etterlatte-behandling", "etterlatte-saksbehandling-ui").forEach { app ->
                val manifest = prosjektrot.resolve("apps/$app/.nais/$miljoe.yaml").readText()
                Regex("""- name: BRUK_INTERN_BEREGNING\s+value: "(true|false)"""")
                    .find(manifest)
                    ?.groupValues
                    ?.get(1) shouldBe "false"
                if (app == "etterlatte-behandling") {
                    Regex("""- name: BRUK_EGEN_DATABASE_FOR_BEREGNING\s+value: "(true|false)"""")
                        .find(manifest)
                        ?.groupValues
                        ?.get(1) shouldBe "false"
                }
            }
        }
    }

    private fun modul(
        aktivert: String? = "true",
        egenDatabase: String? = null,
    ): BeregningModule {
        val verdier =
            buildMap<EnvEnum, String> {
                aktivert?.let { put(BRUK_INTERN_BEREGNING, it) }
                egenDatabase?.let { put(BRUK_EGEN_DATABASE_FOR_BEREGNING, it) }
            }
        val env = Miljoevariabler.httpClient(verdier)
        val repositories =
            BeregningRepositoryModule(dataSource, env) {
                antallCrudKlienter++
                crudKlient
            }
        return BeregningModule(env, repositories, serviceModule, featureToggleService)
    }

    private val teststier =
        listOf(
            "/api/beregning/ugyldig-id",
            "/api/beregning/beregningsgrunnlag/ugyldig-id",
            "/api/beregning/avkorting/ugyldig-id",
            "/api/beregning/sanksjon/ugyldig-id",
            "/api/beregning/ytelse-med-grunnlag/ugyldig-id",
            "/api/beregning/grunnbeloep",
        )
}
