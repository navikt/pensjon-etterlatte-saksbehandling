package no.nav.etterlatte.grunnlagsendring.doedshendelse

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.GosysOppgaveKlientTest
import no.nav.etterlatte.PdltjenesterKlientTest
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.attachMockContext
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.token.issueSystembrukerToken
import no.nav.etterlatte.libs.common.behandling.DoedshendelseBrevDistribuert
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class DoedshendelseRouteTest(
    val dataSource: DataSource,
) {
    private val featureToggleService: FeatureToggleService = DummyFeatureToggleService()
    private val mockOAuth2Server = MockOAuth2Server()
    private val pdlTjenesterKlient = spyk<PdltjenesterKlientTest>()
    private val gosysOppgaveKlient = spyk<GosysOppgaveKlientTest>()
    private val doedshendelseDao: DoedshendelseDao = DoedshendelseDao(ConnectionAutoclosingTest(dataSource))
    private val ukjentBeroertDao: UkjentBeroertDao = UkjentBeroertDao(ConnectionAutoclosingTest(dataSource))

    private val service =
        DoedshendelseService(
            doedshendelseDao = doedshendelseDao,
            pdlTjenesterKlient = pdlTjenesterKlient,
            featureToggleService = featureToggleService,
            gosysOppgaveKlient = gosysOppgaveKlient,
            ukjentBeroertDao = ukjentBeroertDao,
        )

    @BeforeAll
    fun before() {
        mockOAuth2Server.startRandomPort()
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE doedshendelse CASCADE;").execute()
        }
        clearAllMocks()
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `Skal få 404 hvis ikke systembruker`() {
        val avdoedFnr = "12345678902"
        val doedshendelseInternal =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = avdoedFnr,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
                endringstype = Endringstype.OPPRETTET,
            )

        doedshendelseDao.opprettDoedshendelse(doedshendelseInternal)
        val sakId = sakId1
        doedshendelseDao.oppdaterDoedshendelse(doedshendelseInternal.copy(sakId = sakId))
        val brevId = 12314L
        withTestApplication { client ->
            client
                .post("/doedshendelse/brevdistribuert") {
                    header(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
                    contentType(ContentType.Application.Json)
                    setBody(DoedshendelseBrevDistribuert(sakId, brevId))
                }.also {
                    Assertions.assertEquals(HttpStatusCode.NotFound, it.status)
                }
        }
        val hentDoedshendelserForPerson = doedshendelseDao.hentDoedshendelserForPerson(avdoedFnr)
        hentDoedshendelserForPerson.size shouldBe 1
        hentDoedshendelserForPerson[0].brevId shouldBe null
    }

    @Test
    fun `Sjekk at vi kan oppdatere doedshendelse`() {
        val avdoedFnr = "12345678902"
        val doedshendelseInternal =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = avdoedFnr,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
                endringstype = Endringstype.OPPRETTET,
            )

        doedshendelseDao.opprettDoedshendelse(doedshendelseInternal)
        val sakId = sakId1
        doedshendelseDao.oppdaterDoedshendelse(doedshendelseInternal.copy(sakId = sakId))
        val brevId = 12314L
        withTestApplication { client ->
            client
                .post("/doedshendelse/brevdistribuert") {
                    header(HttpHeaders.Authorization, "Bearer $systemBruker")
                    contentType(ContentType.Application.Json)
                    setBody(DoedshendelseBrevDistribuert(sakId, brevId))
                }.also {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                }
        }
        val hentDoedshendelserForPerson = doedshendelseDao.hentDoedshendelserForPerson(avdoedFnr)
        hentDoedshendelserForPerson.size shouldBe 1
        hentDoedshendelserForPerson[0].brevId shouldBe brevId
    }

    private fun withTestApplication(block: suspend (client: HttpClient) -> Unit) {
        val systembruker = mockk<SystemUser> { every { name() } returns "name" }
        testApplication {
            val client =
                runServer(mockOAuth2Server) {
                    attachMockContext(systembruker)
                    doedshendelseRoute(service)
                }
            block(client)
        }
    }

    private val systemBruker: String by lazy { mockOAuth2Server.issueSystembrukerToken() }
    private val saksbehandlerToken: String by lazy { mockOAuth2Server.issueSaksbehandlerToken() }
}
