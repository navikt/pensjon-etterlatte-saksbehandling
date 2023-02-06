package no.nav.etterlatte.itest

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.serialization.jackson.jackson
import io.ktor.server.auth.Authentication
import io.ktor.server.testing.testApplication
import no.nav.etterlatte.CommonFactory
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingsBehov
import no.nav.etterlatte.behandling.FastsettVirkningstidspunktResponse
import no.nav.etterlatte.behandling.KommerBarnetTilgodeJson
import no.nav.etterlatte.behandling.ManueltOpphoerResponse
import no.nav.etterlatte.behandling.TilVilkaarsvurderingJson
import no.nav.etterlatte.behandling.VedtakHendelse
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.klienter.GrunnlagKlientTest
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlientTest
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.LeaderElection
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerRequest
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.module
import no.nav.etterlatte.oppgave.OppgaveListeDto
import no.nav.etterlatte.sak.Sak
import no.nav.etterlatte.sikkerhet.tokenTestSupportAcceptsAllTokens
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.lang.Thread.sleep
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

class ApplicationTest {
    @Test
    fun verdikjedetest() {
        val fnr = "123"
        val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:12")
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        var behandlingOpprettet: UUID? = null
        val beans = TestBeanFactory(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        )

        testApplication {
            val client = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
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
            val sak: Sak = client.get("/personer/$fnr/saker/BARNEPENSJON") {
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
                assertEquals(SakType.BARNEPENSJON, lestSak.sakType)
            }

            val behandlingId = client.post("/behandlinger/foerstegangsbehandling") {
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

            client.post("/api/behandling/$behandlingId/virkningstidspunkt") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    mapOf("dato" to "2022-02-01T01:00:00.000Z", "begrunnelse" to "En begrunnelse")
                )
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)

                val expected = FastsettVirkningstidspunktResponse(
                    YearMonth.of(2022, 2),
                    Grunnlagsopplysning.Saksbehandler("Saksbehandler01", Instant.now()),
                    "En begrunnelse"
                )
                assertEquals(expected.dato, it.body<FastsettVirkningstidspunktResponse>().dato)
                assertEquals(expected.kilde.ident, it.body<FastsettVirkningstidspunktResponse>().kilde.ident)
                assertEquals(expected.begrunnelse, it.body<FastsettVirkningstidspunktResponse>().begrunnelse)
            }

            client.post("/api/behandling/$behandlingId/kommerbarnettilgode") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(KommerBarnetTilgodeJson(JaNeiVetIkke.JA, "begrunnelse"))
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get("/behandlinger/$behandlingId/vilkaarsvurder") {
                addAuthSaksbehandler()
            }.also {
                beans.dataSource().connection.use {
                    val actual = BehandlingDao { it }.hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.OPPRETTET, actual.status)
                }

                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/behandlinger/$behandlingId/vilkaarsvurder") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(TilVilkaarsvurderingJson(VilkaarsvurderingUtfall.OPPFYLT))
            }.also {
                beans.dataSource().connection.use {
                    val actual = BehandlingDao { it }.hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.VILKAARSVURDERT, actual.status)
                }

                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/behandlinger/$behandlingId/beregn") {
                addAuthSaksbehandler()
            }.also {
                beans.dataSource().connection.use {
                    val actual = BehandlingDao { it }.hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.BEREGNET, actual.status)
                }

                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/behandlinger/$behandlingId/fatteVedtak") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(VedtakHendelse(123L, "saksb", Tidspunkt.now(), null, null))
            }.also {
                beans.dataSource().connection.use {
                    val actual = BehandlingDao { it }.hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.FATTET_VEDTAK, actual.status)
                }

                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get("/api/oppgaver") {
                addAuthAttesterer()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val oppgaver: OppgaveListeDto = it.body()
                assertEquals(1, oppgaver.oppgaver.size)
                assertEquals(behandlingId, oppgaver.oppgaver.first().behandlingId)
            }

            client.post("/behandlinger/$behandlingId/attester") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(VedtakHendelse(123L, "saksb", Tidspunkt.now(), null, null))
            }.also {
                beans.dataSource().connection.use {
                    val actual = BehandlingDao { it }.hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.ATTESTERT, actual.status)
                }

                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/behandlinger/$behandlingId/iverksett") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    VedtakHendelse(
                        12L,
                        null,
                        Tidspunkt.now(),
                        null,
                        null
                    )
                )
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get("/behandlinger/$behandlingId") {
                addAuthSaksbehandler()
            }.also {
                val behandling = it.body<DetaljertBehandling>()

                assertEquals(HttpStatusCode.OK, it.status)
                assertEquals(BehandlingStatus.IVERKSATT, behandling.status)
            }

            client.post("/grunnlagsendringshendelse/doedshendelse") {
                addAuthServiceBruker()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Doedshendelse("søker", LocalDate.now(), Endringstype.OPPRETTET))
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/grunnlagsendringshendelse/doedshendelse") {
                addAuthServiceBruker()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Doedshendelse("søker", LocalDate.now(), Endringstype.OPPRETTET))
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/grunnlagsendringshendelse/utflyttingshendelse") {
                addAuthServiceBruker()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    UtflyttingsHendelse(
                        fnr = "søker",
                        tilflyttingsLand = null,
                        tilflyttingsstedIUtlandet = null,
                        utflyttingsdato = null,
                        endringstype = Endringstype.OPPRETTET
                    )
                )
            }

            client.post("/grunnlagsendringshendelse/forelderbarnrelasjonhendelse") {
                addAuthServiceBruker()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    ForelderBarnRelasjonHendelse(
                        fnr = "søker",
                        relatertPersonsIdent = null,
                        relatertPersonsRolle = "",
                        minRolleForPerson = "",
                        relatertPersonUtenFolkeregisteridentifikator = null,
                        endringstype = Endringstype.OPPRETTET
                    )
                )
            }

            val manueltOpphoer = client.post("/api/behandlinger/${sak.id}/manueltopphoer") {
                addAuthSaksbehandler()
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    ManueltOpphoerRequest(
                        sak = sak.id,
                        opphoerAarsaker = listOf(
                            ManueltOpphoerAarsak.SOESKEN_DOED,
                            ManueltOpphoerAarsak.UTFLYTTING_FRA_NORGE
                        ),
                        fritekstAarsak = "kunne ikke behandles manuelt"
                    )
                )
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }.body<ManueltOpphoerResponse>()

            client.get("/behandlinger/manueltopphoer?behandlingsid=${manueltOpphoer.behandlingId}") {
                addAuthSaksbehandler()
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val result = it.body<DetaljertBehandling>()
                assertEquals(sak.id, result.sak)
                assertEquals(BehandlingType.MANUELT_OPPHOER, result.behandlingType)
            }

            val behandlingIdNyFoerstegangsbehandling = client.post("/behandlinger/foerstegangsbehandling") {
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

            client.post("api/behandling/$behandlingIdNyFoerstegangsbehandling/avbryt") {
                addAuthSaksbehandler()
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get("/behandlinger/foerstegangsbehandling?behandlingsid=$behandlingIdNyFoerstegangsbehandling") {
                addAuthSaksbehandler()
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val result = it.body<DetaljertBehandling>()
                assertEquals(BehandlingStatus.AVBRUTT, result.status)
            }
        }

        beans.behandlingHendelser().nyHendelse.close()

        kotlin.runCatching { sleep(2000) }
        assertNotNull(behandlingOpprettet)
        val rapid = beans.rapidSingleton
        assertEquals(5, rapid.publiserteMeldinger.size)
        assertEquals(
            "BEHANDLING:OPPRETTET",
            objectMapper.readTree(rapid.publiserteMeldinger.first().verdi)["@event_name"].textValue()
        )
        assertEquals(
            "BEHANDLING:GYLDIG_FREMSATT",
            objectMapper.readTree(rapid.publiserteMeldinger[1].verdi)["@event_name"].textValue()
        )
        assertEquals(
            "BEHANDLING:OPPRETTET",
            objectMapper.readTree(rapid.publiserteMeldinger[2].verdi)["@event_name"].textValue()
        )
        assertEquals(
            "BEHANDLING:OPPRETTET",
            objectMapper.readTree(rapid.publiserteMeldinger[3].verdi)["@event_name"].textValue()

        )
        assertEquals(
            "BEHANDLING:AVBRUTT",
            objectMapper.readTree(rapid.publiserteMeldinger.last().verdi)["@event_name"].textValue()
        )

        beans.dataSource().connection.use {
            HendelseDao { it }.finnHendelserIBehandling(behandlingOpprettet!!).also { println(it) }
        }

        postgreSQLContainer.stop()
    }
}

val clientCredentialTokenMedKanSetteKildeRolle =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImVuLWFwcCIsIm9pZCI6ImVuLWFwcCIsIm5hbWUiOiJKb2huIERvZSIsImlhdCI6MTUxNjIzOTAyMiwiTkFWaWRlbnQiOiJTYWtzYmVoYW5kbGVyMDEiLCJyb2xlcyI6WyJrYW4tc2V0dGUta2lsZGUiXX0.2ftwnoZiUfUa_J6WUkqj_Wdugb0CnvVXsEs-JYnQw_g" // ktlint-disable max-line-length
val saksbehandlerToken =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImF6dXJlLWlkIGZvciBzYWtzYmVoYW5kbGVyIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJOQVZpZGVudCI6IlNha3NiZWhhbmRsZXIwMSJ9.271mDij4YsO4Kk8w8AvX5BXxlEA8U-UAOtdG1Ix_kQY" // ktlint-disable max-line-length
val attestererToken =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImF6dXJlLWlkIGZvciBzYWtzYmVoYW5kbGVyIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJOQVZpZGVudCI6IlNha3NiZWhhbmRsZXIwMSIsImdyb3VwcyI6WyIwYWYzOTU1Zi1kZjg1LTRlYjAtYjViMi00NWJmMmM4YWViOWUiLCI2M2Y0NmY3NC04NGE4LTRkMWMtODdhOC03ODUzMmFiM2FlNjAiXX0.YzF4IXwaolgOCODNwkEKn43iZbwHpQuSmQObQm0co-A" // ktlint-disable max-line-length

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
    private val username: String,
    private val password: String
) : CommonFactory() {

    val rapidSingleton: TestProdusent<String, String> by lazy { TestProdusent() }
    override fun dataSource(): DataSource =
        DataSourceBuilder.createDataSource(jdbcUrl, username, password)

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
                JacksonConverter(no.nav.etterlatte.libs.common.objectMapper)
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
                JacksonConverter(no.nav.etterlatte.libs.common.objectMapper)
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