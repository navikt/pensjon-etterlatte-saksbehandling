package no.nav.etterlatte.hendelserufoere

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.BehandlingKlient
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

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
                personIdent = SOEKER_FOEDSELSNUMMER.value,
                virkningsdato = LocalDate.parse("2018-01-01"),
                fodselsdato = LocalDate.parse("2000-01-01"),
                vedtaksType = VedtaksType.INNV,
            )

        runBlocking {
            ufoereHendelseFordeler.haandterHendelse(ufoereHendelse)
        }

        coVerify(exactly = 1) { behandlingKlient.postTilBehandling(any()) }
    }

    @Test
    fun `skal håndtere ufoerehendelse der bruker fyller 21 i måneden for virkningstidspunkt`() {
        coEvery { behandlingKlient.postTilBehandling(any()) } returns Unit

        val ufoereHendelse =
            UfoereHendelse(
                personIdent = SOEKER_FOEDSELSNUMMER.value,
                virkningsdato = LocalDate.parse("2021-01-01"),
                fodselsdato = LocalDate.parse("2000-01-01"),
                vedtaksType = VedtaksType.INNV,
            )

        runBlocking {
            ufoereHendelseFordeler.haandterHendelse(ufoereHendelse)
        }

        coVerify(exactly = 1) { behandlingKlient.postTilBehandling(any()) }
    }

    @Test
    fun `skal ignorere ufoerehendelse der bruker er 21 år og 1 mnd på virkningstidspunkt`() {
        val ufoereHendelse =
            UfoereHendelse(
                personIdent = SOEKER_FOEDSELSNUMMER.value,
                virkningsdato = LocalDate.parse("2021-02-01"),
                fodselsdato = LocalDate.parse("2000-01-01"),
                vedtaksType = VedtaksType.INNV,
            )

        runBlocking {
            ufoereHendelseFordeler.haandterHendelse(ufoereHendelse)
        }

        coVerify(exactly = 0) { behandlingKlient.postTilBehandling(any()) }
    }

    @Test
    fun `skal ignorere ufoerehendelse der bruker er 17 år og 11 mnd på virkningstidspunkt`() {
        val ufoereHendelse =
            UfoereHendelse(
                personIdent = SOEKER_FOEDSELSNUMMER.value,
                virkningsdato = LocalDate.parse("2017-12-01"),
                fodselsdato = LocalDate.parse("2000-01-01"),
                vedtaksType = VedtaksType.INNV,
            )

        runBlocking {
            ufoereHendelseFordeler.haandterHendelse(ufoereHendelse)
        }

        coVerify(exactly = 0) { behandlingKlient.postTilBehandling(any()) }
    }
}
