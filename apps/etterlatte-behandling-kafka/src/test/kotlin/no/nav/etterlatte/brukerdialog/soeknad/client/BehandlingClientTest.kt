package no.nav.etterlatte.brukerdialog.soeknad.client

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.jackson
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.brukerdialog.soeknad.PersongalleriMapper
import no.nav.etterlatte.libs.common.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.event.FordelerFordelt
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.Barnepensjon
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID

internal class BehandlingClientTest {
    @Test
    fun testInitierBehandling() {
        val behandlingId = UUID.randomUUID()
        val requestList = mutableListOf<HttpRequestData>()

        val behandlingClient = createBehandlingClient(requestList, behandlingId)
        val hendelseJson = objectMapper.readTree(javaClass.getResource("/behandlingsbehov/barnepensjon.json")!!.readText())
        val soeknad = objectMapper.treeToValue<Barnepensjon>(hendelseJson[FordelerFordelt.skjemaInfoKey])
        val hentetSaksid =
            behandlingClient.opprettBehandling(
                sakId1,
                soeknad.mottattDato,
                PersongalleriMapper.hentPersongalleriFraSoeknad(soeknad),
            )

        assertEquals(behandlingId, hentetSaksid)
        assertEquals(
            sakId1,
            objectMapper
                .readValue<BehandlingsBehov>(
                    (runBlocking { String(requestList[0].body.toByteArray()) }),
                ).sakId,
        )
    }

    @ParameterizedTest
    @MethodSource("ugyldigeHttpStatuskoder")
    fun `Skal kaste FeiletVedOpprettBehandling ved feil`(ugyldigStatusCode: HttpStatusCode) {
        val hendelseJson = objectMapper.readTree(javaClass.getResource("/behandlingsbehov/barnepensjon.json")!!.readText())
        val soeknad = objectMapper.treeToValue<Barnepensjon>(hendelseJson[FordelerFordelt.skjemaInfoKey])
        val client =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        respondError(ugyldigStatusCode)
                    }
                }
            }
        val behandlingClient = BehandlingClient(client, "")
        assertThrows<FeiletVedOpprettBehandling> {
            behandlingClient.opprettBehandling(
                sakId1,
                soeknad.mottattDato,
                PersongalleriMapper.hentPersongalleriFraSoeknad(soeknad),
            )
        }
    }

    companion object {
        @JvmStatic
        private fun ugyldigeHttpStatuskoder(): List<HttpStatusCode> = HttpStatusCode.allStatusCodes.filter { !it.isSuccess() }
    }

    private fun createBehandlingClient(
        requestList: MutableList<HttpRequestData>,
        randomUUID: UUID,
    ): BehandlingClient {
        val httpClient =
            HttpClient(
                MockEngine { request ->
                    requestList.add(request)
                    respond(
                        content = ByteReadChannel(randomUUID.toString()),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain"),
                    )
                },
            ) {
                install(ContentNegotiation) { jackson {} }
            }
        return BehandlingClient(httpClient, "")
    }
}
