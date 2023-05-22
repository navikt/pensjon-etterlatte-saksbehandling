package trygdetid

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.trygdetid.KodeverkService
import no.nav.etterlatte.trygdetid.klienter.KodeverkKlient
import no.nav.etterlatte.trygdetid.klienter.KodeverkResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class KodeverkServiceTest {
    private val mockKlient = mockk<KodeverkKlient>()
    private val service = KodeverkService(mockKlient)

    @Test
    fun `Hent alle landkoder`() {
        coEvery { mockKlient.hentLandkoder() } returns opprettLandkoderResponse()

        runBlocking {
            val land = service.hentAlleLand()

            assertEquals(5, land.size)
            coVerify(exactly = 1) { mockKlient.hentLandkoder() }
        }
    }

    @Test
    fun `Cache for landkode fungerer`() {
        coEvery { mockKlient.hentLandkoder() } returns opprettLandkoderResponse()

        runBlocking {
            val land = service.hentAlleLand()
            assertEquals(5, land.size)
        }

        coVerify(exactly = 1) { mockKlient.hentLandkoder() }
    }

    private fun opprettLandkoderResponse(): KodeverkResponse =
        objectMapper.readValue(javaClass.getResource("/kodeverk/landkoder.json")!!.readText())
}