package no.nav.etterlatte.itest

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
import io.ktor.server.auth.Authentication
import io.ktor.server.testing.testApplication
import no.nav.etterlatte.CommonFactory
import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.behandling.BehandlingsBehov
import no.nav.etterlatte.behandling.HendelseDao
import no.nav.etterlatte.behandling.VedtakHendelse
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.module
import no.nav.etterlatte.oppgave.OppgaveListeDto
import no.nav.etterlatte.sak.Sak
import no.nav.etterlatte.sikkerhet.tokenTestSupportAcceptsAllTokens
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ApplicationTest {
    @Test
    fun verdikjedetest() {
        val fnr = "123"
        val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:12")
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        var behandlingOpprettet: UUID? = null
        val beans = TestBeanFactory(postgreSQLContainer.jdbcUrl)

        testApplication {

            val client = createClient {
                install(ContentNegotiation) {
                    jackson {
                        registerModule(JavaTimeModule())
                    }
                }
            }
            install(Authentication) {
                tokenTestSupportAcceptsAllTokens()
            }
            application { module(beans) }
            client.get("/saker/123") {
                addAuthSaksbehandler()
            }.apply {
                assertEquals(HttpStatusCode.NotFound, status)
            }
            val sak: Sak = client.get("/personer/$fnr/saker/BP") {
                addAuthSaksbehandler()
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
            }.body()

            client.get("/saker/${sak.id}") {
                addAuthSaksbehandler()
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val lestSak: Sak = it.body()
                assertEquals("123", lestSak.ident)
                assertEquals("BP", lestSak.sakType)

            }

            val behandlingId = client.post("/behandlinger") {
                addAuthServiceBruker()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    BehandlingsBehov(
                        1,
                        Persongalleri("søker", "innsender", emptyList(), emptyList(), emptyList()),
                        LocalDateTime.now().toString()
                    )

                )
            }.let {
                assertEquals(HttpStatusCode.OK, it.status)
                UUID.fromString(it.body())
            }
            behandlingOpprettet = behandlingId

            client.get("/sak/1/behandlinger") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.let {
                assertEquals(HttpStatusCode.OK, it.status)
                it.body<BehandlingListe>()
            }.also {
                assertEquals(1, it.behandlinger.size)
            }

            client.post("/behandlinger/$behandlingId/gyldigfremsatt") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    GyldighetsResultat(
                        VurderingsResultat.OPPFYLT,
                        listOf(
                            VurdertGyldighet(
                                GyldighetsTyper.INNSENDER_ER_FORELDER,
                                VurderingsResultat.OPPFYLT,
                                "innsenderFnr"
                            )
                        ),
                        LocalDateTime.now()
                    )
                )

            }.let {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get("/behandlinger/$behandlingId") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val behandling: DetaljertBehandling = it.body()
                assertNotNull(behandling.id)
                assertEquals("innsender", behandling.innsender)
                assertEquals(VurderingsResultat.OPPFYLT, behandling.gyldighetsproeving?.resultat)

            }
            client.post("/behandlinger/$behandlingId/hendelser/vedtak/FATTET") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    VedtakHendelse(
                        12L,
                        "Saksbehandlier",
                        Tidspunkt.now(),
                        null,
                        null
                    )
                )
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get("/behandlinger/$behandlingId") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val behandling: DetaljertBehandling = it.body()
                assertNotNull(behandling.id)
                assertEquals("FATTET_VEDTAK", behandling.status?.name)

            }

            client.get("/oppgaver") {
                addAuthAttesterer()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val oppgaver: OppgaveListeDto = it.body()
                assertEquals(1, oppgaver.oppgaver.size)
                assertEquals(behandlingId, oppgaver.oppgaver.first().behandlingId)
            }

            client.post("/behandlinger/revurdering/pdlhendelse/doedshendelse") {
                addAuthServiceBruker()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Doedshendelse("søker", LocalDate.now()))
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            // For denne skal det ikke opprettes en revurdring siden det allerede eksisterer en revurdering for dette fnr med samme aarsakstype; doedshendelse
            client.post("/behandlinger/revurdering/pdlhendelse/doedshendelse") {
                addAuthServiceBruker()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Doedshendelse("søker", LocalDate.now()))
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

        }

        beans.behandlingHendelser().nyHendelse.close()

        assertNotNull(behandlingOpprettet)
        val rapid = beans.rapidSingleton
        assertEquals(2, rapid.publiserteMeldinger.size)
        assertEquals(
            "BEHANDLING:OPPRETTET",
            objectMapper.readTree(rapid.publiserteMeldinger.first().verdi)["@event_name"].textValue()
        )
        assertEquals(
            "BEHANDLING:OPPRETTET",
            objectMapper.readTree(rapid.publiserteMeldinger[1].verdi)["@event_name"].textValue()
        )

        beans.datasourceBuilder().dataSource.connection.use {
            HendelseDao { it }.finnHendelserIBehandling(behandlingOpprettet!!).also { println(it) }
        }

        postgreSQLContainer.stop()
    }
}

val clientCredentialTokenMedKanSetteKildeRolle =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImVuLWFwcCIsIm9pZCI6ImVuLWFwcCIsIm5hbWUiOiJKb2huIERvZSIsImlhdCI6MTUxNjIzOTAyMiwiTkFWaWRlbnQiOiJTYWtzYmVoYW5kbGVyMDEiLCJyb2xlcyI6WyJrYW4tc2V0dGUta2lsZGUiXX0.2ftwnoZiUfUa_J6WUkqj_Wdugb0CnvVXsEs-JYnQw_g"
val saksbehandlerToken =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImF6dXJlLWlkIGZvciBzYWtzYmVoYW5kbGVyIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJOQVZpZGVudCI6IlNha3NiZWhhbmRsZXIwMSJ9.271mDij4YsO4Kk8w8AvX5BXxlEA8U-UAOtdG1Ix_kQY"
val attestererToken =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImF6dXJlLWlkIGZvciBzYWtzYmVoYW5kbGVyIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJOQVZpZGVudCI6IlNha3NiZWhhbmRsZXIwMSIsImdyb3VwcyI6WyIwYWYzOTU1Zi1kZjg1LTRlYjAtYjViMi00NWJmMmM4YWViOWUiLCI2M2Y0NmY3NC04NGE4LTRkMWMtODdhOC03ODUzMmFiM2FlNjAiXX0.YzF4IXwaolgOCODNwkEKn43iZbwHpQuSmQObQm0co-A"

fun HttpRequestBuilder.addAuthSaksbehandler() {
    header(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
}

fun HttpRequestBuilder.addAuthAttesterer() {
    header(HttpHeaders.Authorization, "Bearer $attestererToken")
}

fun HttpRequestBuilder.addAuthServiceBruker() {
    header(HttpHeaders.Authorization, "Bearer $clientCredentialTokenMedKanSetteKildeRolle")
}


class TestBeanFactory(
    private val jdbcUrl: String,
) : CommonFactory() {
    val rapidSingleton: TestProdusent<String, String> by lazy { TestProdusent() }
    override fun datasourceBuilder(): DataSourceBuilder = DataSourceBuilder(mapOf("DB_JDBC_URL" to jdbcUrl))
    override fun rapid(): KafkaProdusent<String, String> = rapidSingleton

}
