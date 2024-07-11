package no.nav.etterlatte.brev.hentinformasjon

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlientException
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningService
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagService
import no.nav.etterlatte.brev.hentinformasjon.trygdetid.TrygdetidService
import no.nav.etterlatte.brev.hentinformasjon.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.brev.hentinformasjon.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.tilbakekreving.tilbakekreving
import no.nav.etterlatte.ktor.simpleSaksbehandlerMedIdent
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.BeregningsGrunnlagFellesDto
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

internal class BrevdataFacadeImplTest {
    private val vedtaksvurderingService = mockk<VedtaksvurderingService>()
    private val grunnlagService = mockk<GrunnlagService>()
    private val beregningService = mockk<BeregningService>()
    private val behandlingService = mockk<BehandlingService>()
    private val trygdetidService = mockk<TrygdetidService>()
    private val vilkaarsvurderingService = mockk<VilkaarsvurderingService>()

    private val service =
        BrevdataFacade(
            vedtaksvurderingService,
            grunnlagService,
            behandlingService,
        )

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(vedtaksvurderingService, grunnlagService, beregningService)
    }

    @Test
    fun `hentGenerellBrevData fungerer som forventet for behandling`() {
        coEvery {
            behandlingService.hentSak(any(), any())
        } returns Sak("ident", SakType.BARNEPENSJON, SAK_ID, ENHET)
        coEvery {
            behandlingService.hentSisteIverksatteBehandling(any(), any())
        } throws BehandlingKlientException("har ikke tidligere behandling")
        coEvery { behandlingService.hentEtterbetaling(any(), any()) } returns null
        coEvery { behandlingService.hentBehandling(any(), any()) } returns lagBehandling()
        coEvery { behandlingService.hentBrevutfall(any(), any()) } returns hentBrevutfall()
        coEvery { vedtaksvurderingService.hentVedtak(any(), any()) } returns opprettBehandlingVedtak()
        val grunnlag = opprettGrunnlag()
        coEvery { grunnlagService.hentGrunnlag(any(), any(), BRUKERTOKEN, BEHANDLING_ID) } returns grunnlag
        coEvery { grunnlagService.hentVergeForSak(any(), any(), any()) } returns null
        coEvery { beregningService.hentBeregning(any(), any()) } returns opprettBeregning()
        coEvery { beregningService.hentBeregningsGrunnlag(any(), any(), any()) } returns opprettBeregningsgrunnlag()
        coEvery { trygdetidService.hentTrygdetid(any(), any()) } returns emptyList()

        val generellBrevData =
            runBlocking {
                service.hentGenerellBrevData(SAK_ID, BEHANDLING_ID, null, BRUKERTOKEN)
            }

        Assertions.assertEquals(SAK_ID, generellBrevData.sak.id)
        Assertions.assertEquals(BEHANDLING_ID, generellBrevData.behandlingId)
        Assertions.assertEquals(grunnlag.mapSpraak(), generellBrevData.spraak)
        with(generellBrevData.personerISak.soeker) {
            Assertions.assertEquals("Søker", fornavn)
            Assertions.assertEquals("Mellom", mellomnavn)
            Assertions.assertEquals("Barn", etternavn)
        }
        Assertions.assertEquals(
            "Død Mellom Far",
            generellBrevData.personerISak.avdoede
                .first()
                .navn,
        )
        Assertions.assertEquals(VedtakType.INNVILGELSE, generellBrevData.forenkletVedtak?.type)
        Assertions.assertEquals(123L, generellBrevData.forenkletVedtak?.id)
        Assertions.assertEquals(ENHET, generellBrevData.forenkletVedtak?.sakenhet)
        Assertions.assertEquals(SAKSBEHANDLER_IDENT, generellBrevData.forenkletVedtak?.saksbehandlerIdent)
        Assertions.assertEquals(ATTESTANT_IDENT, generellBrevData.forenkletVedtak?.attestantIdent)

        coVerify(exactly = 1) {
            grunnlagService.hentGrunnlag(any(), any(), any(), BEHANDLING_ID)
            grunnlagService.hentVergeForSak(any(), any(), any())
            vedtaksvurderingService.hentVedtak(any(), any())
        }
    }

    @Test
    fun `hentGenerellBrevData fungerer som forventet for tilbakekreving`() {
        val tilbakekreving = tilbakekreving()
        coEvery { behandlingService.hentSak(any(), any()) } returns Sak("ident", SakType.BARNEPENSJON, SAK_ID, ENHET)
        coEvery { vedtaksvurderingService.hentVedtak(any(), any()) } returns opprettTilbakekrevingVedtak(tilbakekreving)
        coEvery { grunnlagService.hentGrunnlag(any(), SAK_ID, BRUKERTOKEN, any()) } returns opprettGrunnlag()
        coEvery { grunnlagService.hentVergeForSak(any(), any(), any()) } returns null
        coEvery { behandlingService.hentBrevutfall(BEHANDLING_ID, BRUKERTOKEN) } returns hentBrevutfall()

        val generellBrevData =
            runBlocking {
                service.hentGenerellBrevData(SAK_ID, BEHANDLING_ID, Spraak.EN, BRUKERTOKEN)
            }

        generellBrevData.sak.id shouldBe SAK_ID
        generellBrevData.behandlingId shouldBe BEHANDLING_ID
        generellBrevData.spraak shouldBe Spraak.EN
        generellBrevData.personerISak.avdoede
            .first()
            .navn shouldBe "Død Mellom Far"
        with(generellBrevData.personerISak.soeker) {
            fornavn shouldBe "Søker"
            mellomnavn shouldBe "Mellom"
            etternavn shouldBe "Barn"
        }
        with(generellBrevData.forenkletVedtak!!) {
            type shouldBe VedtakType.TILBAKEKREVING
            sakenhet shouldBe ENHET
            id shouldBe 123L
            saksbehandlerIdent shouldBe SAKSBEHANDLER_IDENT
            attestantIdent shouldBe ATTESTANT_IDENT
            this.tilbakekreving shouldBe tilbakekreving
        }

        coVerify(exactly = 1) {
            grunnlagService.hentGrunnlag(any(), SAK_ID, any(), any())
            grunnlagService.hentVergeForSak(any(), any(), any())
            vedtaksvurderingService.hentVedtak(any(), any())
        }
    }

    private fun opprettBeregning() =
        mockk<BeregningDTO> {
            every { beregningsperioder } returns
                listOf(
                    opprettBeregningsperiode(
                        YearMonth.now(),
                        beloep = 3063,
                    ),
                )
        }

    private fun opprettBeregningsgrunnlag() =
        mockk<BeregningsGrunnlagFellesDto> {
            every { beregningsMetode } returns
                mockk {
                    every { beregningsMetode } returns BeregningsMetode.BEST
                }
        }

    private fun opprettBehandlingVedtak() =
        mockk<VedtakDto> {
            every { type } returns VedtakType.INNVILGELSE
            every { sak } returns VedtakSak("ident", SakType.BARNEPENSJON, SAK_ID)
            every { id } returns 123L
            every { status } returns VedtakStatus.OPPRETTET
            every { vedtakFattet } returns VedtakFattet(SAKSBEHANDLER_IDENT, ENHET, Tidspunkt.now())
            every { attestasjon } returns Attestasjon(ATTESTANT_IDENT, ENHET, Tidspunkt.now())
            every { innhold } returns
                mockk<VedtakInnholdDto.VedtakBehandlingDto> {
                    every { behandling.id } returns BEHANDLING_ID
                    every { virkningstidspunkt } returns YearMonth.now()
                    every { behandling.revurderingsaarsak } returns null
                    every { behandling.revurderingInfo } returns null
                    every { behandling.type } returns BehandlingType.FØRSTEGANGSBEHANDLING
                }
        }

    private fun opprettTilbakekrevingVedtak(vedtakInnhold: Tilbakekreving = tilbakekreving()) =
        mockk<VedtakDto> {
            every { type } returns VedtakType.TILBAKEKREVING
            every { sak } returns VedtakSak("ident", SakType.BARNEPENSJON, SAK_ID)
            every { id } returns 123L
            every { status } returns VedtakStatus.OPPRETTET
            every { vedtakFattet } returns VedtakFattet(SAKSBEHANDLER_IDENT, ENHET, Tidspunkt.now())
            every { attestasjon } returns Attestasjon(ATTESTANT_IDENT, ENHET, Tidspunkt.now())
            every { innhold } returns
                mockk<VedtakInnholdDto.VedtakTilbakekrevingDto> {
                    every { tilbakekreving } returns vedtakInnhold.toObjectNode()
                }
        }

    private fun opprettGrunnlag() =
        GrunnlagTestData(
            opplysningsmapSakOverrides =
                mapOf(
                    Opplysningstype.SPRAAK to opprettOpplysning(Spraak.NB.toJsonNode()),
                    Opplysningstype.PERSONGALLERI_V1 to
                        opprettOpplysning(
                            Persongalleri(soeker = SOEKER_FOEDSELSNUMMER.value, innsender = "innsender").toJsonNode(),
                        ),
                ),
        ).hentOpplysningsgrunnlag()

    private fun opprettOpplysning(jsonNode: JsonNode) =
        Opplysning.Konstant(
            STATISK_UUID,
            GRUNNLAGSOPPLYSNING_PDL,
            jsonNode,
        )

    private fun opprettBeregningsperiode(
        fom: YearMonth,
        tom: YearMonth? = null,
        beloep: Int,
        soeskenFlokk: List<String>? = null,
    ) = Beregningsperiode(
        UUID.randomUUID(),
        fom,
        tom,
        beloep,
        soeskenFlokk,
        null,
        1000,
        10000,
        10,
        beregningsMetode = BeregningsMetode.NASJONAL,
        samletNorskTrygdetid = 10,
        samletTeoretiskTrygdetid = 20,
        broek = null,
    )

    private fun lagBehandling() =
        DetaljertBehandling(
            id = UUID.randomUUID(),
            sak = 1L,
            sakType = SakType.BARNEPENSJON,
            soeker = "123",
            status = BehandlingStatus.OPPRETTET,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            virkningstidspunkt = null,
            boddEllerArbeidetUtlandet = null,
            utlandstilknytning = null,
            revurderingsaarsak = null,
            prosesstype = Prosesstype.MANUELL,
            revurderingInfo = null,
            kilde = Vedtaksloesning.GJENNY,
            sendeBrev = true,
            opphoerFraOgMed = null,
        )

    private fun hentBrevutfall() = null

    private companion object {
        private val GRUNNLAGSOPPLYSNING_PDL = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null)
        private val STATISK_UUID = UUID.randomUUID()
        private val BEHANDLING_ID = UUID.randomUUID()
        private const val ENHET = "0000"
        private const val SAKSBEHANDLER_IDENT = "Z1235"
        private val BRUKERTOKEN = simpleSaksbehandlerMedIdent(SAKSBEHANDLER_IDENT)
        private const val ATTESTANT_IDENT = "Z54321"
        private const val SAK_ID = 123L
    }
}
