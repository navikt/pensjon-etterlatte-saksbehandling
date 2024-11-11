package no.nav.etterlatte.hendelserufoere

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.BehandlingKlient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UfoereHendelseFordelerTest {
    private val behandlingKlient = mockk<BehandlingKlient>()
    private lateinit var ufoereHendelseFordeler: UfoereHendelseFordeler

    @BeforeEach
    fun setup() {
        ufoereHendelseFordeler = UfoereHendelseFordeler(behandlingKlient)
    }

    @Test
    fun `skal håndtere ufoerehendelse der bruker er mellom og 18 og 21 på virkningstidspunkt`() {
        val attenAarIMaaneder = 12 * 18

        coEvery { behandlingKlient.postTilBehandling(any()) } returns Unit

        val ufoereHendelse: UfoereHendelse =
            UfoereHendelse().apply {
                personidentifikator = "12312312312"
                ytelse = "ufoere"
                virkningstidspunkt = "2021-01-01"
                alderVedVirkningstidspunkt = attenAarIMaaneder
                hendelsestype = "ufoere"
            }

        runBlocking {
            ufoereHendelseFordeler.haandterHendelse(ufoereHendelse)
        }

        coVerify(exactly = 1) { behandlingKlient.postTilBehandling(any()) }
    }

    @Test
    fun `skal håndtere ufoerehendelse der bruker fyller 21 på virkningstidspunkt`() {
        val tjueenAarIMaaneder = 12 * 21

        coEvery { behandlingKlient.postTilBehandling(any()) } returns Unit

        val ufoereHendelse: UfoereHendelse =
            UfoereHendelse().apply {
                personidentifikator = "12312312312"
                ytelse = "ufoere"
                virkningstidspunkt = "2021-01-01"
                alderVedVirkningstidspunkt = tjueenAarIMaaneder
                hendelsestype = "ufoere"
            }

        runBlocking {
            ufoereHendelseFordeler.haandterHendelse(ufoereHendelse)
        }

        coVerify(exactly = 1) { behandlingKlient.postTilBehandling(any()) }
    }

    @Test
    fun `skal ignorere ufoerehendelse der bruker ikke er mellom og 18 og 21 på virkningstidspunkt`() {
        val tolvAarIMaaneder = 12 * 12

        val ufoereHendelse: UfoereHendelse =
            UfoereHendelse().apply {
                personidentifikator = "12312312312"
                ytelse = "ufoere"
                virkningstidspunkt = "2021-01-01"
                alderVedVirkningstidspunkt = tolvAarIMaaneder
                hendelsestype = "ufoere"
            }

        runBlocking {
            ufoereHendelseFordeler.haandterHendelse(ufoereHendelse)
        }
    }
}
