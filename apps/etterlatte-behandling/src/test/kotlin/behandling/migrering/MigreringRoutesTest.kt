package no.nav.etterlatte.behandling.migrering

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.rapidsandrivers.migrering.AvdoedForelder
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetid
import no.nav.etterlatte.sikkerLogg
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
            val client =
                runServerWithModule(server) {
                    restModule(
                        sikkerLogg,
                        withMetrics = true,
                    ) {
                        applicationContext
                    }
                }

            val request =
                MigreringRequest(
                    pesysId = PesysId(1),
                    enhet = Enhet("4817"),
                    soeker = SOEKER_FOEDSELSNUMMER,
                    avdoedForelder = listOf(AvdoedForelder(AVDOED_FOEDSELSNUMMER, Tidspunkt.now())),
                    dodAvYrkesskade = false,
                    gjenlevendeForelder = null,
                    foersteVirkningstidspunkt = YearMonth.now(),
                    beregning =
                        Beregning(
                            brutto = 3500,
                            netto = 3500,
                            anvendtTrygdetid = 40,
                            datoVirkFom = Tidspunkt.now(),
                            prorataBroek = null,
                            g = 100_000,
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

            Assertions.assertEquals(YearMonth.of(2024, 1), behandling.virkningstidspunkt!!.dato)
            Assertions.assertEquals(Prosesstype.AUTOMATISK, behandling.prosesstype)

            client.get("/saker/${behandling.sak}") {
                addAuthToken(tokenSaksbehandler)
            }.apply {
                Assertions.assertEquals(HttpStatusCode.OK, status)
            }
        }
    }
}
