package no.nav.etterlatte.brev

import io.kotest.matchers.collections.shouldContain
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
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.AvkortetBeregningsperiode
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Innsender
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.brevbaker.BrevbakerKlient
import no.nav.etterlatte.brev.brevbaker.BrevbakerPdfResponse
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningService
import no.nav.etterlatte.brev.hentinformasjon.trygdetid.TrygdetidService
import no.nav.etterlatte.brev.hentinformasjon.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.brev.hentinformasjon.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevDataMapperFerdigstillingVedtak
import no.nav.etterlatte.brev.model.BrevDataMapperRedigerbartUtfallVedtak
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevKodeMapperVedtak
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.pdf.PDFGenerator
import no.nav.etterlatte.brev.vedtaksbrev.BrevManglerPDF
import no.nav.etterlatte.brev.vedtaksbrev.KanIkkeOppretteVedtaksbrev
import no.nav.etterlatte.brev.vedtaksbrev.SaksbehandlerOgAttestantSammePerson
import no.nav.etterlatte.brev.vedtaksbrev.VedtaksbrevService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.ktor.token.simpleAttestant
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.FeilutbetalingValg
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.Kroner
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
    private val beregningService = mockk<BeregningService>()
    private val trygdetidService = mockk<TrygdetidService>()
    private val vedtaksvurderingService = mockk<VedtaksvurderingService>()
    private val adresseService = mockk<AdresseService>()
    private val dokarkivService = mockk<DokarkivServiceImpl>()
    private val migreringBrevDataService = MigreringBrevDataService(beregningService)
    private val brevKodeMappingVedtak = BrevKodeMapperVedtak()
    private val brevbakerService = mockk<BrevbakerService>()
    private val behandlingService = mockk<BehandlingService>()
    private val featureToggleService =
        mockk<FeatureToggleService> {
            every { isEnabled(any(), any(), any()) } returnsArgument 1
        }
    private val vilkaarsvurderingService = mockk<VilkaarsvurderingService>()
    private val pdfGenerator = PDFGenerator(db, brevdataFacade, adresseService, brevbakerService)
    private val redigerbartVedleggHenter = RedigerbartVedleggHenter(brevbakerService, adresseService, behandlingService)
    private val innholdTilRedigerbartBrevHenter =
        InnholdTilRedigerbartBrevHenter(brevdataFacade, brevbakerService, adresseService, redigerbartVedleggHenter)
    private val brevoppretter =
        Brevoppretter(
            adresseService,
            db,
            innholdTilRedigerbartBrevHenter,
        )

    private val brevDataMapperFerdigstilling =
        spyk(
            BrevDataMapperFerdigstillingVedtak(
                beregningService,
                trygdetidService,
                behandlingService,
                vilkaarsvurderingService,
            ),
        )
    private val vedtaksbrevService =
        VedtaksbrevService(
            db,
            vedtaksvurderingService,
            brevKodeMappingVedtak,
            brevoppretter,
            pdfGenerator,
            BrevDataMapperRedigerbartUtfallVedtak(behandlingService, beregningService, migreringBrevDataService, trygdetidService),
            brevDataMapperFerdigstilling,
            behandlingService,
            featureToggleService,
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
        val SAK_ID = randomSakId()
        val BEHANDLING_ID = UUID.randomUUID()
        val PDF_BYTES = "Hello world!".toByteArray()
        val SAKSBEHANDLER = simpleSaksbehandler()
        val ATTESTANT = simpleAttestant()
        val utbetalingsinfo =
            Utbetalingsinfo(
                false,
                1,
                Kroner(3436),
                LocalDate.now(),
                false,
                listOf(
                    beregningsperiode(
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
        fun `sletting av vedtaksbrev`() {
            val brev = opprettBrev(Status.OPPRETTET, mockk())
            every { db.hentBrevForBehandling(any(), any()) } returns listOf(brev)
            coEvery {
                behandlingService.hentVedtaksbehandlingKanRedigeres(BEHANDLING_ID, SAKSBEHANDLER)
            } returns true

            runBlocking { vedtaksbrevService.settVedtaksbrevTilSlettet(BEHANDLING_ID, SAKSBEHANDLER) }

            verify { db.hentBrevForBehandling(BEHANDLING_ID, Brevtype.VEDTAK) }
            verify { db.settBrevSlettet(brev.id, SAKSBEHANDLER) }
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
            val sakId = randomSakId()
            val behandling = opprettGenerellBrevdata(sakType, vedtakType)
            val mottaker = opprettMottaker()

            every { db.hentBrevForBehandling(BEHANDLING_ID, Brevtype.VEDTAK) } returns emptyList()
            coEvery { brevdataFacade.hentGenerellBrevData(any(), any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsender(any(), any()) } returns opprettAvsender()
            coEvery { adresseService.hentMottakere(any(), any(), any()) } returns listOf(mottaker)
            coEvery { brevbakerService.hentRedigerbarTekstFraBrevbakeren(any()) } returns Slate(emptyList())
            coEvery { behandlingService.hentVedtaksbehandlingKanRedigeres(any(), any()) } returns true
            coEvery { behandlingService.hentEtterbetaling(any(), any()) } returns null
            coEvery { behandlingService.hentBrevutfall(any(), any()) } returns
                mockk<BrevutfallDto> {
                    every { feilutbetaling?.valg } returns FeilutbetalingValg.JA_VARSEL
                    every { aldersgruppe } returns Aldersgruppe.UNDER_18
                }

            if (sakType == SakType.OMSTILLINGSSTOENAD) {
                coEvery { behandlingService.hentBehandling(any(), any()) } returns
                    mockk {
                        every { opphoerFraOgMed } returns null
                        every { prosesstype } returns Prosesstype.MANUELL
                    }

                coEvery { beregningService.finnAvkortingsinfo(any(), any(), any(), any(), any()) } returns
                    Avkortingsinfo(
                        LocalDate.now(),
                        listOf(
                            AvkortetBeregningsperiode(
                                LocalDate.now(),
                                LocalDate.now().plusYears(4),
                                Kroner(120000),
                                Kroner(100000),
                                Kroner(20000),
                                Kroner(10000),
                                40,
                                Kroner(5000),
                                Kroner(0),
                                40,
                                Kroner(5000),
                                BeregningsMetode.NASJONAL,
                                BeregningsMetode.BEST,
                                null,
                                null,
                                false,
                            ),
                        ),
                        false,
                        true,
                    )
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
                brevdataFacade.hentGenerellBrevData(sakId, BEHANDLING_ID, null, any())
                adresseService.hentMottakere(sakType, behandling.personerISak, any())
                adresseService.hentAvsender(any(), any())
            }

            verify {
                db.hentBrevForBehandling(BEHANDLING_ID, Brevtype.VEDTAK)
                db.opprettBrev(capture(brevSlot), ATTESTANT)
                brevbaker wasNot Called
                dokarkivService wasNot Called
            }

            val brev = brevSlot.captured
            brev.sakId shouldBe sakId
            brev.behandlingId shouldBe behandling.behandlingId
            brev.soekerFnr shouldBe behandling.personerISak.soeker.fnr.value
            brev.mottakere shouldContain mottaker
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
            val sakId = randomSakId()
            val behandling = opprettGenerellBrevdata(sakType, vedtakType, revurderingsaarsak = revurderingsaarsak)
            val mottaker = opprettMottaker()

            coEvery { brevbakerService.hentRedigerbarTekstFraBrevbakeren(any()) } returns opprettSlate()
            every { db.hentBrevForBehandling(behandling.behandlingId!!, Brevtype.VEDTAK) } returns emptyList()
            coEvery { brevdataFacade.hentGenerellBrevData(any(), any(), any(), any()) } returns behandling
            coEvery { adresseService.hentMottakere(sakType, any(), any()) } returns listOf(mottaker)
            coEvery { adresseService.hentAvsender(any(), any()) } returns opprettAvsender()
            coEvery { beregningService.finnUtbetalingsinfo(any(), any(), any()) } returns utbetalingsinfo
            coEvery { behandlingService.hentEtterbetaling(any(), any()) } returns null
            coEvery { behandlingService.hentVedtaksbehandlingKanRedigeres(any(), any()) } returns true
            coEvery { behandlingService.hentBehandling(any(), any()) } returns mockk { every { erSluttbehandling } returns false }
            coEvery { behandlingService.hentBrevutfall(any(), any()) } returns
                mockk<BrevutfallDto> {
                    every { feilutbetaling?.valg } returns FeilutbetalingValg.JA_VARSEL
                    every { aldersgruppe } returns Aldersgruppe.UNDER_18
                    every { frivilligSkattetrekk } returns true
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
                brevdataFacade.hentGenerellBrevData(sakId, BEHANDLING_ID, null, any())
                adresseService.hentMottakere(sakType, behandling.personerISak, any())
                adresseService.hentAvsender(any(), any())
                brevbakerService.hentRedigerbarTekstFraBrevbakeren(any())
            }

            verify {
                db.opprettBrev(capture(brevSlot), ATTESTANT)
                dokarkivService wasNot Called
            }

            val brev = brevSlot.captured
            brev.sakId shouldBe sakId
            brev.behandlingId shouldBe behandling.behandlingId
            brev.soekerFnr shouldBe behandling.personerISak.soeker.fnr.value
            brev.mottakere shouldContain mottaker
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

            assertThrows<InternfeilException> {
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
            val sakId = randomSakId()
            val behandling = opprettGenerellBrevdata(SakType.BARNEPENSJON, VedtakType.INNVILGELSE)
            val mottaker = opprettMottaker()

            every { db.hentBrevForBehandling(behandling.behandlingId!!, Brevtype.VEDTAK) } returns emptyList()
            coEvery { brevdataFacade.hentGenerellBrevData(any(), any(), any(), any()) } returns behandling
            coEvery { adresseService.hentMottakere(any(), any(), any()) } returns listOf(mottaker)

            coEvery { behandlingService.hentVedtaksbehandlingKanRedigeres(any(), any()) } returns false

            assertThrows<KanIkkeOppretteVedtaksbrev> {
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
                behandlingService.hentVedtaksbehandlingKanRedigeres(BEHANDLING_ID, SAKSBEHANDLER)
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
                db.settBrevFerdigstilt(brev.id, ATTESTANT)
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
                assertThrows<BrevManglerPDF> {
                    vedtaksbrevService.ferdigstillVedtaksbrev(brev.behandlingId!!, brukerTokenInfo = ATTESTANT)
                }
            }

            verify {
                db.hentBrevForBehandling(brev.behandlingId!!, Brevtype.VEDTAK)
                db.hentPdf(brev.id)
            }
            verify(exactly = 0) { db.settBrevFerdigstilt(any(), ATTESTANT) }

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
                assertThrows<SaksbehandlerOgAttestantSammePerson> {
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
            coEvery { brevdataFacade.hentGenerellBrevData(any(), any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsender(any(), any()) } returns opprettAvsender()
            coEvery { brevbakerService.genererPdf(any(), any()) } returns opprettBrevbakerResponse()
            coEvery { brevDataMapperFerdigstilling.brevDataFerdigstilling(any()) } returns ManueltBrevDataTest()
            coEvery { vedtaksvurderingService.hentVedtak(any(), any()) } returns
                mockk<VedtakDto> {
                    every { status } returns VedtakStatus.FATTET_VEDTAK
                    every { vedtakFattet } returns VedtakFattet("ident", Enheter.PORSGRUNN.enhetNr, Tidspunkt.now())
                }

            runBlocking {
                vedtaksbrevService.genererPdf(brev.id, bruker = SAKSBEHANDLER)
            }

            verify {
                db.oppdaterBrevkoder(any(), any())
                db.lagrePdf(any(), any())
                db.hentBrev(brev.id)
                db.oppdaterBrevkoder(brev.id, Brevkoder.TOMT_INFORMASJONSBREV)
            }

            coVerify {
                brevdataFacade.hentGenerellBrevData(brev.sakId, brev.behandlingId!!, brev.spraak, SAKSBEHANDLER)
                adresseService.hentAvsender(any(), any())
                brevbakerService.genererPdf(any(), any())
            }
        }

        @Test
        fun `PDF genereres og lagres hvis vedtak er fattet`() {
            val behandling =
                opprettGenerellBrevdata(SakType.OMSTILLINGSSTOENAD, VedtakType.INNVILGELSE, VedtakStatus.FATTET_VEDTAK)

            val brev = opprettBrev(Status.OPPRETTET, BrevProsessType.REDIGERBAR)
            every { db.hentBrev(any()) } returns brev
            coEvery { brevdataFacade.hentGenerellBrevData(any(), any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsender(any(), any()) } returns opprettAvsender()
            coEvery { brevbakerService.genererPdf(any(), any()) } returns opprettBrevbakerResponse()
            coEvery { brevDataMapperFerdigstilling.brevDataFerdigstilling(any()) } returns ManueltBrevDataTest()
            coEvery { vedtaksvurderingService.hentVedtak(any(), any()) } returns
                mockk<VedtakDto> {
                    every { status } returns VedtakStatus.FATTET_VEDTAK
                    every { vedtakFattet } returns VedtakFattet("ident", Enheter.PORSGRUNN.enhetNr, Tidspunkt.now())
                }

            runBlocking {
                vedtaksbrevService.genererPdf(brev.id, bruker = ATTESTANT)
            }

            verify {
                db.oppdaterBrevkoder(any(), any())
                db.lagrePdf(any(), any())
                db.hentBrev(brev.id)
                db.lagrePdf(brev.id, any())
                db.oppdaterBrevkoder(brev.id, Brevkoder.TOMT_INFORMASJONSBREV)
            }

            coVerify {
                brevdataFacade.hentGenerellBrevData(brev.sakId, brev.behandlingId!!, brev.spraak, ATTESTANT)
                adresseService.hentAvsender(any(), any())
                brevbakerService.genererPdf(any(), any())
            }
        }

        @Test
        fun `PDF genereres, men lagres ikke hvis saksbehandler sjekker sin egen sak med FATTET_VEDTAK`() {
            val behandling =
                opprettGenerellBrevdata(SakType.OMSTILLINGSSTOENAD, VedtakType.INNVILGELSE, VedtakStatus.FATTET_VEDTAK)

            val brev = opprettBrev(Status.OPPRETTET, BrevProsessType.REDIGERBAR)
            every { db.hentBrev(any()) } returns brev
            coEvery { brevdataFacade.hentGenerellBrevData(any(), any(), any(), any()) } returns behandling
            coEvery { adresseService.hentAvsender(any(), any()) } returns opprettAvsender()
            coEvery { brevbakerService.genererPdf(any(), any()) } returns opprettBrevbakerResponse()
            coEvery { brevDataMapperFerdigstilling.brevDataFerdigstilling(any()) } returns ManueltBrevDataTest()
            coEvery { vedtaksvurderingService.hentVedtak(any(), any()) } returns
                mockk<VedtakDto> {
                    every { status } returns VedtakStatus.FATTET_VEDTAK
                    every { vedtakFattet } returns VedtakFattet("ident", Enheter.PORSGRUNN.enhetNr, Tidspunkt.now())
                }

            runBlocking {
                vedtaksbrevService.genererPdf(brev.id, bruker = SAKSBEHANDLER)
            }

            verify {
                db.oppdaterBrevkoder(any(), any())
                db.lagrePdf(any(), any())
                db.hentBrev(brev.id)
                db.oppdaterBrevkoder(brev.id, Brevkoder.TOMT_INFORMASJONSBREV)
            }

            coVerify {
                brevdataFacade.hentGenerellBrevData(brev.sakId, brev.behandlingId!!, behandling.spraak, SAKSBEHANDLER)
                adresseService.hentAvsender(any(), any())
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
            every { db.hentBrevInnhold(any()) } returns BrevInnhold("Tittel", Spraak.NB, Slate(emptyList()))
            coEvery { brevdataFacade.hentGenerellBrevData(any(), any(), any(), any()) } returns behandling
            coEvery { behandlingService.hentBrevutfall(any(), any()) } returns
                mockk<BrevutfallDto> {
                    every { feilutbetaling?.valg } returns FeilutbetalingValg.JA_VARSEL
                }
            coEvery { adresseService.hentAvsender(any(), any()) } returns opprettAvsender()
            coEvery { brevbakerService.hentRedigerbarTekstFraBrevbakeren(any()) } returnsMany
                listOf(
                    opprettOpphoerPayload(),
                    opprettVedleggPayload(),
                )

            runBlocking {
                db.oppdaterPayload(brev.id, tomPayload, SAKSBEHANDLER)
                db.oppdaterPayloadVedlegg(brev.id, listOf(vedleggPayload(tomPayload)), SAKSBEHANDLER)
            }

            val nyttInnhold =
                runBlocking {
                    vedtaksbrevService.hentNyttInnhold(
                        brev.sakId,
                        brev.id,
                        brev.behandlingId!!,
                        SAKSBEHANDLER,
                        Brevtype.VEDTAK,
                    )
                }

            nyttInnhold shouldBe
                BrevService.BrevPayload(
                    opphoerPayload,
                    listOf(
                        vedleggPayload(opprettVedleggPayload()),
                    ),
                )

            verify {
                db.hentBrev(brev.id)
                db.hentBrevInnhold(brev.id)
                db.oppdaterPayload(brev.id, opphoerPayload, SAKSBEHANDLER)
                db.oppdaterPayload(brev.id, tomPayload, SAKSBEHANDLER)
                db.oppdaterPayloadVedlegg(brev.id, listOf(vedleggPayload(opprettVedleggPayload())), SAKSBEHANDLER)
                db.oppdaterPayloadVedlegg(brev.id, listOf(vedleggPayload(tomPayload)), SAKSBEHANDLER)
                db.hentBrevkoder(brev.id)
                db.oppdaterBrevkoder(brev.id, Brevkoder.OMS_OPPHOER)
                db.oppdaterTittel(brev.id, Brevkoder.OMS_OPPHOER.tittel, SAKSBEHANDLER)
            }

            coVerify {
                brevdataFacade.hentGenerellBrevData(brev.sakId, brev.behandlingId!!, behandling.spraak, SAKSBEHANDLER)
                brevbakerService.hentRedigerbarTekstFraBrevbakeren(any())
                adresseService.hentAvsender(any(), any())
            }
        }

        @ParameterizedTest
        @EnumSource(
            Status::class,
            mode = EnumSource.Mode.EXCLUDE,
            names = ["OPPRETTET", "OPPDATERT"],
        )
        fun `Brev innhold kan ikke endres`(brevstatus: Status) {
            val brev = opprettBrev(brevstatus, mockk())

            every { db.hentBrev(any()) } returns brev
            every { db.hentPdf(any()) } returns Pdf(PDF_BYTES)

            coEvery { vedtaksvurderingService.hentVedtak(any(), any()) } returns
                mockk<VedtakDto> {
                    every { status } returns VedtakStatus.FATTET_VEDTAK
                    every { vedtakFattet } returns VedtakFattet("ident", Enheter.PORSGRUNN.enhetNr, Tidspunkt.now())
                }
            runBlocking {
                vedtaksbrevService.genererPdf(brev.id, bruker = SAKSBEHANDLER)
            }

            verify {
                db.oppdaterBrevkoder(any(), any())
                db.lagrePdf(any(), any())
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
        sakId = randomSakId(),
        behandlingId = BEHANDLING_ID,
        tittel = "tittel",
        spraak = Spraak.NB,
        prosessType = prosessType,
        soekerFnr = "fnr",
        status = status,
        Tidspunkt.now(),
        Tidspunkt.now(),
        mottakere = listOf(opprettMottaker()),
        brevtype = Brevtype.VEDTAK,
        brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
    )

    private fun opprettGenerellBrevdata(
        sakType: SakType,
        vedtakType: VedtakType,
        vedtakStatus: VedtakStatus = VedtakStatus.OPPRETTET,
        systemkilde: Vedtaksloesning = Vedtaksloesning.GJENNY,
        revurderingsaarsak: Revurderingaarsak? = null,
    ): GenerellBrevData {
        val soeker = SOEKER_FOEDSELSNUMMER.value
        return GenerellBrevData(
            sak = Sak(soeker, sakType, SAK_ID, Enheter.PORSGRUNN.enhetNr, null, null),
            personerISak =
                PersonerISak(
                    Innsender(Foedselsnummer("11057523044")),
                    Soeker("GRØNN", "MELLOMNAVN", "KOPP", Foedselsnummer(soeker)),
                    listOf(Avdoed(Foedselsnummer(""), "DØD TESTPERSON", LocalDate.now().minusMonths(1))),
                    verge = null,
                ),
            behandlingId = BEHANDLING_ID,
            forenkletVedtak =
                ForenkletVedtak(
                    1,
                    vedtakStatus,
                    vedtakType,
                    Enheter.PORSGRUNN.enhetNr,
                    SAKSBEHANDLER.ident(),
                    attestantIdent = null,
                    vedtaksdato = null,
                    virkningstidspunkt = YearMonth.now(),
                ),
            spraak = Spraak.NB,
            systemkilde = systemkilde,
            revurderingsaarsak = revurderingsaarsak,
        )
    }

    private fun opprettMottaker() =
        Mottaker(
            id = UUID.randomUUID(),
            navn = "Rød Blanding",
            foedselsnummer = MottakerFoedselsnummer(SOEKER_FOEDSELSNUMMER.value),
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
        ).let { Base64.getDecoder().decode(it.file) }
            .let { Pdf(it) }

    private fun opprettAvsender() =
        Avsender(
            kontor = "Nav Porsgrunn",
            Telefonnummer("55553333"),
            "Sak Saksbehandler",
            "Per Attestant",
        )

    private fun opprettSlate() = Slate()

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

    private fun opprettVedleggPayload() =
        Slate(
            listOf(
                Slate.Element(
                    type = Slate.ElementType.HEADING_TWO,
                    children =
                        listOf(
                            Slate.InnerElement(
                                type = null,
                                text = "Innhold vedlegg",
                                children = null,
                                placeholder = null,
                            ),
                        ),
                ),
            ),
        )

    private fun vedleggPayload(payload: Slate): BrevInnholdVedlegg =
        BrevInnholdVedlegg(
            tittel = "Utfall ved forhåndsvarsel av feilutbetaling",
            key = BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING,
            payload = payload,
        )
}

data class ManueltBrevDataTest(
    override val innhold: List<Slate.Element> = emptyList(),
) : BrevDataFerdigstilling
