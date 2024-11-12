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
        coEvery { behandlingKlient.postTilBehandling(any()) } returns Unit

        val ufoereHendelse =
            UfoereHendelse(
                personIdent = "12312312312",
                virkningsdato = "2018-01-01",
                fodselsdato = "2000-01-01",
                vedtaksType = "ufoere",
            )

        runBlocking {
            ufoereHendelseFordeler.haandterHendelse(ufoereHendelse)
        }

        coVerify(exactly = 1) { behandlingKlient.postTilBehandling(any()) }
    }

    @Test
    fun `skal håndtere ufoerehendelse der bruker fyller 21 på virkningstidspunkt`() {
        coEvery { behandlingKlient.postTilBehandling(any()) } returns Unit

        val ufoereHendelse =
            UfoereHendelse(
                personIdent = "12312312312",
                virkningsdato = "2021-01-01",
                fodselsdato = "2000-01-01",
                vedtaksType = "ufoere",
            )

        runBlocking {
            ufoereHendelseFordeler.haandterHendelse(ufoereHendelse)
        }

        coVerify(exactly = 1) { behandlingKlient.postTilBehandling(any()) }
    }

    @Test
    fun `skal ignorere ufoerehendelse der bruker ikke er mellom og 18 og 21 på virkningstidspunkt`() {
        val ufoereHendelse =
            UfoereHendelse(
                personIdent = "12312312312",
                virkningsdato = "2021-01-01",
                fodselsdato = "1980-01-01",
                vedtaksType = "ufoere",
            )

        runBlocking {
            ufoereHendelseFordeler.haandterHendelse(ufoereHendelse)
        }
    }
}
