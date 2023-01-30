package no.nav.etterlatte.brev

import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.Persongalleri
import no.nav.etterlatte.brev.behandling.SakOgBehandlingService
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Avdoed
import no.nav.etterlatte.brev.model.Avsender
import no.nav.etterlatte.brev.model.Innsender
import no.nav.etterlatte.brev.model.InnvilgetBrevRequest
import no.nav.etterlatte.brev.model.SaksbehandlerInfo
import no.nav.etterlatte.brev.model.Soeker
import no.nav.etterlatte.brev.navansatt.NavansattKlient
import no.nav.etterlatte.brev.pdf.PdfGeneratorKlient
import no.nav.etterlatte.journalpost.DokarkivServiceImpl
import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.brev.model.Status
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

internal class VedtaksbrevServiceTest {

    private val db = mockk<BrevRepository>(relaxed = true)
    private val pdfGenerator = mockk<PdfGeneratorKlient>()
    private val sakOgBehandlingService = mockk<SakOgBehandlingService>()
    private val adresseService = mockk<AdresseService>()
    private val dokarkivService = mockk<DokarkivServiceImpl>()
    private val navansattKlient = mockk<NavansattKlient>()
    private val ident = "Z1234"

    private val vedtaksbrevService =
        VedtaksbrevService(db, pdfGenerator, sakOgBehandlingService, adresseService, dokarkivService, navansattKlient)

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(db, pdfGenerator, sakOgBehandlingService, adresseService, dokarkivService, navansattKlient)
    }

    @Test
    fun `Opprett vedtaksbrev hvis ikke finnes`() {
        every { db.hentBrevForBehandling(any()) } returns listOf(
            Brev(1, BEHANDLING_ID, "tittel", Status.OPPRETTET, Mottaker(STOR_SNERK), false),
            Brev(2, BEHANDLING_ID, "tittel", Status.OPPDATERT, Mottaker(STOR_SNERK), false),
            Brev(3, BEHANDLING_ID, "tittel", Status.FERDIGSTILT, Mottaker(STOR_SNERK), false),
            Brev(4, BEHANDLING_ID, "tittel", Status.JOURNALFOERT, Mottaker(STOR_SNERK), false),
            Brev(5, BEHANDLING_ID, "tittel", Status.DISTRIBUERT, Mottaker(STOR_SNERK), false)
        )

        val behandling = opprettBehandling()
        coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any(), any()) } returns behandling
        coEvery {
            adresseService.hentAvsenderEnhet(any(), any())
        } returns Avsender("Porsgrunn", "adresse", "postnr", "telefon")
        coEvery { adresseService.hentMottakerAdresse(any()) } returns opprettMottaker()
        coEvery { pdfGenerator.genererPdf(any()) } returns "".toByteArray()
        coEvery { navansattKlient.HentSaksbehandlerInfo(any()) } returns SaksbehandlerInfo(
            ident,
            "Sak Saksbehandler",
            "Sak",
            "Saksbehandler",
            "sak@nav.no"
        )
        coEvery { adresseService.hentEnhet(any()) } returns ""

        runBlocking {
            vedtaksbrevService.oppdaterVedtaksbrev(SAK_ID, BEHANDLING_ID, "saksbehandler", "token")
        }

        verify(exactly = 1) { db.opprettBrev(any()) }
        verify(exactly = 1) { db.hentBrevForBehandling(any()) }
        coVerify(exactly = 1) { pdfGenerator.genererPdf(any<InnvilgetBrevRequest>()) }
        coVerify(exactly = 1) { sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, any(), any()) }
        coVerify(exactly = 1) { adresseService.hentAvsenderEnhet(PORSGRUNN, "Sak Saksbehandler") }
        coVerify(exactly = 1) { adresseService.hentMottakerAdresse(behandling.persongalleri.innsender.fnr) }
        coVerify(exactly = 1) { adresseService.hentEnhet(PORSGRUNN) }
        coVerify(exactly = 1) { navansattKlient.HentSaksbehandlerInfo(ident) }

        verify { dokarkivService wasNot Called }
    }

    @Test
    fun `Oppdatering av vedtaksbrev som finnes`() {
        every { db.hentBrevForBehandling(any()) } returns listOf(
            Brev(1, BEHANDLING_ID, "tittel", Status.OPPRETTET, Mottaker(STOR_SNERK), false),
            Brev(2, BEHANDLING_ID, "tittel", Status.OPPDATERT, Mottaker(STOR_SNERK), true),
            Brev(3, BEHANDLING_ID, "tittel", Status.FERDIGSTILT, Mottaker(STOR_SNERK), false),
            Brev(4, BEHANDLING_ID, "tittel", Status.JOURNALFOERT, Mottaker(STOR_SNERK), false),
            Brev(5, BEHANDLING_ID, "tittel", Status.DISTRIBUERT, Mottaker(STOR_SNERK), false)
        )

        val behandling = opprettBehandling()
        coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any(), any()) } returns behandling
        coEvery {
            adresseService.hentAvsenderEnhet(any(), any())
        } returns Avsender("Porsgrunn", "adresse", "postnr", "telefon")
        coEvery { adresseService.hentMottakerAdresse(any()) } returns opprettMottaker()
        coEvery { pdfGenerator.genererPdf(any()) } returns "".toByteArray()
        coEvery { navansattKlient.HentSaksbehandlerInfo(any()) } returns SaksbehandlerInfo(
            ident,
            "Sak Saksbehandler",
            "Sak",
            "Saksbehandler",
            "sak@nav.no"
        )
        coEvery { adresseService.hentEnhet(any()) } returns ""

        runBlocking {
            val brevID = vedtaksbrevService.oppdaterVedtaksbrev(SAK_ID, BEHANDLING_ID, "saksbehandler", "token")

            assertEquals(2, brevID)
        }

        verify(exactly = 1) { db.oppdaterBrev(2, any()) }
        verify(exactly = 1) { db.hentBrevForBehandling(any()) }
        coVerify(exactly = 1) { pdfGenerator.genererPdf(any<InnvilgetBrevRequest>()) }
        coVerify(exactly = 1) { sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, any(), any()) }
        coVerify(exactly = 1) { adresseService.hentAvsenderEnhet(PORSGRUNN, "Sak Saksbehandler") }
        coVerify(exactly = 1) { adresseService.hentMottakerAdresse(behandling.persongalleri.innsender.fnr) }
        coVerify(exactly = 1) { adresseService.hentEnhet(PORSGRUNN) }
        coVerify(exactly = 1) { navansattKlient.HentSaksbehandlerInfo(ident) }

        verify { dokarkivService wasNot Called }
    }

    @Test
    fun `Ferdigstilt vedtaksbrev skal kaste exception ved endringsforsøk`() {
        every { db.hentBrevForBehandling(any()) } returns listOf(
            Brev(
                1,
                BEHANDLING_ID,
                "tittel",
                Status.FERDIGSTILT,
                Mottaker(STOR_SNERK),
                true
            )
        )

        runBlocking {
            assertThrows<IllegalArgumentException> {
                vedtaksbrevService.oppdaterVedtaksbrev(123, BEHANDLING_ID, "saksbehandler", "token")
            }
        }

        verify(exactly = 1) { db.hentBrevForBehandling(BEHANDLING_ID) }
    }

    private fun opprettBehandling() = Behandling(
        SAK_ID,
        BEHANDLING_ID,
        Spraak.NB,
        Persongalleri(
            Innsender("STOR SNERK", "11057523044"),
            Soeker("GRØNN KOPP", "12345"),
            Avdoed("DØD TESTPERSON", LocalDate.now().minusMonths(1))
        ),
        ForenkletVedtak(1, VedtakType.INNVILGELSE, PORSGRUNN, ident, PORSGRUNN, ident),
        Utbetalingsinfo(
            false,
            listOf(Beregningsperiode(LocalDate.now(), LocalDate.now().plusYears(4), 120000, 1, 5000, 40))
        )
    )

    private fun opprettMottaker() = no.nav.etterlatte.brev.model.Mottaker(
        "navn",
        "adresse",
        "1234",
        "poststed",
        "land"
    )

    private companion object {
        private val SAK_ID = 123L
        private val BEHANDLING_ID = UUID.randomUUID().toString()
        private val STOR_SNERK = Foedselsnummer.of("11057523044")
        private const val PORSGRUNN = "0805"
    }
}