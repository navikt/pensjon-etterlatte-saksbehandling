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
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Innsender
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.brevbaker.BlockTilSlateKonverterer
import no.nav.etterlatte.brev.brevbaker.BrevbakerKlient
import no.nav.etterlatte.brev.brevbaker.BrevbakerPdfResponse
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.hentinformasjon.VedtaksvurderingService
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevDataMapperFerdigstillingVedtak
import no.nav.etterlatte.brev.model.BrevDataMapperRedigerbartUtfallVedtak
import no.nav.etterlatte.brev.model.BrevKodeMapperVedtak
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Brevtype
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.Kroner
import no.nav.pensjon.brevbaker.api.model.LetterMetadata
import no.nav.pensjon.brevbaker.api.model.RenderedJsonLetter
import no.nav.pensjon.brevbaker.api.model.Telefonnummer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.YearMonth
import java.util.Base64
import java.util.UUID
import kotlin.random.Random

internal class VedtaksbrevServiceTest {
    private val db = mockk<BrevRepository>(relaxed = true)
    private val brevbaker = mockk<BrevbakerKlient>()
    private val brevdataFacade = mockk<BrevdataFacade>()
    private val vedtaksvurderingService = mockk<VedtaksvurderingService>()
    private val adresseService = mockk<AdresseService>()
    private val dokarkivService = mockk<DokarkivServiceImpl>()
    private val migreringBrevDataService = MigreringBrevDataService(brevdataFacade)
    private val brevKodeMapperVedtak = BrevKodeMapperVedtak()
    private val brevbakerService = mockk<BrevbakerService>()
    private val pdfGenerator =
        PDFGenerator(db, brevdataFacade, adresseService, brevbakerService)
    private val redigerbartVedleggHenter = RedigerbartVedleggHenter(brevbakerService)
    private val brevoppretter =
        Brevoppretter(
            adresseService,
            db,
            brevdataFacade,
            brevbakerService,
            redigerbartVedleggHenter,
        )

    private val brevDataMapperFerdigstilling = spyk(BrevDataMapperFerdigstillingVedtak(brevdataFacade))
    private val vedtaksbrevService =
        VedtaksbrevService(
            db,
            vedtaksvurderingService,
            brevKodeMapperVedtak,
            brevoppretter,
            pdfGenerator,
            BrevDataMapperRedigerbartUtfallVedtak(brevdataFacade, migreringBrevDataService),
            brevDataMapperFerdigstilling,
        )

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(db, adresseService, dokarkivService, brevbaker)
    }

    private companion object {
        private const val SAK_ID = 123L
        private val BEHANDLING_ID = UUID.randomUUID()
        private const val PORSGRUNN = "0805"
        private val PDF_BYTES = "Hello world!".toByteArray()
        private val SAKSBEHANDLER = BrukerTokenInfo.of("token", "saksbehandler", null, null, null)
        private val ATTESTANT = BrukerTokenInfo.of("token", "attestant", null, null, null)
        private val utbetalingsinfo =
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
                        40,
                        prorataBroek = null,
                        institusjon = false,
                        beregningsMetodeAnvendt = BeregningsMetode.NASJONAL,
                        beregningsMetodeFraGrunnlag = BeregningsMetode.BEST,
                    ),
                ),
            )
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
            every { db.hentBrevForBehandling(any(), any()) } returns listOf(forventetBrev)

            val brev = vedtaksbrevService.hentVedtaksbrev(BEHANDLING_ID)

            brev shouldBe forventetBrev

            verify(exactly = 1) { db.hentBrevForBehandling(BEHANDLING_ID, Brevtype.VEDTAK) }
        }

        @Test
        fun `Sletting av brev`() {
            every { db.fjernFerdigstiltStatusUnderkjentVedtak(any(), any()) } returns true

            val vedtak = """{}""".toJsonNode()
            val gjenaapnetOK = vedtaksbrevService.fjernFerdigstiltStatusUnderkjentVedtak(1, vedtak)

            gjenaapnetOK shouldBe true

            verify { db.fjernFerdigstiltStatusUnderkjentVedtak(1, vedtak) }
        }
    }

    @Nested
    inner class OpprettVedtaksbrevTest {
        @ParameterizedTest
        @CsvSource(
            value = [
                "OMSTILLINGSSTOENAD,OPPHOER,REDIGERBAR",
                "OMSTILLINGSSTOENAD,ENDRING,REDIGERBAR",
                "BARNEPENSJON,OPPHOER,REDIGERBAR",
            ],
        )
        fun `Vedtaksbrev finnes ikke - skal opprettes nytt`(
            sakType: SakType,
            vedtakType: VedtakType,
            forventetProsessType: BrevProsessType,
        ) {
            val sakId = Random.nextLong()
            val behandling = opprettGenerellBrevdata(sakType, vedtakType)
            val mottaker = opprettMottaker()

            every { db.hentBrevForBehandling(behandling.behandlingId!!, Brevtype.VEDTAK) } returns emptyList()
            coEvery { brevdataFacade.hentGenerellBrevData(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsender(any()) } returns opprettAvsender()
            coEvery { adresseService.hentMottakerAdresse(any()) } returns mottaker
            coEvery { brevbakerService.hentRedigerbarTekstFraBrevbakeren(any()) } returns Slate(emptyList())
            coEvery { brevdataFacade.hentBehandling(any(), any()) } returns
                mockk<DetaljertBehandling>().apply {
                    every { status } returns BehandlingStatus.BEREGNET
                }
            coEvery { brevdataFacade.hentEtterbetaling(any(), any()) } returns mockk()

            runBlocking {
                vedtaksbrevService.opprettVedtaksbrev(
                    sakId,
                    BEHANDLING_ID,
                    ATTESTANT,
                )
            }

            val brevSlot = slot<OpprettNyttBrev>()

            coVerify {
                db.hentBrevForBehandling(BEHANDLING_ID, Brevtype.VEDTAK)
                brevdataFacade.hentGenerellBrevData(sakId, BEHANDLING_ID, any())
                adresseService.hentMottakerAdresse(behandling.personerISak.innsender!!.fnr.value)
            }

            verify {
                db.opprettBrev(capture(brevSlot))
                brevbaker wasNot Called
                dokarkivService wasNot Called
            }

            val brev = brevSlot.captured
            brev.sakId shouldBe sakId
            brev.behandlingId shouldBe behandling.behandlingId
            brev.soekerFnr shouldBe behandling.personerISak.soeker.fnr.value
            brev.mottaker shouldBe mottaker
            brev.prosessType shouldBe forventetProsessType
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "BARNEPENSJON,YRKESSKADE,REDIGERBAR,ENDRING",
                "BARNEPENSJON,,REDIGERBAR,ENDRING",
                "BARNEPENSJON,,REDIGERBAR,INNVILGELSE",
            ],
        )
        fun `Vedtaksbrev finnes ikke - skal opprette nytt redigerbart brev`(
            sakType: SakType,
            revurderingsaarsak: Revurderingaarsak?,
            forventetProsessType: BrevProsessType,
            vedtakType: VedtakType,
        ) {
            val sakId = Random.nextLong()
            val behandling = opprettGenerellBrevdata(sakType, vedtakType, revurderingsaarsak = revurderingsaarsak)
            val mottaker = opprettMottaker()

            coEvery { brevbakerService.hentRedigerbarTekstFraBrevbakeren(any()) } returns opprettRenderedJsonLetter()
            every { db.hentBrevForBehandling(behandling.behandlingId!!, Brevtype.VEDTAK) } returns emptyList()
            coEvery { brevdataFacade.hentGenerellBrevData(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentMottakerAdresse(any()) } returns mottaker
            coEvery { brevdataFacade.finnUtbetalingsinfo(any(), any(), any(), any()) } returns utbetalingsinfo
            coEvery { brevdataFacade.hentEtterbetaling(any(), any()) } returns null
            coEvery { brevdataFacade.hentBehandling(any(), any()) } returns
                mockk<DetaljertBehandling>().apply {
                    every { status } returns BehandlingStatus.BEREGNET
                }

            runBlocking {
                vedtaksbrevService.opprettVedtaksbrev(
                    sakId,
                    BEHANDLING_ID,
                    ATTESTANT,
                )
            }

            val brevSlot = slot<OpprettNyttBrev>()

            coVerify {
                db.hentBrevForBehandling(BEHANDLING_ID, Brevtype.VEDTAK)
                brevdataFacade.hentGenerellBrevData(sakId, BEHANDLING_ID, any())
                adresseService.hentMottakerAdresse(behandling.personerISak.innsender!!.fnr.value)
                brevbakerService.hentRedigerbarTekstFraBrevbakeren(any())
            }

            verify {
                db.opprettBrev(capture(brevSlot))
                dokarkivService wasNot Called
            }

            val brev = brevSlot.captured
            brev.sakId shouldBe sakId
            brev.behandlingId shouldBe behandling.behandlingId
            brev.soekerFnr shouldBe behandling.personerISak.soeker.fnr.value
            brev.mottaker shouldBe mottaker
            brev.prosessType shouldBe forventetProsessType
        }

        @Test
        fun `Vedtaksbrev finnes allerede - skal kaste feil`() {
            every { db.hentBrevForBehandling(any(), any()) } returns
                listOf(
                    opprettBrev(
                        Status.OPPRETTET,
                        BrevProsessType.AUTOMATISK,
                    ),
                )

            assertThrows<IllegalArgumentException> {
                runBlocking {
                    vedtaksbrevService.opprettVedtaksbrev(
                        SAK_ID,
                        BEHANDLING_ID,
                        SAKSBEHANDLER,
                    )
                }
            }

            coVerify {
                db.hentBrevForBehandling(BEHANDLING_ID, Brevtype.VEDTAK)
            }

            verify {
                db.hentBrevForBehandling(BEHANDLING_ID, Brevtype.VEDTAK)

                brevbakerService wasNot Called
                adresseService wasNot Called
                dokarkivService wasNot Called
                brevdataFacade wasNot Called
            }
        }

        @Test
        fun `Behandling er i feil status - skal kaste feil`() {
            val sakId = Random.nextLong()
            val behandling = opprettGenerellBrevdata(SakType.BARNEPENSJON, VedtakType.INNVILGELSE)
            val mottaker = opprettMottaker()

            every { db.hentBrevForBehandling(behandling.behandlingId!!, Brevtype.VEDTAK) } returns listOf()
            coEvery { brevdataFacade.hentGenerellBrevData(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentMottakerAdresse(any()) } returns mottaker

            coEvery { brevdataFacade.hentBehandling(any(), any()) } returns
                mockk<DetaljertBehandling>().apply {
                    every { status } returns BehandlingStatus.IVERKSATT
                }

            assertThrows<IllegalArgumentException> {
                runBlocking {
                    vedtaksbrevService.opprettVedtaksbrev(
                        sakId,
                        behandling.behandlingId!!,
                        SAKSBEHANDLER,
                    )
                }
            }

            coVerify {
                db.hentBrevForBehandling(BEHANDLING_ID, Brevtype.VEDTAK)
                brevdataFacade.hentBehandling(BEHANDLING_ID, SAKSBEHANDLER)
                brevbaker wasNot Called
                adresseService wasNot Called
                dokarkivService wasNot Called
            }
        }
    }

    @Nested
    inner class BrevInnholdTest {
        @Test
        fun `Ferdigstille vedtaksbrev som ATTESTANT - status vedtak fattet - ferdigstilles OK`() {
            val brev = opprettBrev(Status.OPPRETTET, mockk())

            every { db.hentBrevForBehandling(any(), any()) } returns listOf(brev)
            every { db.hentPdf(any()) } returns Pdf("".toByteArray())
            coEvery { vedtaksvurderingService.hentVedtakSaksbehandlerOgStatus(any(), any()) } returns
                Pair(
                    SAKSBEHANDLER.ident(),
                    VedtakStatus.FATTET_VEDTAK,
                )

            runBlocking {
                vedtaksbrevService.ferdigstillVedtaksbrev(brev.behandlingId!!, brukerTokenInfo = ATTESTANT)
            }

            verify {
                db.hentBrevForBehandling(brev.behandlingId!!, Brevtype.VEDTAK)
                db.settBrevFerdigstilt(brev.id)
                db.hentPdf(brev.id)
            }

            coVerify {
                vedtaksvurderingService.hentVedtakSaksbehandlerOgStatus(brev.behandlingId!!, any())
            }
        }

        @Test
        fun `Ferdigstille vedtaksbrev som ATTESTANT - status vedtak fattet, men PDF mangler`() {
            val brev = opprettBrev(Status.OPPRETTET, mockk())

            every { db.hentBrevForBehandling(any(), any()) } returns listOf(brev)
            every { db.hentPdf(any()) } returns null
            coEvery { vedtaksvurderingService.hentVedtakSaksbehandlerOgStatus(any(), any()) } returns
                Pair(
                    SAKSBEHANDLER.ident(),
                    VedtakStatus.FATTET_VEDTAK,
                )

            runBlocking {
                assertThrows<IllegalStateException> {
                    vedtaksbrevService.ferdigstillVedtaksbrev(brev.behandlingId!!, brukerTokenInfo = ATTESTANT)
                }
            }

            verify {
                db.hentBrevForBehandling(brev.behandlingId!!, Brevtype.VEDTAK)
                db.hentPdf(brev.id)
            }
            verify(exactly = 0) { db.settBrevFerdigstilt(any()) }

            coVerify {
                vedtaksvurderingService.hentVedtakSaksbehandlerOgStatus(brev.behandlingId!!, any())
            }
        }

        @Test
        fun `Ferdigstille vedtaksbrev som SAKSBEHANDLER - vedtak fattet - skal kaste feil`() {
            val brev = opprettBrev(Status.OPPRETTET, mockk())

            every { db.hentBrevForBehandling(any(), any()) } returns listOf(brev)
            coEvery { vedtaksvurderingService.hentVedtakSaksbehandlerOgStatus(any(), any()) } returns
                Pair(
                    SAKSBEHANDLER.ident(),
                    VedtakStatus.FATTET_VEDTAK,
                )

            runBlocking {
                assertThrows<IllegalStateException> {
                    vedtaksbrevService.ferdigstillVedtaksbrev(brev.behandlingId!!, brukerTokenInfo = SAKSBEHANDLER)
                }
            }

            verify {
                db.hentBrevForBehandling(brev.behandlingId!!, Brevtype.VEDTAK)
            }

            coVerify {
                vedtaksvurderingService.hentVedtakSaksbehandlerOgStatus(brev.behandlingId!!, any())
            }
        }

        @Test
        fun `Ferdigstille allerede ferdigstilt brev`() {
            val brev = opprettBrev(Status.FERDIGSTILT, mockk())

            every { db.hentBrevForBehandling(any(), any()) } returns listOf(brev)

            runBlocking {
                vedtaksbrevService.ferdigstillVedtaksbrev(brev.behandlingId!!, brukerTokenInfo = SAKSBEHANDLER)
            }

            verify { db.hentBrevForBehandling(brev.behandlingId!!, Brevtype.VEDTAK) }
        }

        @Test
        fun `PDF genereres uten lagring`() {
            val behandling =
                opprettGenerellBrevdata(SakType.OMSTILLINGSSTOENAD, VedtakType.INNVILGELSE, VedtakStatus.OPPRETTET)

            val brev = opprettBrev(Status.OPPRETTET, BrevProsessType.REDIGERBAR)
            every { db.hentBrev(any()) } returns brev
            coEvery { brevdataFacade.hentGenerellBrevData(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsender(any()) } returns opprettAvsender()
            coEvery { brevbakerService.genererPdf(any(), any()) } returns opprettBrevbakerResponse()
            coEvery { brevDataMapperFerdigstilling.brevDataFerdigstilling(any()) } returns ManueltBrevData()

            runBlocking {
                vedtaksbrevService.genererPdf(brev.id, bruker = SAKSBEHANDLER)
            }

            verify {
                db.hentBrev(brev.id)
            }

            coVerify {
                brevdataFacade.hentGenerellBrevData(brev.sakId, brev.behandlingId!!, SAKSBEHANDLER)
                adresseService.hentAvsender(any())
                brevbakerService.genererPdf(any(), any())
            }
        }

        @Test
        fun `PDF genereres og lagres hvis vedtak er fattet`() {
            val behandling =
                opprettGenerellBrevdata(SakType.OMSTILLINGSSTOENAD, VedtakType.INNVILGELSE, VedtakStatus.FATTET_VEDTAK)

            val brev = opprettBrev(Status.OPPRETTET, BrevProsessType.REDIGERBAR)
            every { db.hentBrev(any()) } returns brev
            coEvery { brevdataFacade.hentGenerellBrevData(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsender(any()) } returns opprettAvsender()
            coEvery { brevbakerService.genererPdf(any(), any()) } returns opprettBrevbakerResponse()
            coEvery { brevDataMapperFerdigstilling.brevDataFerdigstilling(any()) } returns ManueltBrevData()

            runBlocking {
                vedtaksbrevService.genererPdf(brev.id, bruker = ATTESTANT)
            }

            verify {
                db.hentBrev(brev.id)
                db.lagrePdf(brev.id, any())
            }

            coVerify {
                brevdataFacade.hentGenerellBrevData(brev.sakId, brev.behandlingId!!, ATTESTANT)
                adresseService.hentAvsender(any())
                brevbakerService.genererPdf(any(), any())
            }
        }

        @Test
        fun `PDF genereres, men lagres ikke hvis saksbehandler sjekker sin egen sak med FATTET_VEDTAK`() {
            val behandling =
                opprettGenerellBrevdata(SakType.OMSTILLINGSSTOENAD, VedtakType.INNVILGELSE, VedtakStatus.FATTET_VEDTAK)

            val brev = opprettBrev(Status.OPPRETTET, BrevProsessType.REDIGERBAR)
            every { db.hentBrev(any()) } returns brev
            coEvery { brevdataFacade.hentGenerellBrevData(any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsender(any()) } returns opprettAvsender()
            coEvery { brevbakerService.genererPdf(any(), any()) } returns opprettBrevbakerResponse()
            coEvery { brevDataMapperFerdigstilling.brevDataFerdigstilling(any()) } returns ManueltBrevData()

            runBlocking {
                vedtaksbrevService.genererPdf(brev.id, bruker = SAKSBEHANDLER)
            }

            verify {
                db.hentBrev(brev.id)
            }

            coVerify {
                brevdataFacade.hentGenerellBrevData(brev.sakId, brev.behandlingId!!, SAKSBEHANDLER)
                adresseService.hentAvsender(any())
                brevbakerService.genererPdf(any(), any())
            }
        }

        @Test
        fun `REDIGERBAR - Nytt innhold genereres og erstatter gammelt innhold`() {
            val behandling =
                opprettGenerellBrevdata(SakType.OMSTILLINGSSTOENAD, VedtakType.OPPHOER, VedtakStatus.FATTET_VEDTAK)

            val brev = opprettBrev(Status.OPPRETTET, BrevProsessType.REDIGERBAR)
            val opphoerPayload = opprettOpphoerPayload()
            val tomPayload = Slate(listOf(Slate.Element(Slate.ElementType.PARAGRAPH)))
            every { db.hentBrev(any()) } returns brev
            coEvery { brevdataFacade.hentGenerellBrevData(any(), any(), any()) } returns behandling
            coEvery { brevbakerService.hentRedigerbarTekstFraBrevbakeren(any()) } returns opprettOpphoerPayload()

            runBlocking {
                db.oppdaterPayload(brev.id, tomPayload)
            }

            val nyttInnhold =
                runBlocking {
                    vedtaksbrevService.hentNyttInnhold(brev.sakId, brev.id, brev.behandlingId!!, SAKSBEHANDLER)
                }

            nyttInnhold shouldBe BrevService.BrevPayload(opphoerPayload, emptyList())

            verify {
                db.oppdaterPayload(brev.id, opphoerPayload)
                db.oppdaterPayload(brev.id, tomPayload)
                db.hentBrevPayloadVedlegg(brev.id)
            }

            coVerify {
                brevdataFacade.hentGenerellBrevData(brev.sakId, brev.behandlingId!!, SAKSBEHANDLER)
                brevbakerService.hentRedigerbarTekstFraBrevbakeren(any())
            }
        }

        @ParameterizedTest
        @EnumSource(
            Status::class,
            mode = EnumSource.Mode.EXCLUDE,
            names = ["OPPRETTET", "OPPDATERT"],
        )
        fun `Brev innhold kan ikke endres`(status: Status) {
            val brev = opprettBrev(status, mockk())

            every { db.hentBrev(any()) } returns brev
            every { db.hentPdf(any()) } returns Pdf(PDF_BYTES)

            runBlocking {
                vedtaksbrevService.genererPdf(brev.id, bruker = SAKSBEHANDLER)
            }

            verify {
                db.hentBrev(brev.id)
                db.hentPdf(brev.id)
            }

            coVerify {
                brevdataFacade wasNot Called
                adresseService wasNot Called
                brevbaker wasNot Called
            }
        }
    }

    private fun opprettBrev(
        status: Status,
        prosessType: BrevProsessType,
    ) = Brev(
        id = Random.nextLong(10000),
        sakId = Random.nextLong(10000),
        behandlingId = BEHANDLING_ID,
        tittel = "tittel",
        prosessType = prosessType,
        soekerFnr = "fnr",
        status = status,
        Tidspunkt.now(),
        Tidspunkt.now(),
        mottaker = opprettMottaker(),
        brevtype = Brevtype.VEDTAK,
    )

    private fun opprettGenerellBrevdata(
        sakType: SakType,
        vedtakType: VedtakType,
        vedtakStatus: VedtakStatus = VedtakStatus.OPPRETTET,
        systemkilde: Vedtaksloesning = Vedtaksloesning.GJENNY,
        revurderingsaarsak: Revurderingaarsak? = null,
    ): GenerellBrevData {
        val soeker = "12345612345"
        return GenerellBrevData(
            sak = Sak(soeker, sakType, SAK_ID, "4808"),
            personerISak =
                PersonerISak(
                    Innsender(Foedselsnummer("11057523044")),
                    Soeker("GRØNN", "MELLOMNAVN", "KOPP", Foedselsnummer(soeker)),
                    listOf(Avdoed("DØD TESTPERSON", LocalDate.now().minusMonths(1))),
                    verge = null,
                ),
            behandlingId = BEHANDLING_ID,
            forenkletVedtak =
                ForenkletVedtak(
                    1,
                    vedtakStatus,
                    vedtakType,
                    PORSGRUNN,
                    SAKSBEHANDLER.ident(),
                    attestantIdent = null,
                    vedtaksdato = null,
                    virkningstidspunkt = YearMonth.now(),
                    revurderingInfo = null,
                ),
            spraak = Spraak.NB,
            systemkilde = systemkilde,
            revurderingsaarsak = revurderingsaarsak,
        )
    }

    private fun opprettMottaker() =
        Mottaker(
            "Rød Blanding",
            foedselsnummer = Foedselsnummer(SOEKER_FOEDSELSNUMMER.value),
            orgnummer = null,
            adresse =
                Adresse(
                    adresseType = "NORSKPOSTADRESSE",
                    adresselinje1 = "Testgaten 13",
                    postnummer = "1234",
                    poststed = "OSLO",
                    land = "Norge",
                    landkode = "NOR",
                ),
        )

    private fun opprettBrevbakerResponse() =
        BrevbakerPdfResponse(
            "",
            LetterMetadata(
                "Testtitle",
                isSensitiv = false,
                LetterMetadata.Distribusjonstype.VEDTAK,
                LetterMetadata.Brevtype.VEDTAKSBREV,
            ),
        ).let { Base64.getDecoder().decode(it.base64pdf) }
            .let { Pdf(it) }

    private fun opprettAvsender() =
        Avsender(
            kontor = "Nav Porsgrunn",
            Telefonnummer("55553333"),
            "Sak Saksbehandler",
            "Per Attestant",
        )

    private fun opprettRenderedJsonLetter() =
        RenderedJsonLetter(
            "",
            RenderedJsonLetter.Sakspart("", "", "", ""),
            emptyList(),
            RenderedJsonLetter.Signatur("", "", "", "", ""),
        ).let { BlockTilSlateKonverterer.konverter(it) }

    private fun opprettOpphoerPayload() =
        Slate(
            listOf(
                Slate.Element(
                    type = Slate.ElementType.HEADING_TWO,
                    children =
                        listOf(
                            Slate.InnerElement(
                                type = null,
                                text = "Dette er en tom brevmal",
                                children = null,
                                placeholder = null,
                            ),
                        ),
                ),
                Slate.Element(
                    type = Slate.ElementType.PARAGRAPH,
                    children =
                        listOf(
                            Slate.InnerElement(
                                type = null,
                                text = "Det finnes ingen brevmal for denne sak- eller vedtakstypen.",
                                children = null,
                                placeholder = null,
                            ),
                        ),
                ),
            ),
        )
}
