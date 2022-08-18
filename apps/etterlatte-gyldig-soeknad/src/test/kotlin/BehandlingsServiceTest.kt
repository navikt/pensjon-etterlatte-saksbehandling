package behandlingfrasoknad

import BehandlingsService
import NyBehandlingRequest
import com.fasterxml.jackson.module.kotlin.readValue
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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import model.GyldigSoeknadService
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class BehandlingsServiceTest {

    @Test
    fun testInitierBehandling() {
        val randomUUID = UUID.randomUUID()
        val requestList = mutableListOf<HttpRequestData>()
        val httpClient = HttpClient(
            MockEngine { request ->
                requestList.add(request)
                respond(
                    content = ByteReadChannel(randomUUID.toString()),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )
            }
        ) {
            install(ContentNegotiation) { jackson {} }
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

        val hendelseJson = objectMapper.readTree(javaClass.getResource("/fordeltmelding.json")!!.readText())
        val hentetSaksid = behandlingsservice.initierBehandling(
            1,
            hendelseJson["@skjema_info"]["mottattDato"].asText(),
            persongalleri.hentPersongalleriFraSoeknad(hendelseJson["@skjema_info"])
        )

        assertEquals(randomUUID, hentetSaksid)
        assertEquals(
            1,
            objectMapper.readValue<NyBehandlingRequest>((runBlocking { String(requestList[0].body.toByteArray()) })).sak
        )
    }

    @Test
    fun testSkaffSak() {
        val requestList = mutableListOf<HttpRequestData>()
        val httpClient = HttpClient(
            MockEngine { request ->
                requestList.add(request)
                respond(
                    content = ByteReadChannel("""{"id":1}"""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        ) {
            install(ContentNegotiation) { jackson {} }
        }

        val behandlingsservice = BehandlingsService(httpClient, "http://behandlingsservice")
        val skaffSakResultat = behandlingsservice.skaffSak("22", "barnepensjon")

        assertEquals(1, skaffSakResultat)
        assertEquals("http://behandlingsservice/personer/22/saker/barnepensjon", requestList[0].url.toString())
    }
}