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
import no.nav.etterlatte.brev.brevbaker.BrevbakerKlient
import no.nav.etterlatte.brev.brevbaker.BrevbakerPdfResponse
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Attestant
import no.nav.etterlatte.brev.model.Avsender
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import no.nav.etterlatte.token.Bruker
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.Kroner
import no.nav.pensjon.brevbaker.api.model.LetterMetadata
import no.nav.pensjon.brevbaker.api.model.Telefonnummer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.util.*
import kotlin.random.Random

internal class VedtaksbrevServiceTest {

    private val db = mockk<BrevRepository>(relaxed = true)
    private val brevbaker = mockk<BrevbakerKlient>()
    private val sakOgBehandlingService = mockk<SakOgBehandlingService>()
    private val adresseService = mockk<AdresseService>()
    private val dokarkivService = mockk<DokarkivServiceImpl>()

    private val vedtaksbrevService =
        VedtaksbrevService(db, sakOgBehandlingService, adresseService, dokarkivService, brevbaker)

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(db, sakOgBehandlingService, adresseService, dokarkivService, brevbaker)
    }

    @Nested
    inner class GenerellBrevTest {
        @Test
        fun `Hent brev med ID`() {
            val forventetBrev = opprettBrev(mockk(), mockk())
            every { db.hentBrev(any()) } returns forventetBrev

            val brev = vedtaksbrevService.hentBrev(1)

            brev shouldBe forventetBrev

            verify(exactly = 1) { db.hentBrev(1) }
        }

        @Test
        fun `Hent vedtaksbrev med behandling id`() {
            val forventetBrev = opprettBrev(mockk(), mockk())
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

        @ParameterizedTest
        @CsvSource(
            value = [
                "OMSTILLINGSSTOENAD,INNVILGELSE,MANUELL",
                "OMSTILLINGSSTOENAD,OPPHOER,MANUELL",
                "OMSTILLINGSSTOENAD,AVSLAG,MANUELL",
                "OMSTILLINGSSTOENAD,ENDRING,MANUELL",
                "BARNEPENSJON,INNVILGELSE,AUTOMATISK",
                "BARNEPENSJON,OPPHOER,MANUELL",
                "BARNEPENSJON,AVSLAG,MANUELL",
                "BARNEPENSJON,ENDRING,MANUELL"
            ]
        )
        fun `Vedtaksbrev finnes ikke - skal opprettes nytt`(
            sakType: SakType,
            vedtakType: VedtakType,
            forventetProsessType: BrevProsessType
        ) {
            val behandling = opprettBehandling(sakType, vedtakType)
            val mottaker = opprettMottaker()

            every { db.hentBrevForBehandling(behandling.behandlingId) } returns null
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
                adresseService.hentMottakerAdresse(behandling.persongalleri.innsender.fnr.value)
            }

            verify {
                db.opprettBrev(capture(brevSlot))
                brevbaker wasNot Called
                dokarkivService wasNot Called
            }

            val brev = brevSlot.captured
            brev.behandlingId shouldBe behandling.behandlingId
            brev.soekerFnr shouldBe behandling.persongalleri.soeker.fnr.value
            brev.tittel shouldBe "Vedtak om ${behandling.vedtak.type.name.lowercase()}"
            brev.mottaker shouldBe mottaker
            brev.prosessType shouldBe forventetProsessType
        }

        @Test
        fun `Vedtaksbrev finnes allerede - skal kaste feil`() {
            every { db.hentBrevForBehandling(any()) } returns opprettBrev(Status.OPPRETTET, BrevProsessType.AUTOMATISK)

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
//                sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, any())
//                adresseService.hentMottakerAdresse(behandling.persongalleri.innsender.fnr.value)
            }

            verify {
                db.hentBrevForBehandling(BEHANDLING_ID)

                brevbaker wasNot Called
                adresseService wasNot Called
                dokarkivService wasNot Called
                sakOgBehandlingService wasNot Called
            }
        }
    }

    @Nested
    inner class BrevInnholdTest {
        @Test
        fun `AUTOMATISK - PDF genereres uten lagring`() {
            val behandling = opprettBehandling(SakType.BARNEPENSJON, VedtakType.INNVILGELSE)

            every { db.hentBrevForBehandling(any()) } returns opprettBrev(Status.OPPRETTET, BrevProsessType.AUTOMATISK)
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsenderOgAttestant(any()) } returns opprettAvsenderOgAttestant()
            coEvery { brevbaker.genererPdf(any()) } returns opprettBrevbakerResponse()

            runBlocking {
                vedtaksbrevService.genererPdf(SAK_ID, BEHANDLING_ID, bruker = SAKSBEHANDLER)
            }

            verify {
                db.hentBrevForBehandling(BEHANDLING_ID)
            }

            coVerify {
                sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, SAKSBEHANDLER)
                adresseService.hentAvsenderOgAttestant(any())
                brevbaker.genererPdf(any())
            }
        }

        @Test
        fun `PDF genereres, lagres, og ferdigstilles hvis vedtak er fattet`() {
            val behandling = opprettBehandling(SakType.BARNEPENSJON, VedtakType.INNVILGELSE, VedtakStatus.FATTET_VEDTAK)

            every { db.hentBrevForBehandling(any()) } returns opprettBrev(Status.OPPRETTET, BrevProsessType.AUTOMATISK)
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsenderOgAttestant(any()) } returns opprettAvsenderOgAttestant()
            coEvery { brevbaker.genererPdf(any()) } returns opprettBrevbakerResponse()

            runBlocking {
                vedtaksbrevService.genererPdf(SAK_ID, BEHANDLING_ID, bruker = ATTESTANT)
            }

            verify {
                db.hentBrevForBehandling(BEHANDLING_ID)
                db.opprettInnholdOgFerdigstill(any(), any())
            }

            coVerify {
                sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, ATTESTANT)
                adresseService.hentAvsenderOgAttestant(any())
                brevbaker.genererPdf(any())
            }
        }

        @Test
        fun `AUTOMATISK - PDF genereres, men lagres ikke hvis saksbehandler sjekker sin egen sak med FATTET_VEDTAK`() {
            val behandling = opprettBehandling(SakType.BARNEPENSJON, VedtakType.INNVILGELSE, VedtakStatus.FATTET_VEDTAK)

            every { db.hentBrevForBehandling(any()) } returns opprettBrev(Status.OPPRETTET, BrevProsessType.AUTOMATISK)
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsenderOgAttestant(any()) } returns opprettAvsenderOgAttestant()
            coEvery { brevbaker.genererPdf(any()) } returns opprettBrevbakerResponse()

            runBlocking {
                vedtaksbrevService.genererPdf(SAK_ID, BEHANDLING_ID, bruker = SAKSBEHANDLER)
            }

            verify {
                db.hentBrevForBehandling(BEHANDLING_ID)
            }

            coVerify {
                sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, SAKSBEHANDLER)
                adresseService.hentAvsenderOgAttestant(any())
                brevbaker.genererPdf(any())
            }
        }

        @Test
        fun `MANUELL - PDF genereres uten lagring`() {
            val behandling =
                opprettBehandling(SakType.OMSTILLINGSSTOENAD, VedtakType.INNVILGELSE, VedtakStatus.OPPRETTET)

            val brev = opprettBrev(Status.OPPRETTET, BrevProsessType.MANUELL)
            every { db.hentBrevForBehandling(any()) } returns brev
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsenderOgAttestant(any()) } returns Pair(mockk(), mockk())
            coEvery { brevbaker.genererPdf(any()) } returns opprettBrevbakerResponse()

            runBlocking {
                vedtaksbrevService.genererPdf(SAK_ID, BEHANDLING_ID, bruker = SAKSBEHANDLER)
            }

            verify {
                db.hentBrevForBehandling(BEHANDLING_ID)
                db.hentBrevInnhold(brev.id) // Henter Slate payload fra db
            }

            coVerify {
                sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, SAKSBEHANDLER)
                adresseService.hentAvsenderOgAttestant(any())
                brevbaker.genererPdf(any())
            }
        }

        @Test
        fun `MANUELL - PDF genereres, lagres, og ferdigstilles hvis vedtak er fattet`() {
            val behandling =
                opprettBehandling(SakType.OMSTILLINGSSTOENAD, VedtakType.INNVILGELSE, VedtakStatus.FATTET_VEDTAK)

            val brev = opprettBrev(Status.OPPRETTET, BrevProsessType.MANUELL)
            every { db.hentBrevForBehandling(any()) } returns brev
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsenderOgAttestant(any()) } returns Pair(mockk(), mockk())
            coEvery { brevbaker.genererPdf(any()) } returns opprettBrevbakerResponse()

            runBlocking {
                vedtaksbrevService.genererPdf(SAK_ID, BEHANDLING_ID, bruker = ATTESTANT)
            }

            verify {
                db.hentBrevForBehandling(BEHANDLING_ID)
                db.hentBrevInnhold(brev.id)
                db.ferdigstillManueltBrevInnhold(brev.id, any())
            }

            coVerify {
                sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, ATTESTANT)
                adresseService.hentAvsenderOgAttestant(any())
                brevbaker.genererPdf(any())
            }
        }

        @Test
        fun `MANUELL - PDF genereres, men lagres ikke hvis saksbehandler sjekker sin egen sak med FATTET_VEDTAK`() {
            val behandling =
                opprettBehandling(SakType.OMSTILLINGSSTOENAD, VedtakType.INNVILGELSE, VedtakStatus.FATTET_VEDTAK)

            val brev = opprettBrev(Status.OPPRETTET, BrevProsessType.MANUELL)
            every { db.hentBrevForBehandling(any()) } returns brev
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsenderOgAttestant(any()) } returns Pair(mockk(), mockk())
            coEvery { brevbaker.genererPdf(any()) } returns opprettBrevbakerResponse()

            runBlocking {
                vedtaksbrevService.genererPdf(SAK_ID, BEHANDLING_ID, bruker = SAKSBEHANDLER)
            }

            verify {
                db.hentBrevInnhold(brev.id)
                db.hentBrevForBehandling(BEHANDLING_ID)
            }

            coVerify {
                sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, SAKSBEHANDLER)
                adresseService.hentAvsenderOgAttestant(any())
                brevbaker.genererPdf(any())
            }
        }

        @ParameterizedTest
        @EnumSource(
            Status::class,
            mode = EnumSource.Mode.EXCLUDE,
            names = ["OPPRETTET", "OPPDATERT"]
        )
        fun `Brev innhold kan ikke endres`(status: Status) {
            val brev = opprettBrev(status, mockk())

            every { db.hentBrevForBehandling(any()) } returns brev
            every { db.hentBrevInnhold(any()) } returns BrevInnhold(Spraak.NB, data = PDF_BYTES)

            runBlocking {
                vedtaksbrevService.genererPdf(SAK_ID, BEHANDLING_ID, bruker = SAKSBEHANDLER)
            }

            verify {
                db.hentBrevForBehandling(BEHANDLING_ID)
                db.hentBrevInnhold(brev.id)
            }

            coVerify {
                sakOgBehandlingService wasNot Called
                adresseService wasNot Called
                brevbaker wasNot Called
            }
        }
    }

    @Nested
    inner class PayloadManuelleBrev {
        @Test
        fun `Hent payload`() {
            val brev = opprettBrev(Status.OPPRETTET, mockk())
            every { db.hentBrevForBehandling(any()) } returns brev
            every { db.hentBrevPayload(any()) } returns mockk()

            runBlocking {
                vedtaksbrevService.hentManueltBrevPayload(BEHANDLING_ID, SAK_ID, SAKSBEHANDLER)
            }

            verify {
                db.hentBrevForBehandling(BEHANDLING_ID)
                db.hentBrevPayload(brev.id)
            }
        }

        @Test
        fun `Hent payload - hvis ikke finnes, opprett ny`() {
            val brev = opprettBrev(Status.OPPRETTET, mockk())
            every { db.hentBrevForBehandling(any()) } returns brev
            every { db.hentBrevPayload(any()) } returns null

            val behandling = opprettBehandling(SakType.OMSTILLINGSSTOENAD, VedtakType.INNVILGELSE)
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling

            runBlocking {
                vedtaksbrevService.hentManueltBrevPayload(BEHANDLING_ID, SAK_ID, SAKSBEHANDLER)
            }

            verify {
                db.hentBrevForBehandling(BEHANDLING_ID)
                db.hentBrevPayload(brev.id)
                db.opprettEllerOppdaterPayload(brev.id, any())
            }

            coVerify {
                sakOgBehandlingService.hentBehandling(SAK_ID, BEHANDLING_ID, SAKSBEHANDLER)
            }
        }
    }

    @Nested
    inner class JournalfoerVedtaksbrev {
        @Test
        fun `Vedtaksbrev journalfoeres som forventet`() {
            val forventetBrev = opprettBrev(Status.FERDIGSTILT, mockk())

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
                listOf(sakOgBehandlingService, adresseService) wasNot Called
            }
        }

        @ParameterizedTest
        @EnumSource(
            Status::class,
            mode = EnumSource.Mode.EXCLUDE,
            names = ["FERDIGSTILT"]
        )
        fun `Journalfoering av brev med ugyldig status`(status: Status) {
            val brev = Brev(
                Random.nextLong(),
                BEHANDLING_ID,
                BrevProsessType.AUTOMATISK,
                "fnr",
                "tittel",
                status,
                opprettMottaker()
            )

            runBlocking {
                assertThrows<IllegalArgumentException> {
                    vedtaksbrevService.journalfoerVedtaksbrev(brev, opprettVedtak())
                }
            }

            verify(exactly = 0) { db.settBrevFerdigstilt(any()) }

            verify {
                listOf(sakOgBehandlingService, adresseService, dokarkivService)
                    .wasNot(Called)
            }
        }
    }

    private fun opprettBrev(
        status: Status,
        prosessType: BrevProsessType
    ) = Brev(
        id = Random.nextLong(10000),
        behandlingId = BEHANDLING_ID,
        prosessType = prosessType,
        soekerFnr = "fnr",
        tittel = "tittel",
        status = status,
        mottaker = opprettMottaker()
    )

    private fun opprettVedtak() = VedtakTilJournalfoering(
        vedtakId = 1234,
        sak = VedtakSak("ident", SakType.BARNEPENSJON, 4),
        behandlingId = BEHANDLING_ID,
        ansvarligEnhet = "ansvarlig enhet"
    )

    private fun opprettBehandling(
        sakType: SakType,
        vedtakType: VedtakType,
        vedtakStatus: VedtakStatus = VedtakStatus.OPPRETTET
    ) = Behandling(
        SAK_ID,
        sakType,
        BEHANDLING_ID,
        Spraak.NB,
        Persongalleri(
            Innsender("STOR SNERK", Folkeregisteridentifikator.of("11057523044")),
            Soeker("GRØNN", "MELLOMNAVN", "KOPP", Folkeregisteridentifikator.of("12345612345")),
            Avdoed("DØD TESTPERSON", LocalDate.now().minusMonths(1))
        ),
        ForenkletVedtak(
            1,
            vedtakStatus,
            vedtakType,
            Saksbehandler(SAKSBEHANDLER.ident(), PORSGRUNN),
            attestant = null
        ),
        Utbetalingsinfo(
            1,
            Kroner(3436),
            LocalDate.now(),
            false,
            listOf(
                Beregningsperiode(
                    LocalDate.now(),
                    LocalDate.now().plusYears(4),
                    Kroner(120000),
                    1,
                    Kroner(5000),
                    40
                )
            )
        )
    )

    private fun opprettMottaker() = Mottaker(
        "Stor Snerk",
        foedselsnummer = Foedselsnummer(STOR_SNERK),
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

    private fun opprettBrevbakerResponse() = BrevbakerPdfResponse(
        "",
        LetterMetadata(
            "Testtitle",
            isSensitiv = false,
            LetterMetadata.Distribusjonstype.VEDTAK,
            LetterMetadata.Brevtype.VEDTAKSBREV
        )
    )

    private fun opprettAvsenderOgAttestant() = Pair(
        Avsender(kontor = "Nav Porsgrunn", "Etterstad 1", "0556", Telefonnummer("55553333"), "Sak Saksbehandler"),
        Attestant("Per Attestant", PORSGRUNN)
    )

    private companion object {
        private const val SAK_ID = 123L
        private val BEHANDLING_ID = UUID.randomUUID()
        private val STOR_SNERK = Folkeregisteridentifikator.of("11057523044").value
        private const val PORSGRUNN = "0805"
        private val PDF_BYTES = "Hello world!".toByteArray()
        private val SAKSBEHANDLER = Bruker.of("token", "saksbehandler", null, null, null)
        private val ATTESTANT = Bruker.of("token", "attestant", null, null, null)
    }
}