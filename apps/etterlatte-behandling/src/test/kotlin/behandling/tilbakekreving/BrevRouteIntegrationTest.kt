package no.nav.etterlatte.behandling.tilbakekreving

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.module
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BrevRouteIntegrationTest : BehandlingIntegrationTest() {
    private lateinit var behandlingDao: BehandlingDao
    private lateinit var sakSkrivDao: SakSkrivDao

    private val brevApiKlientMock: BrevApiKlient =
        mockk {
            coEvery { opprettVedtaksbrev(any(), any(), any()) } returns mockk()
        }

    @BeforeEach
    fun setUp() {
        sakSkrivDao = applicationContext.sakSkrivDao
        behandlingDao = applicationContext.behandlingDao
    }

    @BeforeAll
    fun start() {
        startServer(brevApiKlient = brevApiKlientMock)
        val user = mockk<SaksbehandlerMedEnheterOgRoller>(relaxed = true)
        val saksbehandlerMedRoller =
            mockk<SaksbehandlerMedRoller> {
                every { harRolleStrengtFortrolig() } returns false
                every { harRolleEgenAnsatt() } returns false
            }
        every { user.saksbehandlerMedRoller } returns saksbehandlerMedRoller
        every { user.name() } returns "User"
        every { user.enheter() } returns listOf(Enheter.defaultEnhet.enhetNr)

        nyKontekstMedBrukerOgDatabase(user, applicationContext.dataSource)
    }

    @AfterAll
    fun afterAllTests() {
        afterAll()
    }

    @AfterEach
    fun afterEachTest() {
        resetDatabase()
    }

    @Test
    fun `skal opprette vedtaksbrev for behandling`() {
        val behandling =
            inTransaction {
                val sak =
                    sakSkrivDao.opprettSak(
                        SOEKER_FOEDSELSNUMMER.value,
                        SakType.OMSTILLINGSSTOENAD,
                        Enheter.defaultEnhet.enhetNr,
                    )
                opprettBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, sak.id)
                    .also { behandlingDao.opprettBehandling(it) }
            }

        withTestApplication { client ->
            val response =
                client.post("/api/behandling/brev/${behandling.id}/vedtak?sakId=${behandling.sakId}") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            response.status shouldBe HttpStatusCode.Created

            coVerify { brevApiKlientMock.opprettVedtaksbrev(eq(behandling.id), any(), any()) }
        }
    }

    fun `skal tilbakestille vedtaksbrev`() {
        val behandling =
            inTransaction {
                val sak =
                    sakSkrivDao.opprettSak(
                        SOEKER_FOEDSELSNUMMER.value,
                        SakType.OMSTILLINGSSTOENAD,
                        Enheter.defaultEnhet.enhetNr,
                    )
                opprettBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, sak.id)
                    .also { behandlingDao.opprettBehandling(it) }
            }

        withTestApplication { client ->
            val response =
                client.post("/api/behandling/brev/${behandling.id}/vedtak?sakId=${behandling.sakId}") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            response.status shouldBe HttpStatusCode.Created
        }
    }

    private fun withTestApplication(block: suspend (client: HttpClient) -> Unit) {
        testApplication {
            val client =
                runServerWithModule(mockOAuth2Server) {
                    module(applicationContext)
                }
            block(client)
        }
    }
}
