package no.nav.etterlatte.brev.adresse

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.adresse.navansatt.NavansattKlient
import no.nav.etterlatte.brev.adresse.navansatt.SaksbehandlerInfo
import no.nav.etterlatte.common.Enhet
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random

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
        coEvery { norg2Mock.hentEnhet(any()) } returns opprettEnhet()
        coEvery { navansattMock.hentSaksbehandlerInfo(SAKSBEHANDLER) }
            .returns(opprettSaksbehandlerInfo(SAKSBEHANDLER, "saks", "behandler"))
        coEvery { navansattMock.hentSaksbehandlerInfo(ATTESTANT) }
            .returns(opprettSaksbehandlerInfo(ATTESTANT, "att", "estant"))

        val avsenderRequest =
            AvsenderRequest(
                saksbehandlerIdent = SAKSBEHANDLER,
                sakenhet = ANSVARLIG_ENHET,
                attestantIdent = ATTESTANT,
            )
        val faktiskAvsender =
            runBlocking {
                adresseService.hentAvsender(avsenderRequest)
            }

        faktiskAvsender.saksbehandler shouldBe "saks behandler"
        faktiskAvsender.attestant shouldBe "att estant"

        coVerify(exactly = 1) {
            norg2Mock.hentEnhet(avsenderRequest.sakenhet.enhetNr)
            navansattMock.hentSaksbehandlerInfo(avsenderRequest.saksbehandlerIdent)
            navansattMock.hentSaksbehandlerInfo(avsenderRequest.attestantIdent!!)
        }
    }

    @Test
    fun `Hent avsender med innlogget bruker og sak`() {
        val zIdent = "Z123456"

        coEvery { norg2Mock.hentEnhet(any()) } returns opprettEnhet()
        coEvery { navansattMock.hentSaksbehandlerInfo(any()) }
            .returns(opprettSaksbehandlerInfo(zIdent, "saks", "behandler"))

        val sakId = Random.nextLong()
        val sak = Sak("ident", SakType.BARNEPENSJON, sakId, Enhet.defaultEnhet.enhetNr)

        val faktiskAvsender =
            runBlocking {
                adresseService.hentAvsender(AvsenderRequest(saksbehandlerIdent = zIdent, sakenhet = sak.enhet.let { Enhet.fraEnhetNr(it) }))
            }

        faktiskAvsender.saksbehandler shouldBe "saks behandler"

        coVerify(exactly = 1) {
            norg2Mock.hentEnhet(sak.enhet)
            navansattMock.hentSaksbehandlerInfo(zIdent)
        }
    }

    private fun opprettEnhet() =
        Norg2Enhet(
            navn = "NAV Porsgrunn",
            enhetNr = ANSVARLIG_ENHET.enhetNr,
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

    private fun opprettSaksbehandlerInfo(
        ident: String,
        fornavn: String,
        etternavn: String,
    ) = SaksbehandlerInfo(ident, "navn", fornavn, etternavn, "epost@nav.no")

    companion object {
        private val ANSVARLIG_ENHET = Enhet.AALESUND
        private const val SAKSBEHANDLER = "Z123456"
        private const val ATTESTANT = "Z00002"
    }
}
