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
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingsBehov
import no.nav.etterlatte.behandling.FastsettVirkningstidspunktResponse
import no.nav.etterlatte.behandling.ManueltOpphoerResponse
import no.nav.etterlatte.behandling.VedtakHendelse
import no.nav.etterlatte.behandling.VurderingMedBegrunnelseJson
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerAarsak
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerRequest
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.funksjonsbrytere.FellesFeatureToggle
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.oppgave.OppgaveListeDto
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.lang.Thread.sleep
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTest : BehandlingIntegrationTest() {

    @BeforeAll
    fun start() = startServer()

    @AfterAll
    fun shutdown() = afterAll()

    @Test
    fun verdikjedetest() {
        val fnr = Folkeregisteridentifikator.of("08071272487").value
        var behandlingOpprettet: UUID? = null

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val client = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
            }
            application { module(applicationContext) }

            assertTrue(applicationContext.featureToggleService.isEnabled(FellesFeatureToggle.NoOperationToggle, false))

            client.get("/saker/1") {
                addAuthToken(tokenSaksbehandler)
            }.apply {
                assertEquals(HttpStatusCode.NotFound, status)
            }
            val sak: Sak = client.post("/personer/saker/${SakType.BARNEPENSJON}") {
                addAuthToken(tokenSaksbehandler)
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
            }.body()

            client.get("/saker/${sak.id}") {
                addAuthToken(tokenSaksbehandler)
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val lestSak: Sak = it.body()
                assertEquals(fnr, lestSak.ident)
                assertEquals(SakType.BARNEPENSJON, lestSak.sakType)
            }

            val behandlingId = client.post("/behandlinger/foerstegangsbehandling") {
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    BehandlingsBehov(
                        1,
                        Persongalleri("søker", "innsender", emptyList(), emptyList(), emptyList()),
                        Tidspunkt.now().toLocalDatetimeUTC().toString()
                    )
                )
            }.let {
                assertEquals(HttpStatusCode.OK, it.status)
                UUID.fromString(it.body())
            }
            behandlingOpprettet = behandlingId

            client.get("/behandlinger/$behandlingId") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                it.body<DetaljertBehandling>()
            }

            client.post("/behandlinger/$behandlingId/gyldigfremsatt") {
                addAuthToken(tokenSaksbehandler)
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
                        Tidspunkt.now().toLocalDatetimeUTC()
                    )
                )
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get("/behandlinger/$behandlingId") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val behandling: DetaljertBehandling = it.body()
                assertNotNull(behandling.id)
                assertEquals("innsender", behandling.innsender)
                assertEquals(VurderingsResultat.OPPFYLT, behandling.gyldighetsproeving?.resultat)
            }

            client.post("/api/behandling/$behandlingId/virkningstidspunkt") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    mapOf("dato" to "2022-02-01T01:00:00.000Z", "begrunnelse" to "En begrunnelse")
                )
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)

                val expected = FastsettVirkningstidspunktResponse(
                    YearMonth.of(2022, 2),
                    Grunnlagsopplysning.Saksbehandler.create("Saksbehandler01"),
                    "En begrunnelse"
                )
                assertEquals(expected.dato, it.body<FastsettVirkningstidspunktResponse>().dato)
                assertEquals(expected.kilde.ident, it.body<FastsettVirkningstidspunktResponse>().kilde.ident)
                assertEquals(expected.begrunnelse, it.body<FastsettVirkningstidspunktResponse>().begrunnelse)
            }

            client.post("/api/behandling/$behandlingId/kommerbarnettilgode") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(VurderingMedBegrunnelseJson(JaNei.JA, "begrunnelse"))
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get("/behandlinger/$behandlingId/vilkaarsvurder") {
                addAuthToken(tokenSaksbehandler)
            }.also {
                applicationContext.dataSource.connection.use {
                    val actual = BehandlingDao { it }.hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.OPPRETTET, actual.status)
                }

                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/behandlinger/$behandlingId/vilkaarsvurder") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("{}")
            }.also {
                applicationContext.dataSource.connection.use {
                    val actual = BehandlingDao { it }.hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.VILKAARSVURDERT, actual.status)
                }

                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/behandlinger/$behandlingId/beregn") {
                addAuthToken(tokenSaksbehandler)
            }.also {
                applicationContext.dataSource.connection.use {
                    val actual = BehandlingDao { it }.hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.BEREGNET, actual.status)
                }

                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/behandlinger/$behandlingId/fatteVedtak") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(VedtakHendelse(123L, "saksb", Tidspunkt.now(), null, null))
            }.also {
                applicationContext.dataSource.connection.use {
                    val actual = BehandlingDao { it }.hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.FATTET_VEDTAK, actual.status)
                }

                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get("/api/oppgaver") {
                addAuthToken(tokenAttestant)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val oppgaver: OppgaveListeDto = it.body()
                assertEquals(1, oppgaver.oppgaver.size)
                assertEquals(behandlingId, oppgaver.oppgaver.first().behandlingId)
            }

            client.post("/behandlinger/$behandlingId/attester") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(VedtakHendelse(123L, "saksb", Tidspunkt.now(), null, null))
            }.also {
                applicationContext.dataSource.connection.use {
                    val actual = BehandlingDao { it }.hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.ATTESTERT, actual.status)
                }

                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/behandlinger/$behandlingId/iverksett") {
                addAuthToken(tokenSaksbehandler)
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
                addAuthToken(tokenSaksbehandler)
            }.also {
                val behandling = it.body<DetaljertBehandling>()

                assertEquals(HttpStatusCode.OK, it.status)
                assertEquals(BehandlingStatus.IVERKSATT, behandling.status)
            }

            client.post("/grunnlagsendringshendelse/doedshendelse") {
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Doedshendelse("1", Endringstype.OPPRETTET, fnr, LocalDate.now()))
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/grunnlagsendringshendelse/doedshendelse") {
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Doedshendelse("1", Endringstype.OPPRETTET, fnr, LocalDate.now()))
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/grunnlagsendringshendelse/utflyttingshendelse") {
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    UtflyttingsHendelse(
                        hendelseId = "1",
                        endringstype = Endringstype.OPPRETTET,
                        fnr = fnr,
                        tilflyttingsLand = null,
                        tilflyttingsstedIUtlandet = null,
                        utflyttingsdato = null
                    )
                )
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/grunnlagsendringshendelse/forelderbarnrelasjonhendelse") {
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    ForelderBarnRelasjonHendelse(
                        hendelseId = "1",
                        endringstype = Endringstype.OPPRETTET,
                        fnr = fnr,
                        relatertPersonsIdent = null,
                        relatertPersonsRolle = "",
                        minRolleForPerson = "",
                        relatertPersonUtenFolkeregisteridentifikator = null
                    )
                )
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            val behandlingIdNyFoerstegangsbehandling = client.post("/behandlinger/foerstegangsbehandling") {
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    BehandlingsBehov(
                        1,
                        Persongalleri(fnr, "innsender", emptyList(), emptyList(), emptyList()),
                        Tidspunkt.now().toLocalDatetimeUTC().toString()
                    )

                )
            }.let {
                assertEquals(HttpStatusCode.OK, it.status)
                UUID.fromString(it.body())
            }

            client.post("/api/behandling/$behandlingIdNyFoerstegangsbehandling/avbryt") {
                addAuthToken(tokenSaksbehandler)
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/api/behandlinger/${sak.id}/manueltopphoer") {
                addAuthToken(tokenSaksbehandler)
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
                it.body<ManueltOpphoerResponse>()
            }

            client.post("/grunnlagsendringshendelse/adressebeskyttelse") {
                addAuthToken(systemBruker)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Adressebeskyttelse(
                        hendelseId = "1",
                        endringstype = Endringstype.OPPRETTET,
                        fnr = fnr,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG
                    )
                )
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get("/behandlinger/$behandlingId") {
                addAuthToken(tokenSaksbehandler)
            }.also {
                assertEquals(HttpStatusCode.NotFound, it.status) // 404 pga adressebeskyttelse
            }
        }

        applicationContext.behandlingsHendelser.hendelserKanal.close()

        kotlin.runCatching { sleep(2000) }
        assertNotNull(behandlingOpprettet)
        val rapid = applicationContext.rapid as TestProdusent
        assertEquals(4, rapid.publiserteMeldinger.size)
        assertEquals(
            "BEHANDLING:OPPRETTET",
            objectMapper.readTree(rapid.publiserteMeldinger.first().verdi)["@event_name"].textValue()
        )
        assertEquals(
            "BEHANDLING:OPPRETTET",
            objectMapper.readTree(rapid.publiserteMeldinger[1].verdi)["@event_name"].textValue()
        )
        assertEquals(
            "BEHANDLING:AVBRUTT",
            objectMapper.readTree(rapid.publiserteMeldinger[2].verdi)["@event_name"].textValue()
        )
        assertEquals(
            "BEHANDLING:OPPRETTET",
            objectMapper.readTree(rapid.publiserteMeldinger[3].verdi)["@event_name"].textValue()
        )
        applicationContext.dataSource.connection.use {
            HendelseDao { it }.finnHendelserIBehandling(behandlingOpprettet!!).also { println(it) }
        }
    }
}