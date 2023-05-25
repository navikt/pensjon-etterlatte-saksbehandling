package no.nav.etterlatte.brev

import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.Innsender
import no.nav.etterlatte.brev.behandling.Persongalleri
import no.nav.etterlatte.brev.behandling.SakOgBehandlingService
import no.nav.etterlatte.brev.behandling.Saksbehandler
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.pdf.PdfGeneratorKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import no.nav.etterlatte.token.Bruker
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.util.*
import kotlin.random.Random

internal class VedtaksbrevServiceTest {

    private val db = mockk<BrevRepository>(relaxed = true)
    private val pdfGenerator = mockk<PdfGeneratorKlient>()
    private val sakOgBehandlingService = mockk<SakOgBehandlingService>()
    private val adresseService = mockk<AdresseService>()
    private val dokarkivService = mockk<DokarkivServiceImpl>()

    private val vedtaksbrevService =
        VedtaksbrevService(db, pdfGenerator, sakOgBehandlingService, adresseService, dokarkivService)

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(db, pdfGenerator, sakOgBehandlingService, adresseService, dokarkivService)
    }

    @Nested
    inner class GenerellBrevTest {
        @Test
        fun `Hent brev med ID`() {
            val forventetBrev = opprettBrev()
            every { db.hentBrev(any()) } returns forventetBrev

            val brev = vedtaksbrevService.hentBrev(1)

            brev shouldBe forventetBrev

            verify(exactly = 1) { db.hentBrev(1) }
        }

        @Test
        fun `Hent vedtaksbrev med behandling id`() {
            val forventetBrev = opprettBrev()
            every { db.hentBrevForBehandling(any()) } returns forventetBrev

            val brev = vedtaksbrevService.hentVedtaksbrev(BEHANDLING_ID)

            brev shouldBe forventetBrev

            verify(exactly = 1) { db.hentBrevForBehandling(BEHANDLING_ID) }
        }

        @Test
        fun `Sletting av brev`() {
            every { db.slett(any()) } returns true

            val slettetOK = vedtaksbrevService.slettVedtaksbrev(1)

            slettetOK shouldBe true

            verify { db.slett(1) }
        }
    }

    @Nested
    inner class OpprettVedtaksbrevTest {
        @Test
        fun `Vedtaksbrev finnes ikke - skal opprettes nytt`() {
            val behandling = opprettBehandling()
            val mottaker = opprettMottaker()

            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentMottakerAdresse(any()) } returns mottaker

            runBlocking {
                vedtaksbrevService.opprettVedtaksbrev(
                    SAK_ID,
                    BEHANDLING_ID,
                    ATTESTANT
                )
            }

            val brevSlot = slot<OpprettNyttBrev>()

            coVerify {
                db.hentBrevForBehandling(BEHANDLING_ID)
                sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, any())
                adresseService.hentMottakerAdresse(behandling.persongalleri.innsender.fnr)
            }

            verify {
                db.opprettBrev(capture(brevSlot))
                pdfGenerator wasNot Called
                dokarkivService wasNot Called
            }

            val brev = brevSlot.captured
            brev.behandlingId shouldBe behandling.behandlingId
            brev.soekerFnr shouldBe behandling.persongalleri.soeker.fnr
            brev.tittel shouldBe "Vedtak om ${behandling.vedtak.type.name.lowercase()}"
            brev.mottaker shouldBe mottaker
            brev.erVedtaksbrev shouldBe true
        }

        @Test
        fun `Vedtaksbrev finnes allerede - skal kaste feil`() {
            every { db.hentBrevForBehandling(any()) } returns opprettBrev()

            val behandling = opprettBehandling()
            val mottaker = opprettMottaker()

            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentMottakerAdresse(any()) } returns mottaker

            assertThrows<IllegalArgumentException> {
                runBlocking {
                    vedtaksbrevService.opprettVedtaksbrev(
                        SAK_ID,
                        BEHANDLING_ID,
                        SAKSBEHANDLER
                    )
                }
            }

            coVerify {
                db.hentBrevForBehandling(BEHANDLING_ID)
                sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, any())
                adresseService.hentMottakerAdresse(behandling.persongalleri.innsender.fnr)
            }

            verify {
                pdfGenerator wasNot Called
                dokarkivService wasNot Called
            }
        }
    }

    @Nested
    inner class BrevInnholdTest {
        @Test
        fun `PDF genereres uten lagring`() {
            val behandling = opprettBehandling(VedtakStatus.OPPRETTET)

            every { db.hentBrevForBehandling(any()) } returns opprettBrev(Status.OPPRETTET)
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsenderOgAttestant(any()) } returns Pair(mockk(), mockk())
            coEvery { pdfGenerator.genererPdf(any()) } returns PDF_BYTES

            runBlocking {
                vedtaksbrevService.genererPdfInnhold(SAK_ID, BEHANDLING_ID, bruker = SAKSBEHANDLER)
            }

            verify {
                db.hentBrevForBehandling(BEHANDLING_ID)
            }

            coVerify {
                sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, SAKSBEHANDLER)
                adresseService.hentAvsenderOgAttestant(any())
                pdfGenerator.genererPdf(any())
            }
        }

        @Test
        fun `PDF genereres, lagres, og ferdigstilles hvis vedtak er fattet`() {
            val behandling = opprettBehandling(VedtakStatus.FATTET_VEDTAK)

            every { db.hentBrevForBehandling(any()) } returns opprettBrev(Status.OPPRETTET)
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsenderOgAttestant(any()) } returns Pair(mockk(), mockk())
            coEvery { pdfGenerator.genererPdf(any()) } returns PDF_BYTES

            runBlocking {
                vedtaksbrevService.genererPdfInnhold(SAK_ID, BEHANDLING_ID, bruker = ATTESTANT)
            }

            verify {
                db.hentBrevForBehandling(BEHANDLING_ID)
                db.opprettInnholdOgFerdigstill(any(), any())
            }

            coVerify {
                sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, ATTESTANT)
                adresseService.hentAvsenderOgAttestant(any())
                pdfGenerator.genererPdf(any())
            }
        }

        @Test
        fun `PDF genereres, men lagres ikke hvis saksbehanler sjekker sin egen sak med FATTET_VEDTAK`() {
            val behandling = opprettBehandling(VedtakStatus.FATTET_VEDTAK)

            every { db.hentBrevForBehandling(any()) } returns opprettBrev(Status.OPPRETTET)
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsenderOgAttestant(any()) } returns Pair(mockk(), mockk())
            coEvery { pdfGenerator.genererPdf(any()) } returns PDF_BYTES

            runBlocking {
                vedtaksbrevService.genererPdfInnhold(SAK_ID, BEHANDLING_ID, bruker = SAKSBEHANDLER)
            }

            verify {
                db.hentBrevForBehandling(BEHANDLING_ID)
            }

            coVerify {
                sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, SAKSBEHANDLER)
                adresseService.hentAvsenderOgAttestant(any())
                pdfGenerator.genererPdf(any())
            }
        }

        @ParameterizedTest
        @EnumSource(
            Status::class,
            mode = EnumSource.Mode.EXCLUDE,
            names = ["OPPRETTET", "OPPDATERT"]
        )
        fun `Brev innhold kan ikke endres`(status: Status) {
            val brev = opprettBrev(status)

            every { db.hentBrevForBehandling(any()) } returns brev
            every { db.hentBrevInnhold(any()) } returns BrevInnhold(Spraak.NB, PDF_BYTES)

            runBlocking {
                vedtaksbrevService.genererPdfInnhold(SAK_ID, BEHANDLING_ID, bruker = SAKSBEHANDLER)
            }

            verify {
                db.hentBrevForBehandling(BEHANDLING_ID)
                db.hentBrevInnhold(brev.id)
            }

            coVerify {
                sakOgBehandlingService wasNot Called
                adresseService wasNot Called
                pdfGenerator wasNot Called
            }
        }
    }

    @Nested
    inner class JournalfoerVedtaksbrev {
        @Test
        fun `Vedtaksbrev journalfoeres som forventet`() {
            val forventetBrev = opprettBrev(Status.FERDIGSTILT)

            val forventetResponse = JournalpostResponse("1", "OK", "melding", true)
            every { dokarkivService.journalfoer(any(), any()) } returns forventetResponse

            val vedtak = opprettVedtak()

            val (brev, response) = runBlocking {
                vedtaksbrevService.journalfoerVedtaksbrev(forventetBrev, vedtak)
            }

            assertEquals(forventetBrev, brev)
            assertEquals(forventetResponse, response)

            verify(exactly = 1) {
                dokarkivService.journalfoer(forventetBrev, vedtak)
                db.settBrevJournalfoert(forventetBrev.id, response)
            }

            verify {
                listOf(pdfGenerator, sakOgBehandlingService, adresseService) wasNot Called
            }
        }

        @ParameterizedTest
        @EnumSource(
            Status::class,
            mode = EnumSource.Mode.EXCLUDE,
            names = ["FERDIGSTILT"]
        )
        fun `Journalfoering av brev med ugyldig status`(status: Status) {
            val brev = Brev(Random.nextLong(), BEHANDLING_ID, "fnr", "tittel", status, opprettMottaker(), true)

            runBlocking {
                assertThrows<IllegalArgumentException> {
                    vedtaksbrevService.journalfoerVedtaksbrev(brev, opprettVedtak())
                }
            }

            verify(exactly = 0) { db.settBrevFerdigstilt(any()) }

            verify {
                listOf(pdfGenerator, sakOgBehandlingService, adresseService, dokarkivService)
                    .wasNot(Called)
            }
        }
    }

    private fun opprettBrev(status: Status = Status.OPPRETTET) = Brev(
        id = Random.nextLong(10000),
        behandlingId = BEHANDLING_ID,
        soekerFnr = "fnr",
        tittel = "tittel",
        status = status,
        mottaker = opprettMottaker(),
        erVedtaksbrev = true
    )

    private fun opprettVedtak() = VedtakTilJournalfoering(
        vedtakId = 1234,
        sak = VedtakSak("ident", SakType.BARNEPENSJON, 4),
        behandlingId = BEHANDLING_ID,
        ansvarligEnhet = "ansvarlig enhet"
    )

    private fun opprettBehandling(vedtakStatus: VedtakStatus = VedtakStatus.OPPRETTET) = Behandling(
        SAK_ID,
        SakType.BARNEPENSJON,
        BEHANDLING_ID,
        Spraak.NB,
        Persongalleri(
            Innsender("STOR SNERK", "11057523044"),
            Soeker("GRØNN KOPP", "12345"),
            Avdoed("DØD TESTPERSON", LocalDate.now().minusMonths(1))
        ),
        ForenkletVedtak(
            1,
            vedtakStatus,
            VedtakType.INNVILGELSE,
            Saksbehandler(SAKSBEHANDLER.ident(), PORSGRUNN),
            attestant = null
        ),
        Utbetalingsinfo(
            null,
            3436,
            LocalDate.now(),
            false,
            listOf(Beregningsperiode(LocalDate.now(), LocalDate.now().plusYears(4), 120000, 1, 5000, 40))
        )
    )

    private fun opprettMottaker() = Mottaker(
        "Stor Snerk",
        foedselsnummer = STOR_SNERK,
        orgnummer = null,
        adresse = Adresse(
            adresseType = "NORSKPOSTADRESSE",
            adresselinje1 = "Testgaten 13",
            postnummer = "1234",
            poststed = "OSLO",
            land = "Norge",
            landkode = "NOR"
        )
    )

    private companion object {
        private const val SAK_ID = 123L
        private val BEHANDLING_ID = UUID.randomUUID()
        private val STOR_SNERK = Folkeregisteridentifikator.of("11057523044")
        private const val PORSGRUNN = "0805"
        private val PDF_BYTES = "Hello world!".toByteArray()
        private val SAKSBEHANDLER = Bruker.of("token", "saksbehandler", null, null, null)
        private val ATTESTANT = Bruker.of("token", "attestant", null, null, null)
    }
}