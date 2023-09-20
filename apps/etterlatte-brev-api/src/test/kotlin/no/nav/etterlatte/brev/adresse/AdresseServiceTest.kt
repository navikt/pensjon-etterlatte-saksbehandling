package no.nav.etterlatte.brev.adresse

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.navansatt.NavansattKlient
import no.nav.etterlatte.brev.navansatt.SaksbehandlerInfo
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
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

        val vedtak =
            ForenkletVedtak(
                1,
                VedtakStatus.FATTET_VEDTAK,
                VedtakType.INNVILGELSE,
                ANSVARLIG_ENHET,
                SAKSBEHANDLER,
                ATTESTANT,
            )

        val faktiskAvsender =
            runBlocking {
                adresseService.hentAvsender(vedtak)
            }

        faktiskAvsender.saksbehandler shouldBe "saks behandler"
        faktiskAvsender.attestant shouldBe "att estant"

        coVerify(exactly = 1) {
            norg2Mock.hentEnhet(vedtak.ansvarligEnhet)
            navansattMock.hentSaksbehandlerInfo(vedtak.saksbehandlerIdent)
            navansattMock.hentSaksbehandlerInfo(vedtak.attestantIdent!!)
        }
    }

    @Test
    fun `Hent avsender med innlogget bruker og sak`() {
        val zIdent = "Z123456"

        coEvery { norg2Mock.hentEnhet(any()) } returns opprettEnhet()
        coEvery { navansattMock.hentSaksbehandlerInfo(any()) }
            .returns(opprettSaksbehandlerInfo(zIdent, "saks", "behandler"))

        val sakId = Random.nextLong()
        val sak = Sak("ident", SakType.BARNEPENSJON, sakId, "enhet")

        val faktiskAvsender =
            runBlocking {
                adresseService.hentAvsender(sak, zIdent)
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
            enhetNr = ANSVARLIG_ENHET,
            kontaktinfo =
                Norg2Kontaktinfo(
                    telefonnummer = "00 11 22 33",
                    epost = "test@nav.no",
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
        private const val ANSVARLIG_ENHET = "1234"
        private const val SAKSBEHANDLER = "Z123456"
        private const val ATTESTANT = "Z00002"
    }
}
