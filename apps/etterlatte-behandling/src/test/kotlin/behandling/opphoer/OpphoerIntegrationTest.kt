package no.nav.etterlatte.behandling.opphoer

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.spyk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.PdltjenesterKlientTest
import no.nav.etterlatte.behandling.DetaljertBehandlingDto
import no.nav.etterlatte.behandling.ViderefoertOpphoerRequest
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.dbutils.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.module
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpphoerIntegrationTest : BehandlingIntegrationTest() {
    private val pdltjenesterKlient = spyk<PdltjenesterKlientTest>()

    @BeforeAll
    fun start() {
        startServer(pdlTjenesterKlient = pdltjenesterKlient)
    }

    @AfterEach
    fun afterEach() {
        resetDatabase()
    }

    @AfterAll
    fun afterAllTests() {
        afterAll()
    }

    @Test
    fun `Skal kunne sette og fjerne opphoersdato på en vanlig behandling`() {
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
                        Assertions.assertEquals(HttpStatusCode.OK, it.status)
                        it.body()
                    }
            Assertions.assertNotNull(sak.id)

            val behandlingId =
                client
                    .post("/behandlinger/opprettbehandling") {
                        addAuthToken(tokenSaksbehandler)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(
                            BehandlingsBehov(
                                sak.id,
                                Persongalleri(fnr, "innsender", emptyList(), emptyList(), emptyList()),
                                Tidspunkt.now().toLocalDatetimeUTC().toString(),
                            ),
                        )
                    }.let {
                        Assertions.assertEquals(HttpStatusCode.OK, it.status)
                        UUID.fromString(it.body())
                    }

            val opphoersdato = YearMonth.now().plusMonths(4)
            client
                .post("/api/behandling/$behandlingId/viderefoert-opphoer") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        ViderefoertOpphoerRequest(
                            _dato = opphoersdato.toString(),
                            skalViderefoere = JaNei.JA,
                            begrunnelse = "Lorem",
                            vilkaarType = VilkaarType.BP_FORMAAL,
                        ),
                    )
                }.let {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                }

            val behandlingDto: DetaljertBehandlingDto =
                client
                    .get("/api/behandling/$behandlingId") {
                        addAuthToken(tokenSaksbehandler)
                    }.body()
            Assertions.assertEquals(opphoersdato, behandlingDto.viderefoertOpphoer?.dato)

            client
                .delete("/api/behandling/$behandlingId/viderefoert-opphoer") {
                    addAuthToken(tokenSaksbehandler)
                }.let {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                }

            val behandlingUtenOpphoer: DetaljertBehandlingDto =
                client
                    .get("/api/behandling/$behandlingId") {
                        addAuthToken(tokenSaksbehandler)
                    }.body()

            Assertions.assertEquals(null, behandlingUtenOpphoer.viderefoertOpphoer?.dato)
        }
    }
}
