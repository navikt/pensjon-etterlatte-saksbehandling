package no.nav.etterlatte.tilbakekreving

import io.ktor.auth.Authentication
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.restModule
import no.nav.etterlatte.testsupport.readFile
import no.nav.etterlatte.testsupport.tokenTestSupportAcceptsAllTokens
import no.nav.etterlatte.tilbakekreving.config.ApplicationContext
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.BehandlingId
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagId
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagJaxb
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagMapper
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.SakId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class TilbakekrevingRoutesTest {

    private val applicationContext: ApplicationContext = mockk {
        every { tokenValidering } returns Authentication.Configuration::tokenTestSupportAcceptsAllTokens
        every { tilbakekrevingService } returns mockk {
            every { hentTilbakekreving(1) } returns
                    Tilbakekreving.MottattKravgrunnlag(
                        sakId = SakId(1),
                        behandlingId = BehandlingId(UUID.randomUUID(), Kravgrunnlag.UUID30("")),
                        kravgrunnlagId = KravgrunnlagId(1),
                        opprettet = Tidspunkt.now(),
                        kravgrunnlag = kravgrunnlagMock()
                    )
        }
    }

    @Test
    fun `skal hente kravgrunnlag`() {
        withTestApplication({ restModule(applicationContext) }) {
            handleRequest(HttpMethod.Get, "/tilbakekreving/1") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addAuthSaksbehandler()
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                // TODO skriv ferdig test
            }
        }
    }

    private fun kravgrunnlagMock() =
        readFile("/kravgrunnlag.xml").let {
            KravgrunnlagJaxb.toDetaljertKravgrunnlagDto(it).let { KravgrunnlagMapper().toKravgrunnlag(it, "") }
        }
}

val saksbehandlerToken =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImF6dXJlLWlkIGZvciBzYWtzYmVoYW5kbGVyIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJOQVZpZGVudCI6IlNha3NiZWhhbmRsZXIwMSJ9.271mDij4YsO4Kk8w8AvX5BXxlEA8U-UAOtdG1Ix_kQY"

fun TestApplicationRequest.addAuthSaksbehandler() {
    addHeader(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
}