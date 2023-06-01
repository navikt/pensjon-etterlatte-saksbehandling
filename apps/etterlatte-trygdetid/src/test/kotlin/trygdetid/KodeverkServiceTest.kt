package trygdetid

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.trygdetid.KodeverkService
import no.nav.etterlatte.trygdetid.LandNormalisert
import no.nav.etterlatte.trygdetid.klienter.Beskrivelse
import no.nav.etterlatte.trygdetid.klienter.Betydning
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

    @Test
    fun `Sjekk at mapping blir riktig`() {
        val betydning = Betydning(
            gyldigTil = "1900-01-01",
            gyldigFra = "9999-12-31",
            beskrivelser = mapOf(Pair("nb", Beskrivelse("term", "tekst")))
        )
        val testdatasandwich = mapOf(
            Pair(
                LandNormalisert.SOR_GEORGIA_OG_SOR_SANDWICHOYENE.isoCode,
                listOf<Betydning>(betydning)
            )
        )

        coEvery { mockKlient.hentLandkoder() } returns KodeverkResponse(testdatasandwich)

        runBlocking {
            val alleLand = service.hentAlleLand()
            assertEquals(1, alleLand.size)
            val land = alleLand[0]
            assertEquals(LandNormalisert.SOR_GEORGIA_OG_SOR_SANDWICHOYENE.beskrivelse, land.beskrivelse.tekst)
        }
    }

    private fun opprettLandkoderResponse(): KodeverkResponse =
        objectMapper.readValue(javaClass.getResource("/kodeverk/landkoder.json")!!.readText())
}