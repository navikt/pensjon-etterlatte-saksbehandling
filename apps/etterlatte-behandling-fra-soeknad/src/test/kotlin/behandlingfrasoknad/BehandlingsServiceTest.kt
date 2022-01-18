package behandlingfrasoknad

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
import no.nav.etterlatte.behandlingfrasoknad.BehandlingsService
import no.nav.etterlatte.behandlingfrasoknad.NyBehandlingRequest
import no.nav.etterlatte.behandlingfrasoknad.Opplysningsuthenter
import org.junit.jupiter.api.Test
import no.nav.etterlatte.common.objectMapper

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
        val opplysningsuthenter = mockk<Opplysningsuthenter>()
        val behandlingsservice = BehandlingsService(httpClient, "", opplysningsuthenter)
        every { opplysningsuthenter.lagOpplysningsListe(any()) } returns emptyList()
        val hendelseJson = objectMapper.readTree(javaClass.getResource("/fullMessage2.json")!!.readText())
        val hentetSaksid =  behandlingsservice.initierBehandling(1, hendelseJson["@skjema_info"], hendelseJson["@lagret_soeknad_id"].longValue())

        assertEquals(randomUUID, hentetSaksid)
        assertEquals(1, objectMapper.readValue<NyBehandlingRequest>((requestList[0].body as TextContent).text).sak)
        assertTrue(objectMapper.readValue<NyBehandlingRequest>((requestList[0].body as TextContent).text).opplysninger.isEmpty())
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
        val opplysningsuthenter = mockk<Opplysningsuthenter>()
        val behandlingsservice = BehandlingsService(httpClient, "http://behandlingsservice", opplysningsuthenter)
        val skaffSakResultat = behandlingsservice.skaffSak("22","barnepensjon")

        assertEquals(1, skaffSakResultat)
        assertEquals("http://behandlingsservice/personer/22/saker/barnepensjon", requestList[0].url.toString())
    }
}