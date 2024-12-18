package no.nav.etterlatte.kodeverk

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.kodeverk.KodeverkNavn.LANDKODER
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.trygdetid.land.LandNormalisert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class KodeverkServiceTest {
    private val mockKlient = mockk<KodeverkKlientImpl>()
    private val service = KodeverkService(mockKlient)
    private val saksbehandler = simpleSaksbehandler()

    @Test
    fun `Hent alle landkoder`() {
        coEvery { mockKlient.hent(LANDKODER, false, saksbehandler) } returns opprettLandkoderResponse()

        runBlocking {
            val land = service.hentAlleLand(saksbehandler)

            assertEquals(5, land.size)
            coVerify(exactly = 1) { mockKlient.hent(LANDKODER, false, saksbehandler) }
        }
    }

    @Test
    fun `Cache for landkode fungerer`() {
        coEvery { mockKlient.hent(LANDKODER, false, saksbehandler) } returns opprettLandkoderResponse()

        runBlocking {
            val land = service.hentAlleLand(saksbehandler)
            assertEquals(5, land.size)
        }

        coVerify(exactly = 1) { mockKlient.hent(LANDKODER, false, saksbehandler) }
    }

    @Test
    fun `Sjekk at mapping blir riktig`() {
        val betydning =
            Betydning(
                gyldigTil = "1900-01-01",
                gyldigFra = "9999-12-31",
                beskrivelser = mapOf(Pair("nb", Beskrivelse("term", "tekst"))),
            )
        val testdatasandwich =
            mapOf(
                Pair(
                    LandNormalisert.SOR_GEORGIA_OG_SOR_SANDWICHOYENE.isoCode,
                    listOf(betydning),
                ),
            )

        coEvery { mockKlient.hent(LANDKODER, false, saksbehandler) } returns KodeverkResponse(testdatasandwich)

        runBlocking {
            val alleLand = service.hentAlleLand(saksbehandler)
            assertEquals(1, alleLand.size)
            val land = alleLand[0]
            assertEquals(LandNormalisert.SOR_GEORGIA_OG_SOR_SANDWICHOYENE.beskrivelse, land.beskrivelse.tekst)
        }
    }

    private fun opprettLandkoderResponse(): KodeverkResponse = objectMapper.readValue(javaClass.getResource("/landkoder.json")!!.readText())
}
