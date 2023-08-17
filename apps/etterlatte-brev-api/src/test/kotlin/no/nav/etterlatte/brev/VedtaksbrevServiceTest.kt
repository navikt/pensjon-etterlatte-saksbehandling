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
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.brevbaker.BrevbakerKlient
import no.nav.etterlatte.brev.brevbaker.BrevbakerPdfResponse
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Avsender
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevDataFeatureToggle
import no.nav.etterlatte.brev.model.BrevDataMapper
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.BrevProsessTypeFactory
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.Kroner
import no.nav.pensjon.brevbaker.api.model.LetterMetadata
import no.nav.pensjon.brevbaker.api.model.RenderedJsonLetter
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
import java.time.YearMonth
import java.util.*
import kotlin.random.Random

internal class VedtaksbrevServiceTest {

    private val db = mockk<BrevRepository>(relaxed = true)
    private val brevbaker = mockk<BrevbakerKlient>()
    private val sakOgBehandlingService = mockk<SakOgBehandlingService>()
    private val adresseService = mockk<AdresseService>()
    private val dokarkivService = mockk<DokarkivServiceImpl>()
    private val featureToggleService =
        DummyFeatureToggleService().also { it.settBryter(BrevDataFeatureToggle.NyMalInnvilgelse, false) }
    private val brevDataMapper = BrevDataMapper(featureToggleService)
    private val brevProsessTypeFactory = BrevProsessTypeFactory(featureToggleService)

    private val vedtaksbrevService =
        VedtaksbrevService(
            db,
            sakOgBehandlingService,
            adresseService,
            dokarkivService,
            BrevbakerService(brevbaker, adresseService, brevDataMapper),
            brevDataMapper,
            brevProsessTypeFactory
        )

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
                "OMSTILLINGSSTOENAD,INNVILGELSE,AUTOMATISK",
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
            val sakId = Random.nextLong()
            val behandling = opprettBehandling(sakType, vedtakType)
            val mottaker = opprettMottaker()

            every { db.hentBrevForBehandling(behandling.behandlingId) } returns null
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentMottakerAdresse(any()) } returns mottaker

            runBlocking {
                vedtaksbrevService.opprettVedtaksbrev(
                    sakId,
                    BEHANDLING_ID,
                    ATTESTANT
                )
            }

            val brevSlot = slot<OpprettNyttBrev>()

            coVerify {
                db.hentBrevForBehandling(BEHANDLING_ID)
                sakOgBehandlingService.hentBehandling(sakId, BEHANDLING_ID, any())
                adresseService.hentMottakerAdresse(behandling.persongalleri.innsender.fnr.value)
            }

            verify {
                db.opprettBrev(capture(brevSlot))
                brevbaker wasNot Called
                dokarkivService wasNot Called
            }

            val brev = brevSlot.captured
            brev.sakId shouldBe sakId
            brev.behandlingId shouldBe behandling.behandlingId
            brev.soekerFnr shouldBe behandling.persongalleri.soeker.fnr.value
            brev.mottaker shouldBe mottaker
            brev.prosessType shouldBe forventetProsessType
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "BARNEPENSJON,YRKESSKADE,REDIGERBAR"
            ]
        )
        fun `Vedtaksbrev finnes ikke - skal opprette nytt redigerbart brev`(
            sakType: SakType,
            revurderingsaarsak: RevurderingAarsak,
            forventetProsessType: BrevProsessType
        ) {
            val sakId = Random.nextLong()
            val behandling = opprettBehandling(sakType, VedtakType.ENDRING, revurderingsaarsak = revurderingsaarsak)
            val mottaker = opprettMottaker()

            coEvery { brevbaker.genererJSON(any()) } returns opprettRenderedJsonLetter()
            coEvery { adresseService.hentAvsender(any()) } returns opprettAvsender()
            every { db.hentBrevForBehandling(behandling.behandlingId) } returns null
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentMottakerAdresse(any()) } returns mottaker

            runBlocking {
                vedtaksbrevService.opprettVedtaksbrev(
                    sakId,
                    BEHANDLING_ID,
                    ATTESTANT
                )
            }

            val brevSlot = slot<OpprettNyttBrev>()

            coVerify {
                db.hentBrevForBehandling(BEHANDLING_ID)
                sakOgBehandlingService.hentBehandling(sakId, BEHANDLING_ID, any())
                adresseService.hentMottakerAdresse(behandling.persongalleri.innsender.fnr.value)
                brevbaker.genererJSON(any())
                adresseService.hentAvsender(any())
            }

            verify {
                db.opprettBrev(capture(brevSlot))
                dokarkivService wasNot Called
            }

            val brev = brevSlot.captured
            brev.sakId shouldBe sakId
            brev.behandlingId shouldBe behandling.behandlingId
            brev.soekerFnr shouldBe behandling.persongalleri.soeker.fnr.value
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
            val brev = opprettBrev(Status.OPPRETTET, BrevProsessType.AUTOMATISK)
            val behandling = opprettBehandling(SakType.BARNEPENSJON, VedtakType.INNVILGELSE)

            every { db.hentBrev(any()) } returns brev
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsender(any()) } returns opprettAvsender()
            coEvery { brevbaker.genererPdf(any()) } returns opprettBrevbakerResponse()

            runBlocking {
                vedtaksbrevService.genererPdf(brev.id, brukerTokenInfo = SAKSBEHANDLER)
            }

            verify {
                db.hentBrev(brev.id)
            }

            coVerify {
                sakOgBehandlingService.hentBehandling(brev.sakId, brev.behandlingId!!, SAKSBEHANDLER)
                adresseService.hentAvsender(any())
                brevbaker.genererPdf(any())
            }
        }

        @Test
        fun `PDF genereres, lagres, og ferdigstilles hvis vedtak er fattet`() {
            val brev = opprettBrev(Status.OPPRETTET, BrevProsessType.AUTOMATISK)
            val behandling = opprettBehandling(SakType.BARNEPENSJON, VedtakType.INNVILGELSE, VedtakStatus.FATTET_VEDTAK)

            every { db.hentBrev(any()) } returns brev
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsender(any()) } returns opprettAvsender()
            coEvery { brevbaker.genererPdf(any()) } returns opprettBrevbakerResponse()

            runBlocking {
                vedtaksbrevService.genererPdf(brev.id, brukerTokenInfo = ATTESTANT)
            }

            verify {
                db.hentBrev(brev.id)
                db.lagrePdfOgFerdigstillBrev(brev.id, any())
            }

            coVerify {
                sakOgBehandlingService.hentBehandling(brev.sakId, brev.behandlingId!!, ATTESTANT)
                adresseService.hentAvsender(any())
                brevbaker.genererPdf(any())
            }
        }

        @Test
        fun `AUTOMATISK - PDF genereres, men lagres ikke hvis saksbehandler sjekker sin egen sak med FATTET_VEDTAK`() {
            val brev = opprettBrev(Status.OPPRETTET, BrevProsessType.AUTOMATISK)
            val behandling = opprettBehandling(SakType.BARNEPENSJON, VedtakType.INNVILGELSE, VedtakStatus.FATTET_VEDTAK)

            every { db.hentBrev(any()) } returns brev
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsender(any()) } returns opprettAvsender()
            coEvery { brevbaker.genererPdf(any()) } returns opprettBrevbakerResponse()

            runBlocking {
                vedtaksbrevService.genererPdf(brev.id, brukerTokenInfo = SAKSBEHANDLER)
            }

            verify {
                db.hentBrev(brev.id)
            }

            coVerify {
                sakOgBehandlingService.hentBehandling(brev.sakId, brev.behandlingId!!, SAKSBEHANDLER)
                adresseService.hentAvsender(any())
                brevbaker.genererPdf(any())
            }
        }

        @Test
        fun `MANUELL - PDF genereres uten lagring`() {
            val behandling =
                opprettBehandling(SakType.OMSTILLINGSSTOENAD, VedtakType.INNVILGELSE, VedtakStatus.OPPRETTET)

            val brev = opprettBrev(Status.OPPRETTET, BrevProsessType.MANUELL)
            every { db.hentBrev(any()) } returns brev
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsender(any()) } returns opprettAvsender()
            coEvery { brevbaker.genererPdf(any()) } returns opprettBrevbakerResponse()

            runBlocking {
                vedtaksbrevService.genererPdf(brev.id, brukerTokenInfo = SAKSBEHANDLER)
            }

            verify {
                db.hentBrev(brev.id)
                db.hentBrevPayload(brev.id) // Henter Slate payload fra db
            }

            coVerify {
                sakOgBehandlingService.hentBehandling(brev.sakId, brev.behandlingId!!, SAKSBEHANDLER)
                adresseService.hentAvsender(any())
                brevbaker.genererPdf(any())
            }
        }

        @Test
        fun `MANUELL - PDF genereres, lagres, og ferdigstilles hvis vedtak er fattet`() {
            val behandling =
                opprettBehandling(SakType.OMSTILLINGSSTOENAD, VedtakType.INNVILGELSE, VedtakStatus.FATTET_VEDTAK)

            val brev = opprettBrev(Status.OPPRETTET, BrevProsessType.MANUELL)
            every { db.hentBrev(any()) } returns brev
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsender(any()) } returns opprettAvsender()
            coEvery { brevbaker.genererPdf(any()) } returns opprettBrevbakerResponse()

            runBlocking {
                vedtaksbrevService.genererPdf(brev.id, brukerTokenInfo = ATTESTANT)
            }

            verify {
                db.hentBrev(brev.id)
                db.hentBrevPayload(brev.id)
                db.lagrePdfOgFerdigstillBrev(brev.id, any())
            }

            coVerify {
                sakOgBehandlingService.hentBehandling(brev.sakId, brev.behandlingId!!, ATTESTANT)
                adresseService.hentAvsender(any())
                brevbaker.genererPdf(any())
            }
        }

        @Test
        fun `MANUELL - PDF genereres, men lagres ikke hvis saksbehandler sjekker sin egen sak med FATTET_VEDTAK`() {
            val behandling =
                opprettBehandling(SakType.OMSTILLINGSSTOENAD, VedtakType.INNVILGELSE, VedtakStatus.FATTET_VEDTAK)

            val brev = opprettBrev(Status.OPPRETTET, BrevProsessType.MANUELL)
            every { db.hentBrev(any()) } returns brev
            coEvery { sakOgBehandlingService.hentBehandling(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsender(any()) } returns opprettAvsender()
            coEvery { brevbaker.genererPdf(any()) } returns opprettBrevbakerResponse()

            runBlocking {
                vedtaksbrevService.genererPdf(brev.id, brukerTokenInfo = SAKSBEHANDLER)
            }

            verify {
                db.hentBrev(brev.id)
                db.hentBrevPayload(brev.id)
            }

            coVerify {
                sakOgBehandlingService.hentBehandling(brev.sakId, brev.behandlingId!!, SAKSBEHANDLER)
                adresseService.hentAvsender(any())
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

            every { db.hentBrev(any()) } returns brev
            every { db.hentPdf(any()) } returns Pdf(PDF_BYTES)

            runBlocking {
                vedtaksbrevService.genererPdf(brev.id, brukerTokenInfo = SAKSBEHANDLER)
            }

            verify {
                db.hentBrev(brev.id)
                db.hentPdf(brev.id)
            }

            coVerify {
                sakOgBehandlingService wasNot Called
                adresseService wasNot Called
                brevbaker wasNot Called
            }
        }
    }

    @Nested
    inner class JournalfoerVedtaksbrev {
        @Test
        fun `Vedtaksbrev journalfoeres som forventet`() {
            val forventetBrev = opprettBrev(Status.FERDIGSTILT, mockk())

            val forventetResponse = JournalpostResponse("1", "OK", "melding", true)
            every { dokarkivService.journalfoer(any<BrevID>(), any()) } returns forventetResponse

            val vedtak = opprettVedtak()

            val response = runBlocking {
                vedtaksbrevService.journalfoerVedtaksbrev(forventetBrev, vedtak)
            }

            assertEquals(forventetResponse, response)

            verify(exactly = 1) {
                dokarkivService.journalfoer(forventetBrev.id, vedtak)
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
                Random.nextLong(),
                BEHANDLING_ID,
                BrevProsessType.AUTOMATISK,
                "fnr",
                status,
                opprettMottaker()
            )

            runBlocking {
                assertThrows<IllegalArgumentException> {
                    vedtaksbrevService.journalfoerVedtaksbrev(brev, opprettVedtak())
                }
            }

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
        sakId = Random.nextLong(10000),
        behandlingId = BEHANDLING_ID,
        prosessType = prosessType,
        soekerFnr = "fnr",
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
        vedtakStatus: VedtakStatus = VedtakStatus.OPPRETTET,
        revurderingsaarsak: RevurderingAarsak? = null
    ) = Behandling(
        SAK_ID,
        sakType,
        BEHANDLING_ID,
        Spraak.NB,
        Persongalleri(
            Innsender("STOR SNERK", Foedselsnummer("11057523044")),
            Soeker("GRØNN", "MELLOMNAVN", "KOPP", Foedselsnummer("12345612345")),
            Avdoed("DØD TESTPERSON", LocalDate.now().minusMonths(1)),
            verge = null
        ),
        ForenkletVedtak(
            1,
            vedtakStatus,
            vedtakType,
            PORSGRUNN,
            SAKSBEHANDLER.ident(),
            attestantIdent = null
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
        ),
        revurderingsaarsak = revurderingsaarsak,
        virkningsdato = YearMonth.of(LocalDate.now().year, LocalDate.now().month)
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

    private fun opprettAvsender() = Avsender(
        kontor = "Nav Porsgrunn",
        "Etterstad 1",
        "0556",
        Telefonnummer("55553333"),
        "Sak Saksbehandler",
        "Per Attestant"
    )

    private fun opprettRenderedJsonLetter() = RenderedJsonLetter(
        "",
        RenderedJsonLetter.Sakspart("", "", "", ""),
        emptyList(),
        RenderedJsonLetter.Signatur("", "", "", "", "")
    )

    private companion object {
        private const val SAK_ID = 123L
        private val BEHANDLING_ID = UUID.randomUUID()
        private val STOR_SNERK = Folkeregisteridentifikator.of("11057523044").value
        private const val PORSGRUNN = "0805"
        private val PDF_BYTES = "Hello world!".toByteArray()
        private val SAKSBEHANDLER = BrukerTokenInfo.of("token", "saksbehandler", null, null, null)
        private val ATTESTANT = BrukerTokenInfo.of("token", "attestant", null, null, null)
    }
}