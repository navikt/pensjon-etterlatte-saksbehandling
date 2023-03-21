package no.nav.etterlatte.brev.adresse.enhetsregister

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BrregServiceTest {

    private val mockKlient = mockk<BrregKlient>()
    private val service = spyk(BrregService(mockKlient))

    @AfterEach
    fun afterEach() {
        clearAllMocks()
        confirmVerified()
    }

    @Test
    fun `Henting av enheter fra brreg`() {
        coEvery { mockKlient.hentEnheter() } returns listOf(
            Enhet("921627009", "STATSFORVALTERENS FELLESTJENESTER"),
            Enhet("974762994", "STATSFORVALTEREN I AGDER"),
            Enhet("974761645", "STATSFORVALTEREN I INNLANDET"),
            Enhet("974764687", "STATSFORVALTEREN I NORDLAND")
        )

        val resultat = runBlocking { service.hentAlleStatsforvaltere() }

        assertEquals(4, resultat.size)

        coVerify(exactly = 1) {
            service.hentAlleStatsforvaltere()
            mockKlient.hentEnheter()
        }
    }

    @Test
    fun `Hent cachet resultat`() {
        coEvery { mockKlient.hentEnheter() } returns listOf(
            Enhet("921627009", "STATSFORVALTERENS FELLESTJENESTER"),
            Enhet("974762994", "STATSFORVALTEREN I AGDER"),
            Enhet("974761645", "STATSFORVALTEREN I INNLANDET"),
            Enhet("974764687", "STATSFORVALTEREN I NORDLAND")
        )

        repeat(10) {
            runBlocking { service.hentAlleStatsforvaltere() }
        }

        coVerify(exactly = 10) { service.hentAlleStatsforvaltere() }
        coVerify(exactly = 1) { mockKlient.hentEnheter() }
    }

    @Test
    fun `Tom liste i respons blir ikke cachet`() {
        coEvery { mockKlient.hentEnheter() } returns emptyList()

        repeat(10) {
            runBlocking { service.hentAlleStatsforvaltere() }
        }

        coVerify(exactly = 10) {
            service.hentAlleStatsforvaltere()
            mockKlient.hentEnheter()
        }
    }
}