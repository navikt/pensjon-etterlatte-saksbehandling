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
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlientException
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningService
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.tilbakekreving.tilbakekreving
import no.nav.etterlatte.klienter.VilkaarsvurderingKlient
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
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

internal class BrevdataFacadeImplTest {
    private val vedtaksvurderingKlient = mockk<VedtaksvurderingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val beregningService = mockk<BeregningService>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val sakService = mockk<SakService>()
    private val trygdetidKlient = mockk<TrygdetidKlient>()
    private val adresseService = mockk<AdresseService>()
    private val vilkaarsvurderingKlient = mockk<VilkaarsvurderingKlient>()

    private val service =
        BrevdataFacade(
            vedtaksvurderingKlient,
            grunnlagKlient,
            beregningService,
            behandlingKlient,
            sakService,
            trygdetidKlient,
            adresseService,
            vilkaarsvurderingKlient,
        )

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(vedtaksvurderingKlient, grunnlagKlient, beregningService)
    }

    @Test
    fun `hentGenerellBrevData fungerer som forventet for behandling`() {
        coEvery {
            sakService.hentSak(any(), any())
        } returns Sak("ident", SakType.BARNEPENSJON, SAK_ID, ENHET)
        coEvery {
            behandlingKlient.hentSisteIverksatteBehandling(any(), any())
        } throws BehandlingKlientException("har ikke tidligere behandling")
        coEvery { behandlingKlient.hentEtterbetaling(any(), any()) } returns null
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns lagBehandling()
        coEvery { behandlingKlient.hentBrevutfall(any(), any()) } returns hentBrevutfall()
        coEvery { vedtaksvurderingKlient.hentVedtak(any(), any()) } returns opprettBehandlingVedtak()
        val grunnlag = opprettGrunnlag()
        coEvery { grunnlagKlient.hentGrunnlag(BEHANDLING_ID, BRUKERTokenInfo) } returns grunnlag
        coEvery { beregningService.hentBeregning(any(), any()) } returns opprettBeregning()
        coEvery { beregningService.hentBeregningsGrunnlag(any(), any(), any()) } returns opprettBeregningsgrunnlag()
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns emptyList()

        val generellBrevData =
            runBlocking {
                service.hentGenerellBrevData(SAK_ID, BEHANDLING_ID, null, BRUKERTokenInfo)
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
            grunnlagKlient.hentGrunnlag(BEHANDLING_ID, any())
            vedtaksvurderingKlient.hentVedtak(any(), any())
        }
    }

    @Test
    fun `hentGenerellBrevData fungerer som forventet for tilbakekreving`() {
        val tilbakekreving = tilbakekreving()
        coEvery { sakService.hentSak(any(), any()) } returns Sak("ident", SakType.BARNEPENSJON, SAK_ID, ENHET)
        coEvery { vedtaksvurderingKlient.hentVedtak(any(), any()) } returns opprettTilbakekrevingVedtak(tilbakekreving)
        coEvery { grunnlagKlient.hentGrunnlagForSak(SAK_ID, BRUKERTokenInfo) } returns opprettGrunnlag()
        coEvery { behandlingKlient.hentBrevutfall(BEHANDLING_ID, BRUKERTokenInfo) } returns hentBrevutfall()

        val generellBrevData =
            runBlocking {
                service.hentGenerellBrevData(SAK_ID, BEHANDLING_ID, Spraak.EN, BRUKERTokenInfo)
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
            grunnlagKlient.hentGrunnlagForSak(SAK_ID, any())
            vedtaksvurderingKlient.hentVedtak(any(), any())
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
        private val BRUKERTokenInfo = BrukerTokenInfo.of("321", SAKSBEHANDLER_IDENT, null, null, null)
        private const val ATTESTANT_IDENT = "Z54321"
        private const val SAK_ID = 123L
    }
}
