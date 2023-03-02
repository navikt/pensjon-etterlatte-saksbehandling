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
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingsBehov
import no.nav.etterlatte.behandling.FastsettVirkningstidspunktResponse
import no.nav.etterlatte.behandling.KommerBarnetTilgodeJson
import no.nav.etterlatte.behandling.ManueltOpphoerResponse
import no.nav.etterlatte.behandling.TilVilkaarsvurderingJson
import no.nav.etterlatte.behandling.VedtakHendelse
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerRequest
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.oppgave.OppgaveListeDto
import no.nav.etterlatte.sak.Sak
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
        val fnr = Foedselsnummer.of("08071272487").value
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
            application { module(beanFactory) }

            client.get("/saker/$fnr") {
                addAuthToken(tokenSaksbehandler)
            }.apply {
                assertEquals(HttpStatusCode.NotFound, status)
            }
            val sak: Sak = client.get("/personer/$fnr/saker/BARNEPENSJON") {
                addAuthToken(tokenSaksbehandler)
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
                assertEquals(HttpStatusCode.OK, it.status)
                UUID.fromString(it.body())
            }
            behandlingOpprettet = behandlingId

            client.get("/sak/1/behandlinger") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.let {
                assertEquals(HttpStatusCode.OK, it.status)
                it.body<BehandlingListe>()
            }.also {
                assertEquals(1, it.behandlinger.size)
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
            }.let {
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
                    Grunnlagsopplysning.Saksbehandler("Saksbehandler01", Tidspunkt.now().instant),
                    "En begrunnelse"
                )
                assertEquals(expected.dato, it.body<FastsettVirkningstidspunktResponse>().dato)
                assertEquals(expected.kilde.ident, it.body<FastsettVirkningstidspunktResponse>().kilde.ident)
                assertEquals(expected.begrunnelse, it.body<FastsettVirkningstidspunktResponse>().begrunnelse)
            }

            client.post("/api/behandling/$behandlingId/kommerbarnettilgode") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(KommerBarnetTilgodeJson(JaNeiVetIkke.JA, "begrunnelse"))
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get("/behandlinger/$behandlingId/vilkaarsvurder") {
                addAuthToken(tokenSaksbehandler)
            }.also {
                beanFactory.dataSource().connection.use {
                    val actual = BehandlingDao { it }.hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.OPPRETTET, actual.status)
                }

                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/behandlinger/$behandlingId/vilkaarsvurder") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(TilVilkaarsvurderingJson(VilkaarsvurderingUtfall.OPPFYLT))
            }.also {
                beanFactory.dataSource().connection.use {
                    val actual = BehandlingDao { it }.hentBehandling(behandlingId)!!
                    assertEquals(BehandlingStatus.VILKAARSVURDERT, actual.status)
                }

                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/behandlinger/$behandlingId/beregn") {
                addAuthToken(tokenSaksbehandler)
            }.also {
                beanFactory.dataSource().connection.use {
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
                beanFactory.dataSource().connection.use {
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
                beanFactory.dataSource().connection.use {
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
                addAuthToken(tokenServiceUser)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Doedshendelse("søker", LocalDate.now(), Endringstype.OPPRETTET))
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/grunnlagsendringshendelse/doedshendelse") {
                addAuthToken(tokenServiceUser)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Doedshendelse("søker", LocalDate.now(), Endringstype.OPPRETTET))
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.post("/grunnlagsendringshendelse/utflyttingshendelse") {
                addAuthToken(tokenServiceUser)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    UtflyttingsHendelse(
                        fnr = fnr,
                        tilflyttingsLand = null,
                        tilflyttingsstedIUtlandet = null,
                        utflyttingsdato = null,
                        endringstype = Endringstype.OPPRETTET
                    )
                )
            }

            client.post("/grunnlagsendringshendelse/forelderbarnrelasjonhendelse") {
                addAuthToken(tokenServiceUser)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    ForelderBarnRelasjonHendelse(
                        fnr = fnr,
                        relatertPersonsIdent = null,
                        relatertPersonsRolle = "",
                        minRolleForPerson = "",
                        relatertPersonUtenFolkeregisteridentifikator = null,
                        endringstype = Endringstype.OPPRETTET
                    )
                )
            }

            client.post("/grunnlagsendringshendelse/adressebeskyttelse") {
                addAuthToken(tokenServiceUser)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Adressebeskyttelse(
                        fnr = fnr,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                        endringstype = Endringstype.OPPRETTET
                    )
                )
            }

            val manueltOpphoer = client.post("/api/behandlinger/${sak.id}/manueltopphoer") {
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
            }.body<ManueltOpphoerResponse>()

            client.get("/behandlinger/manueltopphoer?behandlingsid=${manueltOpphoer.behandlingId}") {
                addAuthToken(tokenSaksbehandler)
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val result = it.body<DetaljertBehandling>()
                assertEquals(sak.id, result.sak)
                assertEquals(BehandlingType.MANUELT_OPPHOER, result.behandlingType)
            }

            val behandlingIdNyFoerstegangsbehandling = client.post("/behandlinger/foerstegangsbehandling") {
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
                assertEquals(HttpStatusCode.OK, it.status)
                UUID.fromString(it.body())
            }

            client.post("api/behandling/$behandlingIdNyFoerstegangsbehandling/avbryt") {
                addAuthToken(tokenSaksbehandler)
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
            }

            client.get("/behandlinger/foerstegangsbehandling?behandlingsid=$behandlingIdNyFoerstegangsbehandling") {
                addAuthToken(tokenSaksbehandler)
            }.also {
                assertEquals(HttpStatusCode.OK, it.status)
                val result = it.body<DetaljertBehandling>()
                assertEquals(BehandlingStatus.AVBRUTT, result.status)
            }
        }

        beanFactory.behandlingHendelser().nyHendelse.close()

        kotlin.runCatching { sleep(2000) }
        assertNotNull(behandlingOpprettet)
        val rapid = beanFactory.rapidSingleton
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
        beanFactory.dataSource().connection.use {
            HendelseDao { it }.finnHendelserIBehandling(behandlingOpprettet!!).also { println(it) }
        }
    }
}