package no.nav.etterlatte.brev.batch

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.brev.BrevBatchOversikt
import no.nav.etterlatte.brev.BrevBatchStatus
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.token.issueSystembrukerToken
import no.nav.etterlatte.libs.common.toJson
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class BrevBatchRouteTest {
    private val dao = mockk<BrevBatchDao>()
    private val service =
        BrevBatchService(dao, BrevBatchConfig { mapOf("BREV_BATCH_AKTIVERT" to "true", "BREV_BATCH_TILLATT_INNSENDER" to "godkjent") })

    @Test
    fun `API beskytter opprettelse og begge leseruter med allowlist og eierskap`() {
        medServer { server ->
            server.start()
            testApplication {
                val client = runServer(server) { brevBatchRoute(service) }
                val id = UUID.randomUUID()
                for (token in listOf(server.issueSaksbehandlerToken(navIdent = "godkjent"), server.issueSystembrukerToken("annen"))) {
                    assertEquals(
                        HttpStatusCode.Forbidden,
                        client
                            .post("/api/brev/batcher") {
                                bearerAuth(token)
                                header("Idempotency-Key", UUID.randomUUID())
                                setBody(batchRequest().toJson())
                            }.status,
                    )
                    assertEquals(HttpStatusCode.Forbidden, client.get("/api/brev/batcher/$id") { bearerAuth(token) }.status)
                    assertEquals(HttpStatusCode.Forbidden, client.get("/api/brev/batcher/$id/saker") { bearerAuth(token) }.status)
                }
                every { dao.hent(id, "godkjent") } returns null
                every { dao.hentSaker(id, "godkjent", 0, 100) } returns null
                assertEquals(
                    HttpStatusCode.NotFound,
                    client.get("/api/brev/batcher/$id") { bearerAuth(server.issueSystembrukerToken("godkjent")) }.status,
                )
                assertEquals(
                    HttpStatusCode.NotFound,
                    client
                        .get("/api/brev/batcher/$id/saker") {
                            bearerAuth(server.issueSystembrukerToken("godkjent"))
                        }.status,
                )
            }
        }
    }

    @Test
    fun `API validerer actual body stoerrelse noekkel og paginering foer lagring`() {
        medServer { server ->
            server.start()
            testApplication {
                val client = runServer(server) { brevBatchRoute(service) }
                val token = server.issueSystembrukerToken("godkjent")
                assertEquals(
                    HttpStatusCode.PayloadTooLarge,
                    client
                        .post("/api/brev/batcher") {
                            bearerAuth(token)
                            header("Idempotency-Key", UUID.randomUUID())
                            contentType(ContentType.Application.Json)
                            setBody(" ".repeat(BrevBatchService.MAKS_REQUEST_BYTES + 1))
                        }.status,
                )
                assertEquals(
                    HttpStatusCode.BadRequest,
                    client
                        .post("/api/brev/batcher") {
                            bearerAuth(token)
                            setBody(batchRequest().toJson())
                        }.status,
                )
                assertEquals(
                    HttpStatusCode.BadRequest,
                    client.get("/api/brev/batcher/${UUID.randomUUID()}/saker?limit=101") { bearerAuth(token) }.status,
                )
                assertEquals(
                    HttpStatusCode.BadRequest,
                    client.get("/api/brev/batcher/${UUID.randomUUID()}/saker?offset=abc") { bearerAuth(token) }.status,
                )
                verify(exactly = 0) { dao.opprett(any(), any(), any(), any()) }
            }
        }
    }

    @Test
    fun `gyldig POST returnerer Accepted med statusadresse og bevart tekst`() {
        medServer { server ->
            server.start()
            val id = UUID.randomUUID()
            val request = batchRequest(listOf(2, 1, 2))
            val normalisert = BrevBatchService.normaliser(request)
            every { dao.opprett("godkjent", any(), normalisert, any()) } returns id
            every { dao.hent(id, "godkjent") } returns BrevBatchOversikt(id, BrevBatchStatus.UNDER_ARBEID, 2, 0, 0, 0, 2, Instant.now())
            testApplication {
                val client = runServer(server) { brevBatchRoute(service) }
                val response =
                    client.post("/api/brev/batcher") {
                        bearerAuth(server.issueSystembrukerToken("godkjent"))
                        header("Idempotency-Key", UUID.randomUUID())
                        contentType(ContentType.Application.Json)
                        setBody(request.toJson())
                    }
                assertEquals(HttpStatusCode.Accepted, response.status)
                assertEquals("/api/brev/batcher/$id", response.headers["Location"])
            }
        }
    }

    private fun medServer(block: (MockOAuth2Server) -> Unit) {
        val server = MockOAuth2Server()
        try {
            block(server)
        } finally {
            server.shutdown()
        }
    }
}
