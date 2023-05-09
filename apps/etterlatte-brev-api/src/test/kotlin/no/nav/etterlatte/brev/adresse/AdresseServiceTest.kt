package no.nav.etterlatte.brev.adresse

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.behandling.Attestant
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.Saksbehandler
import no.nav.etterlatte.brev.navansatt.NavansattKlient
import no.nav.etterlatte.brev.navansatt.SaksbehandlerInfo
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AdresseServiceTest {

    private val norg2Mock = mockk<Norg2Klient>()
    private val navansattMock = mockk<NavansattKlient>()
    private val regoppslagMock = mockk<RegoppslagKlient>()

    private val adresseService = AdresseService(norg2Mock, navansattMock, regoppslagMock)

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(norg2Mock, navansattMock, regoppslagMock)
    }

    @Test
    fun `Hent avsender og attestant fungerer`() {
        coEvery { norg2Mock.hentEnhet(saksbehandler.enhet) } returns opprettEnhet(saksbehandler.enhet)
        coEvery { norg2Mock.hentEnhet(attestant.enhet) } returns opprettEnhet(attestant.enhet)
        coEvery { navansattMock.hentSaksbehandlerInfo(saksbehandler.ident) }
            .returns(opprettSaksbehandlerInfo(saksbehandler.ident))
        coEvery { navansattMock.hentSaksbehandlerInfo(attestant.ident) }
            .returns(opprettSaksbehandlerInfo(attestant.ident))

        val vedtak = ForenkletVedtak(1, VedtakStatus.FATTET_VEDTAK, VedtakType.INNVILGELSE, saksbehandler, attestant)

        val (faktiskAvsender, faktiskAttestant) = runBlocking {
            adresseService.hentAvsenderOgAttestant(vedtak)
        }

        faktiskAvsender.saksbehandler shouldBe "fornavn etternavn"
        faktiskAttestant?.navn shouldBe "fornavn etternavn"

        coVerify(exactly = 1) {
            norg2Mock.hentEnhet(saksbehandler.enhet)
            norg2Mock.hentEnhet(attestant.enhet)
            navansattMock.hentSaksbehandlerInfo(saksbehandler.ident)
            navansattMock.hentSaksbehandlerInfo(attestant.ident)
        }
    }

    private fun opprettEnhet(enhetNr: String) = Norg2Enhet(
        navn = "NAV Porsgrunn",
        enhetNr = enhetNr,
        kontaktinfo = Norg2Kontaktinfo(
            telefonnummer = "00 11 22 33",
            epost = "test@nav.no",
            postadresse = Postadresse(
                type = "postboksadresse",
                postboksnummer = "012345",
                postboksanlegg = "Testanlegget"
            )
        )
    )

    private fun opprettSaksbehandlerInfo(ident: String) =
        SaksbehandlerInfo(ident, "navn", "fornavn", "etternavn", "epost@nav.no")

    companion object {
        private val saksbehandler = Saksbehandler("Z123456", "1000")
        private val attestant = Attestant("Z00002", "3000")
    }
}