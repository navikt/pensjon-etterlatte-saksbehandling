package no.nav.etterlatte.behandling.etterbetaling

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.module
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.util.UUID

class EtterbetalingTest : BehandlingIntegrationTest() {
    @BeforeEach
    fun start() = startServer()

    @AfterEach
    fun afterEach() {
        afterAll()
    }

    @Test
    fun `kan registrere etterbetaling og saa hente den ut igjen`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson { registerModule(JavaTimeModule()) }
                    }
                }
            application {
                module(applicationContext)
            }

            val fnr = AVDOED_FOEDSELSNUMMER.value

            val sak: Sak =
                client.post("personer/saker/${SakType.BARNEPENSJON}") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(fnr))
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }.let {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                    it.body()
                }

            val utgangspunkt = LocalDate.of(2023, Month.OCTOBER, 1)
            val behandlingId =
                client.post("/behandlinger/opprettbehandling") {
                    addAuthToken(tokenSaksbehandler)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        BehandlingsBehov(
                            sak.id,
                            Persongalleri(fnr, "innsender", emptyList(), emptyList(), emptyList()),
                            LocalDateTime.of(utgangspunkt, LocalTime.NOON).toString(),
                        ),
                    )
                }.let {
                    Assertions.assertEquals(HttpStatusCode.OK, it.status)
                    UUID.fromString(it.body())
                }

            val fraDato = utgangspunkt.minusMonths(3)
            val tilDato = utgangspunkt
            client.put("/api/behandling/$behandlingId/etterbetaling") {
                addAuthToken(tokenSaksbehandler)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    EtterbetalingDTO(
                        fraDato = fraDato,
                        tilDato = tilDato,
                    ),
                )
            }

            client.get("/api/behandling/$behandlingId/etterbetaling") {
                addAuthToken(tokenSaksbehandler)
            }.let {
                Assertions.assertEquals(HttpStatusCode.OK, it.status)
                val dto = it.body<EtterbetalingDTO>()
                Assertions.assertEquals(LocalDate.of(2023, Month.JULY, 1), dto.fraDato)
                Assertions.assertEquals(LocalDate.of(2023, Month.OCTOBER, 31), dto.tilDato)
            }
        }
    }
}
