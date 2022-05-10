package behandlingfrasoknad

import BehandlingsService
import NyBehandlingRequest
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.HttpRequestData
import io.ktor.content.TextContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.mockk.every
import io.mockk.mockk
import model.GyldigSoeknadService
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import org.junit.jupiter.api.Test
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Assertions.*
import java.util.*

internal class BehandlingsServiceTest {

    @Test
    fun testInitierBehandling() {
        val randomUUID = UUID.randomUUID()
        val requestList = mutableListOf<HttpRequestData>()
        val httpClient = HttpClient(MockEngine { request ->
            requestList.add(request)
            respond(
                content = ByteReadChannel(randomUUID.toString()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/plain"),
            )
        }) {
            install(JsonFeature) { serializer = JacksonSerializer() }
        }
        val persongalleri = mockk<GyldigSoeknadService>()
        val behandlingsservice = BehandlingsService(httpClient, "")
        every { persongalleri.hentPersongalleriFraSoeknad(any()) } returns Persongalleri(
            "",
            "",
            emptyList(),
            emptyList(),
            emptyList()
        )

        val hendelseJson = objectMapper.readTree(javaClass.getResource("/fullMessage2.json")!!.readText())
        val hentetSaksid = behandlingsservice.initierBehandling(
            1,
            hendelseJson["@skjema_info"]["mottattDato"].asText(),
            persongalleri.hentPersongalleriFraSoeknad(hendelseJson["@skjema_info"])
        )

        assertEquals(randomUUID, hentetSaksid)
        assertEquals(1, objectMapper.readValue<NyBehandlingRequest>((requestList[0].body as TextContent).text).sak)
    }

    @Test
    fun testSkaffSak() {
        val requestList = mutableListOf<HttpRequestData>()
        val httpClient = HttpClient(MockEngine { request ->
            requestList.add(request)
            respond(
                content = ByteReadChannel("""{"id":1}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) {
            install(JsonFeature) { serializer = JacksonSerializer() }
        }

        val behandlingsservice = BehandlingsService(httpClient, "http://behandlingsservice")
        val skaffSakResultat = behandlingsservice.skaffSak("22", "barnepensjon")

        assertEquals(1, skaffSakResultat)
        assertEquals("http://behandlingsservice/personer/22/saker/barnepensjon", requestList[0].url.toString())
    }
}