package no.nav.etterlatte.grunnlagsendring

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.asContextElement
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdHendelseBeriket
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdKilde
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdsType
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.INNSENDER_FOEDSELSNUMMER
import no.nav.etterlatte.mockSaksbehandler
import no.nav.etterlatte.module
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.soeker
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class GrunnlagsendringshendelseRouteTest : BehandlingIntegrationTest() {
    private lateinit var context: Context

    @BeforeEach
    fun start() {
        startServer()
        context = nyKontekstMedBrukerOgDatabase(mockSaksbehandler("Z123456"), applicationContext.dataSource)
    }

    @AfterEach
    fun afterEach() {
        afterAll()
    }

    @Test
    fun `Skal kunne håndtere en adressebeskyttelsesendring`() {
        val fnr = AVDOED_FOEDSELSNUMMER.value

        testApplication {
            val client =
                runServerWithModule(mockOAuth2Server) {
                    module(applicationContext)
                }

            val sak: Sak =
                client
                    .post("personer/saker/${SakType.BARNEPENSJON}") {
                        addAuthToken(tokenSaksbehandler)
                        contentType(ContentType.Application.Json)
                        setBody(FoedselsnummerDTO(fnr))
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }.let {
                        assertEquals(HttpStatusCode.OK, it.status)
                        it.body()
                    }
            Assertions.assertNotNull(sak.id)

            val behandlingId =
                client
                    .post("/behandlinger/opprettbehandling") {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(behandlingsBehov(sak))
                    }.let {
                        assertEquals(HttpStatusCode.OK, it.status)
                        UUID.fromString(it.body())
                    }

            client
                .get("/behandlinger/$behandlingId") {
                    addAuthToken(this@GrunnlagsendringshendelseRouteTest.systemBruker)
                }.let {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/grunnlagsendringshendelse/adressebeskyttelse") {
                    addAuthToken(this@GrunnlagsendringshendelseRouteTest.systemBruker)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        Adressebeskyttelse(
                            hendelseId = "1",
                            fnr = fnr,
                            endringstype = Endringstype.OPPRETTET,
                        ),
                    )
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/grunnlagsendringshendelse/adressebeskyttelse") {
                    addAuthToken(this@GrunnlagsendringshendelseRouteTest.systemBruker)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        Adressebeskyttelse(
                            hendelseId = "1",
                            fnr = fnr,
                            endringstype = Endringstype.OPPRETTET,
                        ),
                    )
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }

            client
                .post("/grunnlagsendringshendelse/adressebeskyttelse") {
                    addAuthToken(this@GrunnlagsendringshendelseRouteTest.systemBruker)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        Adressebeskyttelse(
                            hendelseId = "1",
                            fnr = fnr,
                            endringstype = Endringstype.OPPRETTET,
                        ),
                    )
                }.also {
                    assertEquals(HttpStatusCode.OK, it.status)
                }
        }
    }

    @Test
    fun `skal opprette hendelse om institusjonsopphold`() {
        withTestApplication(context) { client ->
            val sak: Sak =
                client
                    .postJson("/personer/saker/${SakType.OMSTILLINGSSTOENAD}") {
                        setBody(FoedselsnummerDTO(soeker))
                    }.apply {
                        assertEquals(HttpStatusCode.OK, status)
                    }.body()

            client
                .postJson("/behandlinger/opprettbehandling") {
                    setBody(behandlingsBehov(sak))
                }.also { assertEquals(HttpStatusCode.OK, it.status) }

            client
                .postJson("/grunnlagsendringshendelse/institusjonsopphold") {
                    setBody(institusjonsopphold())
                }.also { assertEquals(HttpStatusCode.OK, it.status) }

            val hendelserResponse =
                client
                    .get("/api/grunnlagsendringshendelse/${sak.id}/institusjon") {
                    }.also { assertEquals(HttpStatusCode.OK, it.status) }

            val hendelser: List<Grunnlagsendringshendelse> = hendelserResponse.body()
            val grunnlagsendringshendelse = hendelser.single()

            grunnlagsendringshendelse.status shouldBe GrunnlagsendringStatus.SJEKKET_AV_JOBB
            grunnlagsendringshendelse.sakId shouldBe sak.id
            grunnlagsendringshendelse.type shouldBe GrunnlagsendringsType.INSTITUSJONSOPPHOLD
            grunnlagsendringshendelse.behandlingId shouldBe null

            val oppgaver = client.get("/oppgaver/sak/${sak.id}/oppgaver").body<List<OppgaveIntern>>()
            val oppgave =
                oppgaver
                    .single { it.type == OppgaveType.VURDER_KONSEKVENS }

            oppgave.referanse shouldBe grunnlagsendringshendelse.id.toString()
            oppgave.status shouldBe Status.NY
            oppgave.saksbehandler shouldBe null
        }
    }

    private fun behandlingsBehov(sak: Sak): BehandlingsBehov =
        BehandlingsBehov(
            sak.id,
            Persongalleri(
                soeker,
                INNSENDER_FOEDSELSNUMMER.value,
                emptyList(),
                emptyList(),
                emptyList(),
            ),
            Tidspunkt.now().toLocalDatetimeUTC().toString(),
        )

    private fun withTestApplication(
        context: Context,
        block: suspend (client: HttpClient) -> Unit,
    ) {
        testApplication(Kontekst.asContextElement(context)) {
            val client =
                runServerWithModule(mockOAuth2Server) {
                    module(applicationContext)
                }
            block(client)
        }
    }

    private fun institusjonsopphold(): InstitusjonsoppholdHendelseBeriket =
        InstitusjonsoppholdHendelseBeriket(
            hendelseId = Random.nextLong(),
            oppholdId = Random.nextLong(),
            norskident = soeker,
            institusjonsoppholdsType = InstitusjonsoppholdsType.INNMELDING,
            institusjonsoppholdKilde = InstitusjonsoppholdKilde.INST,
            institusjonsType = "fengsel",
            startdato = LocalDate.now().minusDays(6),
            faktiskSluttdato = null,
            forventetSluttdato = LocalDate.now().plusMonths(18),
            institusjonsnavn = "Feng Shel fengsel",
            organisasjonsnummer = "957838938",
        )

    private suspend fun HttpClient.postJson(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse =
        post {
            addAuthToken(this@GrunnlagsendringshendelseRouteTest.systemBruker)
            contentType(ContentType.Application.Json)
            url(urlString)
            block()
        }

    private suspend fun HttpClient.get(
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse =
        get {
            url(urlString)
            block()
            if (headers[HttpHeaders.Authorization] == null) {
                addAuthToken(this@GrunnlagsendringshendelseRouteTest.systemBruker)
            }
        }
}
