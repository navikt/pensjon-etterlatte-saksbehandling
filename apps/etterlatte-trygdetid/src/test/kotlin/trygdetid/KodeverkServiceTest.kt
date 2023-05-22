package trygdetid

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
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

            val forventedeLand =
                "[{\"gyldigFra\":\"1900-01-01\",\"gyldigTil\":\"9999-12-31\",\"beskrivelser\":{\"nn\":{\"term\":\"CUBA\",\"tekst\":\"CUBA\"},\"nb\":{\"term\":\"CUBA\",\"tekst\":\"CUBA\"},\"en\":{\"term\":\"CUBA\",\"tekst\":\"CUBA\"}}},{\"gyldigFra\":\"1900-01-01\",\"gyldigTil\":\"9999-12-31\",\"beskrivelser\":{\"nn\":{\"term\":\"POLEN\",\"tekst\":\"POLEN\"},\"nb\":{\"term\":\"POLEN\",\"tekst\":\"POLEN\"},\"en\":{\"term\":\"POLEN\",\"tekst\":\"POLEN\"}}},{\"gyldigFra\":\"1900-01-01\",\"gyldigTil\":\"9999-12-31\",\"beskrivelser\":{\"nn\":{\"term\":\"SVERIGE\",\"tekst\":\"SVERIGE\"},\"nb\":{\"term\":\"SVERIGE\",\"tekst\":\"SVERIGE\"},\"en\":{\"term\":\"SVERIGE\",\"tekst\":\"SVERIGE\"}}},{\"gyldigFra\":\"1900-01-01\",\"gyldigTil\":\"9999-12-31\",\"beskrivelser\":{\"nn\":{\"term\":\"ISLAND\",\"tekst\":\"ISLAND\"},\"nb\":{\"term\":\"ISLAND\",\"tekst\":\"ISLAND\"},\"en\":{\"term\":\"ISLAND\",\"tekst\":\"ISLAND\"}}},{\"gyldigFra\":\"1900-01-01\",\"gyldigTil\":\"9999-12-31\",\"beskrivelser\":{\"nn\":{\"term\":\"NORGE\",\"tekst\":\"NORGE\"},\"nb\":{\"term\":\"NORGE\",\"tekst\":\"NORGE\"},\"en\":{\"term\":\"NORGE\",\"tekst\":\"NORGE\"}}}]"
            assertEquals(forventedeLand, land.toJson())
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