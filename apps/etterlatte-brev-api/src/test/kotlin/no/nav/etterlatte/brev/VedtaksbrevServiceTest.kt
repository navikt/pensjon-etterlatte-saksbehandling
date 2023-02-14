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
import no.nav.etterlatte.brev.adresse.Norg2Enhet
import no.nav.etterlatte.brev.behandling.Attestant
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
import no.nav.etterlatte.brev.model.InnvilgetBrevRequest
import no.nav.etterlatte.brev.pdf.PdfGeneratorKlient
import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.brev.model.Status
import no.nav.etterlatte.libs.common.journalpost.JournalpostResponse
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

internal class VedtaksbrevServiceTest {

    private val db = mockk<BrevRepository>(relaxed = true)
    private val pdfGenerator = mockk<PdfGeneratorKlient>()
    private val sakOgBehandlingService = mockk<SakOgBehandlingService>()
    private val adresseService = mockk<AdresseService>()
    private val dokarkivService = mockk<DokarkivServiceImpl>()
    private val ident = "Z1234"
    private val saksbehandlerNavn = "Sak Saksbehandler"

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
    inner class OppdaterVedtaksbrev {
        @Test
        fun `Opprett vedtaksbrev hvis ikke finnes`() {
            every { db.hentBrevForBehandling(any()) } returns listOf(
                Brev(1, BEHANDLING_ID, "fnr", "tittel", Status.OPPRETTET, Mottaker(STOR_SNERK), false),
                Brev(2, BEHANDLING_ID, "fnr", "tittel", Status.OPPDATERT, Mottaker(STOR_SNERK), false),
                Brev(3, BEHANDLING_ID, "fnr", "tittel", Status.FERDIGSTILT, Mottaker(STOR_SNERK), false),
                Brev(4, BEHANDLING_ID, "fnr", "tittel", Status.JOURNALFOERT, Mottaker(STOR_SNERK), false),
                Brev(5, BEHANDLING_ID, "fnr", "tittel", Status.DISTRIBUERT, Mottaker(STOR_SNERK), false)
            )

            val behandling = opprettBehandling()
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any(), any()) } returns behandling
            coEvery { adresseService.hentMottakerAdresse(any()) } returns opprettMottaker()
            coEvery { pdfGenerator.genererPdf(any()) } returns "".toByteArray()
            coEvery { adresseService.hentAvsenderOgAttestant(any()) } returns Pair(mockk(), mockk())

            runBlocking {
                vedtaksbrevService.oppdaterVedtaksbrev(SAK_ID, BEHANDLING_ID, "saksbehandler", "token")
            }

            verify(exactly = 1) {
                db.opprettBrev(any())
                db.hentBrevForBehandling(any())
            }
            coVerify(exactly = 1) {
                pdfGenerator.genererPdf(any<InnvilgetBrevRequest>())
                sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, any(), any())
                adresseService.hentAvsenderOgAttestant(behandling.vedtak)
                adresseService.hentMottakerAdresse(behandling.persongalleri.innsender.fnr)
            }

            verify { dokarkivService wasNot Called }
        }

        @Test
        fun `Oppdatering av vedtaksbrev som finnes`() {
            every { db.hentBrevForBehandling(any()) } returns listOf(
                Brev(1, BEHANDLING_ID, "fnr", "tittel", Status.OPPRETTET, Mottaker(STOR_SNERK), false),
                Brev(2, BEHANDLING_ID, "fnr", "tittel", Status.OPPDATERT, Mottaker(STOR_SNERK), true),
                Brev(3, BEHANDLING_ID, "fnr", "tittel", Status.FERDIGSTILT, Mottaker(STOR_SNERK), false),
                Brev(4, BEHANDLING_ID, "fnr", "tittel", Status.JOURNALFOERT, Mottaker(STOR_SNERK), false),
                Brev(5, BEHANDLING_ID, "fnr", "tittel", Status.DISTRIBUERT, Mottaker(STOR_SNERK), false)
            )

            val behandling = opprettBehandling()
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsenderOgAttestant(any()) } returns Pair(mockk(), mockk())
            coEvery { adresseService.hentMottakerAdresse(any()) } returns opprettMottaker()
            coEvery { pdfGenerator.genererPdf(any()) } returns "".toByteArray()
            coEvery { adresseService.hentEnhet(any()) } returns Norg2Enhet()

            runBlocking {
                val brev = vedtaksbrevService.oppdaterVedtaksbrev(SAK_ID, BEHANDLING_ID, "saksbehandler", "token")

                assertNotNull(brev)
            }

            verify(exactly = 1) {
                db.oppdaterBrev(2, any())
                db.hentBrevForBehandling(any())
            }
            coVerify(exactly = 1) {
                pdfGenerator.genererPdf(any<InnvilgetBrevRequest>())
                sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, any(), any())
                adresseService.hentAvsenderOgAttestant(any<ForenkletVedtak>())
                adresseService.hentMottakerAdresse(behandling.persongalleri.innsender.fnr)
            }

            verify { dokarkivService wasNot Called }
        }

        @Test
        fun `Ferdigstilt vedtaksbrev skal returnere uendret brev`() {
            every { db.hentBrevForBehandling(any()) } returns listOf(
                Brev(
                    1,
                    BEHANDLING_ID,
                    "fnr",
                    "tittel",
                    Status.FERDIGSTILT,
                    Mottaker(STOR_SNERK),
                    true
                )
            )

            runBlocking {
                vedtaksbrevService.oppdaterVedtaksbrev(123, BEHANDLING_ID, "saksbehandler", "token")
            }

            verify(exactly = 1) { db.hentBrevForBehandling(BEHANDLING_ID) }
            verify(exactly = 0) {
                db.oppdaterBrev(any(), any())
                db.opprettBrev(any())
            }
        }
    }

    @Nested
    inner class FerdigstillVedtaksbrev {

        @ParameterizedTest
        @EnumSource(Status::class, names = ["OPPRETTET", "OPPDATERT"])
        fun `Ferdigstille vedtaksbrev med gyldig status`(status: Status) {
            every { db.hentBrevForBehandling(any()) } returns listOf(
                Brev(1, BEHANDLING_ID, "fnr", "tittel", Status.OPPDATERT, Mottaker(STOR_SNERK), false),
                Brev(2, BEHANDLING_ID, "fnr", "tittel", status, Mottaker(STOR_SNERK), true),
                Brev(3, BEHANDLING_ID, "fnr", "tittel", Status.FERDIGSTILT, Mottaker(STOR_SNERK), false)
            )
            every { db.oppdaterStatus(any(), any()) } returns true

            runBlocking {
                val ferdigstiltOK = vedtaksbrevService.ferdigstillVedtaksbrev(BEHANDLING_ID)

                Assertions.assertTrue(ferdigstiltOK)
            }

            verify(exactly = 1) {
                db.hentBrevForBehandling(BEHANDLING_ID)
                db.oppdaterStatus(2, Status.FERDIGSTILT)
            }

            verify {
                listOf(pdfGenerator, sakOgBehandlingService, adresseService, dokarkivService)
                    .wasNot(Called)
            }
        }

        @Test
        fun `Vedtaksbrev er allerede ferdigstilt`() {
            every { db.hentBrevForBehandling(any()) } returns listOf(
                Brev(1, BEHANDLING_ID, "fnr", "tittel", Status.OPPDATERT, Mottaker(STOR_SNERK), false),
                Brev(2, BEHANDLING_ID, "fnr", "tittel", Status.FERDIGSTILT, Mottaker(STOR_SNERK), true),
                Brev(3, BEHANDLING_ID, "fnr", "tittel", Status.JOURNALFOERT, Mottaker(STOR_SNERK), false)
            )

            runBlocking {
                val ferdigstiltOK = vedtaksbrevService.ferdigstillVedtaksbrev(BEHANDLING_ID)

                Assertions.assertTrue(ferdigstiltOK)
            }

            verify(exactly = 1) { db.hentBrevForBehandling(BEHANDLING_ID) }
            verify(exactly = 0) { db.oppdaterStatus(any(), any()) }

            verify {
                listOf(pdfGenerator, sakOgBehandlingService, adresseService, dokarkivService)
                    .wasNot(Called)
            }
        }

        @ParameterizedTest
        @EnumSource(
            Status::class,
            mode = EnumSource.Mode.EXCLUDE,
            names = ["OPPRETTET", "OPPDATERT", "FERDIGSTILT", "JOURNALFOERT"]
        )
        fun `Ferdigstille vedtaksbrev`(status: Status) {
            every { db.hentBrevForBehandling(any()) } returns listOf(
                Brev(1, BEHANDLING_ID, "fnr", "tittel", Status.OPPDATERT, Mottaker(STOR_SNERK), false),
                Brev(2, BEHANDLING_ID, "fnr", "tittel", status, Mottaker(STOR_SNERK), true),
                Brev(3, BEHANDLING_ID, "fnr", "tittel", Status.FERDIGSTILT, Mottaker(STOR_SNERK), false)
            )

            runBlocking {
                assertThrows<IllegalArgumentException> {
                    vedtaksbrevService.ferdigstillVedtaksbrev(BEHANDLING_ID)
                }
            }

            verify(exactly = 1) { db.hentBrevForBehandling(BEHANDLING_ID) }
            verify(exactly = 0) { db.oppdaterStatus(any(), any()) }

            verify {
                listOf(pdfGenerator, sakOgBehandlingService, adresseService, dokarkivService)
                    .wasNot(Called)
            }
        }
    }

    @Nested
    inner class JournalfoerVedtaksbrev {

        @Test
        fun `Vedtaksbrev journalfoeres som forventet`() {
            val forventetBrev = Brev(2, BEHANDLING_ID, "fnr", "tittel", Status.FERDIGSTILT, Mottaker(STOR_SNERK), true)

            every { db.hentBrevForBehandling(any()) } returns listOf(
                forventetBrev,
                Brev(1, BEHANDLING_ID, "fnr", "tittel", Status.OPPDATERT, Mottaker(STOR_SNERK), false),
                Brev(3, BEHANDLING_ID, "fnr", "tittel", Status.JOURNALFOERT, Mottaker(STOR_SNERK), false)
            )

            val forventetResponse = JournalpostResponse("1", "OK", "melding", true)
            every { dokarkivService.journalfoer(any(), any()) } returns forventetResponse

            val vedtak = opprettVedtak()

            runBlocking {
                val (brev, response) = vedtaksbrevService.journalfoerVedtaksbrev(vedtak)

                assertEquals(forventetBrev, brev)
                assertEquals(forventetResponse, response)
            }

            verify(exactly = 1) {
                db.hentBrevForBehandling(BEHANDLING_ID)
                dokarkivService.journalfoer(forventetBrev, vedtak)
                db.oppdaterStatus(forventetBrev.id, Status.JOURNALFOERT, any())
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
            every { db.hentBrevForBehandling(any()) } returns listOf(
                Brev(1, BEHANDLING_ID, "fnr", "tittel", Status.OPPDATERT, Mottaker(STOR_SNERK), false),
                Brev(2, BEHANDLING_ID, "fnr", "tittel", status, Mottaker(STOR_SNERK), true),
                Brev(3, BEHANDLING_ID, "fnr", "tittel", Status.FERDIGSTILT, Mottaker(STOR_SNERK), false)
            )

            runBlocking {
                assertThrows<IllegalArgumentException> {
                    vedtaksbrevService.journalfoerVedtaksbrev(opprettVedtak())
                }
            }

            verify(exactly = 1) { db.hentBrevForBehandling(BEHANDLING_ID) }
            verify(exactly = 0) { db.oppdaterStatus(any(), any()) }

            verify {
                listOf(pdfGenerator, sakOgBehandlingService, adresseService, dokarkivService)
                    .wasNot(Called)
            }
        }
    }

    private fun opprettVedtak() = mockk<Vedtak> {
        every { behandling.id } returns BEHANDLING_ID
        every { sak.ident } returns "ident"
        every { vedtakFattet } returns VedtakFattet("Z12345", "ansvarlig enhet", ZonedDateTime.now())
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
        ForenkletVedtak(1, VedtakType.INNVILGELSE, Saksbehandler(ident, PORSGRUNN), Attestant(ident, PORSGRUNN)),
        Utbetalingsinfo(
            null,
            3436,
            LocalDate.now(),
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
        private val BEHANDLING_ID = UUID.randomUUID()
        private val STOR_SNERK = Foedselsnummer.of("11057523044")
        private const val PORSGRUNN = "0805"
    }
}