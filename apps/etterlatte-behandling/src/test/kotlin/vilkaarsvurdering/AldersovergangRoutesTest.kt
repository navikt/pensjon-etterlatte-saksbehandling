package no.nav.etterlatte.vilkaarsvurdering

import io.kotest.matchers.shouldBe
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
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.attachMockContext
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSystembrukerToken
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.mockSaksbehandler
import no.nav.etterlatte.mockedSakTilgangDao
import no.nav.etterlatte.vilkaarsvurdering.service.AldersovergangService
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AldersovergangRoutesTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val aldersovergangService = mockk<AldersovergangService>()

    private val behandlingId = UUID.randomUUID()
    private val behandlingDetSkalKopieresFraId = UUID.randomUUID()

    @BeforeAll
    fun before() {
        mockOAuth2Server.startRandomPort()
        Kontekst.set(Context(saksbehandler, DatabaseContext(mockk()), mockedSakTilgangDao(), null))
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    private val token: String by lazy { mockOAuth2Server.issueSystembrukerToken() }
    private val saksbehandler = mockSaksbehandler("User")

    @Test
    fun `skal delegere til aldersovergangservice`() {
        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContext(saksbehandler)
                aldersovergang(aldersovergangService)
            }

            val vilkaarsvurdering =
                mockk<Vilkaarsvurdering> {
                    every { id } returns UUID.randomUUID()
                }
            coEvery {
                aldersovergangService.behandleOpphoerAldersovergang(behandlingId, behandlingDetSkalKopieresFraId, any())
            } returns vilkaarsvurdering

            val response =
                client.post("/api/vilkaarsvurdering/aldersovergang/$behandlingId?fra=$behandlingDetSkalKopieresFraId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.OK

            coVerify { aldersovergangService.behandleOpphoerAldersovergang(behandlingId, behandlingDetSkalKopieresFraId, any()) }
            verify { vilkaarsvurdering.id }
        }
    }
}
