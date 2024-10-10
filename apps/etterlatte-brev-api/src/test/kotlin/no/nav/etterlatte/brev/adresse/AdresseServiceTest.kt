package no.nav.etterlatte.brev.adresse

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brev.AvsenderRequest
import no.nav.etterlatte.brev.adresse.saksbehandler.SaksbehandlerKlient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AdresseServiceTest {
    private val norg2Mock = mockk<Norg2Klient>()
    private val saksbehandlerKlient = mockk<SaksbehandlerKlient>()
    private val regoppslagMock = mockk<RegoppslagKlient>()
    private val adresseService = AdresseService(norg2Mock, saksbehandlerKlient, regoppslagMock)

    companion object {
        private val ANSVARLIG_ENHET = Enheter.defaultEnhet.enhetNr
        private const val SAKSBEHANDLER = "Z123456"
        private const val ATTESTANT = "Z00002"
    }

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(norg2Mock, saksbehandlerKlient, regoppslagMock)
    }

    @Test
    fun `Hent avsender og attestant fungerer`() {
        coEvery { norg2Mock.hentEnhet(any()) } returns opprettEnhet()
        coEvery { saksbehandlerKlient.hentSaksbehandlerNavn(SAKSBEHANDLER, any()) } returns "saks behandler"
        coEvery { saksbehandlerKlient.hentSaksbehandlerNavn(ATTESTANT, any()) } returns "att estant"

        val avsenderRequest =
            AvsenderRequest(
                saksbehandlerIdent = SAKSBEHANDLER,
                sakenhet = ANSVARLIG_ENHET,
                attestantIdent = ATTESTANT,
            )
        val faktiskAvsender =
            runBlocking {
                adresseService.hentAvsender(avsenderRequest, mockk())
            }

        faktiskAvsender.saksbehandler shouldBe "saks behandler"
        faktiskAvsender.attestant shouldBe "att estant"

        coVerify(exactly = 1) {
            norg2Mock.hentEnhet(avsenderRequest.sakenhet)
            saksbehandlerKlient.hentSaksbehandlerNavn(avsenderRequest.saksbehandlerIdent, any())
            saksbehandlerKlient.hentSaksbehandlerNavn(avsenderRequest.attestantIdent!!, any())
        }
    }

    @Test
    fun `Hent avsender med innlogget bruker og sak`() {
        val zIdent = "Z123456"

        coEvery { norg2Mock.hentEnhet(any()) } returns opprettEnhet()
        coEvery { saksbehandlerKlient.hentSaksbehandlerNavn(any(), any()) } returns "saks behandler"

        val sakId = randomSakId()
        val sak = Sak("ident", SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr)

        val faktiskAvsender =
            runBlocking {
                adresseService.hentAvsender(AvsenderRequest(saksbehandlerIdent = zIdent, sakenhet = sak.enhet), mockk())
            }

        faktiskAvsender.saksbehandler shouldBe "saks behandler"

        coVerify(exactly = 1) {
            norg2Mock.hentEnhet(sak.enhet)
            saksbehandlerKlient.hentSaksbehandlerNavn(zIdent, any())
        }
    }

    private fun opprettEnhet() =
        Norg2Enhet(
            navn = "NAV Porsgrunn",
            enhetNr = ANSVARLIG_ENHET,
            kontaktinfo =
                Norg2Kontaktinfo(
                    telefonnummer = "00 11 22 33",
                    epost =
                        Norg2Epost(
                            adresse = "test@nav.no",
                        ),
                    postadresse =
                        Postadresse(
                            type = "postboksadresse",
                            postboksnummer = "012345",
                            postboksanlegg = "Testanlegget",
                        ),
                ),
        )
}
