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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.gyldigsoeknad.barnepensjon.GyldigSoeknadService
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.event.FordelerFordelt
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.Barnepensjon
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class BehandlingClientTest {

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
        val behandlingsservice = BehandlingClient(httpClient, "")
        every { persongalleri.hentPersongalleriFraSoeknad(any()) } returns Persongalleri(
            "",
            "",
            emptyList(),
            emptyList(),
            emptyList()
        )

        val hendelseJson = objectMapper.readTree(javaClass.getResource("/fordeltmelding.json")!!.readText())
        val soeknad = objectMapper.treeToValue<Barnepensjon>(hendelseJson[FordelerFordelt.skjemaInfoKey])
        val hentetSaksid = behandlingsservice.initierBehandling(
            1,
            soeknad.mottattDato,
            persongalleri.hentPersongalleriFraSoeknad(soeknad)
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

        val behandlingsservice = BehandlingClient(httpClient, "http://behandlingsservice")
        val skaffSakResultat = behandlingsservice.skaffSak("22", "barnepensjon")

        assertEquals(1, skaffSakResultat)
        assertEquals("http://behandlingsservice/personer/saker/barnepensjon", requestList[0].url.toString())
    }
}