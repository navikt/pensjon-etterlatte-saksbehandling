package no.nav.etterlatte.gyldigsoeknad.client

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.gyldigsoeknad.PersongalleriMapper
import no.nav.etterlatte.libs.common.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.event.FordelerFordelt
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.Barnepensjon
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
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
                1,
                soeknad.mottattDato,
                PersongalleriMapper.hentPersongalleriFraSoeknad(soeknad),
            )

        assertEquals(behandlingId, hentetSaksid)
        assertEquals(
            1,
            objectMapper
                .readValue<BehandlingsBehov>(
                    (runBlocking { String(requestList[0].body.toByteArray()) }),
                ).sakId,
        )
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
