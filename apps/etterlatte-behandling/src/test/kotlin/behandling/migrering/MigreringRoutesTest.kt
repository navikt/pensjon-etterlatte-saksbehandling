package behandling.migrering

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.module
import no.nav.etterlatte.rapidsandrivers.migrering.AvdoedForelder
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetid
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigreringRoutesTest : BehandlingIntegrationTest() {
    @BeforeAll
    fun start() = startServer()

    @AfterAll
    fun shutdown() = afterAll()

    @Test
    fun `migrering oppretter sak og behandling`() {
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
            application { module(applicationContext) }
            val request =
                MigreringRequest(
                    pesysId = PesysId(1),
                    enhet = Enhet("4817"),
                    soeker = SOEKER_FOEDSELSNUMMER,
                    avdoedForelder = listOf(AvdoedForelder(AVDOED_FOEDSELSNUMMER, Tidspunkt.now())),
                    gjenlevendeForelder = null,
                    virkningstidspunkt = YearMonth.now(),
                    foersteVirkningstidspunkt = YearMonth.now().minusYears(10),
                    beregning =
                        Beregning(
                            brutto = BigDecimal(1000),
                            netto = BigDecimal(1000),
                            anvendtTrygdetid = BigDecimal(40),
                            datoVirkFom = Tidspunkt.now(),
                            g = BigDecimal(100000),
                        ),
                    trygdetid = Trygdetid(emptyList()),
                    flyktningStatus = false,
                    spraak = Spraak.NN,
                )

            val response: BehandlingOgSak =
                client.post("/migrering") {
                    addAuthToken(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.apply {
                    Assertions.assertEquals(HttpStatusCode.Created, status)
                }.body()

            val behandling =
                client.get("/behandlinger/${response.behandlingId}") {
                    addAuthToken(tokenSaksbehandler)
                }.apply {
                    Assertions.assertEquals(HttpStatusCode.OK, status)
                }.body<DetaljertBehandling>()

            // reformtidspunkt er konfigurert til å være 2023-10 p.t.
            Assertions.assertEquals(YearMonth.of(2023, 10), behandling.virkningstidspunkt!!.dato)

            client.get("/saker/${behandling.sak}") {
                addAuthToken(tokenSaksbehandler)
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, status)
            }
        }
    }
}
