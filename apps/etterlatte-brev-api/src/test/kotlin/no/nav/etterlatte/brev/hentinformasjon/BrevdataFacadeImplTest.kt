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
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlientException
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.tilbakekreving.tilbakekreving
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
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
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakNyDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

internal class BrevdataFacadeImplTest {
    private val vedtaksvurderingKlient = mockk<VedtaksvurderingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val beregningKlient = mockk<BeregningKlient>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val sakService = mockk<SakService>()
    private val trygdetidService = mockk<TrygdetidService>()

    private val service =
        BrevdataFacade(
            vedtaksvurderingKlient,
            grunnlagKlient,
            beregningKlient,
            behandlingKlient,
            sakService,
            trygdetidService,
        )

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(vedtaksvurderingKlient, grunnlagKlient, beregningKlient)
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
        coEvery { vedtaksvurderingKlient.hentVedtak(any(), any()) } returns opprettBehandlingVedtak()
        coEvery { grunnlagKlient.hentGrunnlag(BEHANDLING_ID, BRUKERTokenInfo) } returns opprettGrunnlag()
        coEvery { beregningKlient.hentBeregning(any(), any()) } returns opprettBeregning()
        coEvery { trygdetidService.finnTrygdetidsgrunnlag(any(), any(), any()) } returns opprettTrygdetid()

        val generellBrevData =
            runBlocking {
                service.hentGenerellBrevData(SAK_ID, BEHANDLING_ID, BRUKERTokenInfo)
            }

        Assertions.assertEquals(SAK_ID, generellBrevData.sak.id)
        Assertions.assertEquals(BEHANDLING_ID, generellBrevData.behandlingId)
        Assertions.assertEquals(Spraak.NB, generellBrevData.spraak)
        with(generellBrevData.personerISak.soeker) {
            Assertions.assertEquals("Søker", fornavn)
            Assertions.assertEquals("Mellom", mellomnavn)
            Assertions.assertEquals("Barn", etternavn)
        }
        Assertions.assertEquals("Død Mellom Far", generellBrevData.personerISak.avdoede.first().navn)
        Assertions.assertEquals(VedtakType.INNVILGELSE, generellBrevData.forenkletVedtak.type)
        Assertions.assertEquals(123L, generellBrevData.forenkletVedtak.id)
        Assertions.assertEquals(ENHET, generellBrevData.forenkletVedtak.ansvarligEnhet)
        Assertions.assertEquals(SAKSBEHANDLER_IDENT, generellBrevData.forenkletVedtak.saksbehandlerIdent)
        Assertions.assertEquals(ATTESTANT_IDENT, generellBrevData.forenkletVedtak.attestantIdent)

        coVerify(exactly = 1) {
            grunnlagKlient.hentGrunnlag(BEHANDLING_ID, any())
            vedtaksvurderingKlient.hentVedtak(any(), any())
        }
    }

    @Test
    fun `hentGenerellBrevData fungerer som forventet for tilbakekreving`() {
        val tilbakekreving = tilbakekreving()
        coEvery {
            sakService.hentSak(any(), any())
        } returns Sak("ident", SakType.BARNEPENSJON, SAK_ID, ENHET)
        coEvery {
            vedtaksvurderingKlient.hentVedtak(any(), any())
        } returns opprettTilbakekrevingVedtak(vedtakInnhold = tilbakekreving)
        coEvery { grunnlagKlient.hentGrunnlagForSak(SAK_ID, BRUKERTokenInfo) } returns opprettGrunnlag()

        val generellBrevData =
            runBlocking {
                service.hentGenerellBrevData(SAK_ID, BEHANDLING_ID, BRUKERTokenInfo)
            }

        generellBrevData.sak.id shouldBe SAK_ID
        generellBrevData.behandlingId shouldBe BEHANDLING_ID
        generellBrevData.spraak shouldBe Spraak.NB
        generellBrevData.personerISak.avdoede.first().navn shouldBe "Død Mellom Far"
        with(generellBrevData.personerISak.soeker) {
            fornavn shouldBe "Søker"
            mellomnavn shouldBe "Mellom"
            etternavn shouldBe "Barn"
        }
        with(generellBrevData.forenkletVedtak) {
            type shouldBe VedtakType.TILBAKEKREVING
            id shouldBe 123L
            ansvarligEnhet shouldBe ENHET
            saksbehandlerIdent shouldBe SAKSBEHANDLER_IDENT
            attestantIdent shouldBe ATTESTANT_IDENT
            this.tilbakekreving shouldBe tilbakekreving
        }

        coVerify(exactly = 1) {
            grunnlagKlient.hentGrunnlagForSak(SAK_ID, any())
            vedtaksvurderingKlient.hentVedtak(any(), any())
        }
    }

    @Test
    fun `FinnUtbetalingsinfo returnerer korrekt informasjon`() {
        coEvery {
            sakService.hentSak(any(), any())
        } returns Sak("ident", SakType.BARNEPENSJON, SAK_ID, ENHET)
        coEvery {
            behandlingKlient.hentSisteIverksatteBehandling(any(), any())
        } returns SisteIverksatteBehandling(UUID.randomUUID())
        coEvery { behandlingKlient.hentEtterbetaling(any(), any()) } returns null
        coEvery { vedtaksvurderingKlient.hentVedtak(any(), any()) } returns opprettBehandlingVedtak()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns opprettGrunnlag()
        coEvery { beregningKlient.hentBeregning(any(), any()) } returns opprettBeregning()
        coEvery { trygdetidService.finnTrygdetidsgrunnlag(any(), any(), any()) } returns opprettTrygdetid()

        val utbetalingsinfo =
            runBlocking {
                service.finnUtbetalingsinfo(BEHANDLING_ID, YearMonth.now(), BRUKERTokenInfo)
            }

        Assertions.assertEquals(Kroner(3063), utbetalingsinfo.beloep)
        Assertions.assertEquals(YearMonth.now().atDay(1), utbetalingsinfo.virkningsdato)
        Assertions.assertEquals(false, utbetalingsinfo.soeskenjustering)
        Assertions.assertEquals(
            listOf(BREV_BEREGNINGSPERIODE),
            utbetalingsinfo.beregningsperioder,
        )

        coVerify(exactly = 1) {
            beregningKlient.hentBeregning(BEHANDLING_ID, any())
        }
    }

    @Test
    fun `FinnUtbetalingsinfo returnerer korrekt antall barn ved soeskenjustering`() {
        coEvery {
            sakService.hentSak(any(), any())
        } returns Sak("ident", SakType.BARNEPENSJON, SAK_ID, ENHET)
        coEvery {
            behandlingKlient.hentSisteIverksatteBehandling(any(), any())
        } returns SisteIverksatteBehandling(UUID.randomUUID())
        coEvery { behandlingKlient.hentEtterbetaling(any(), any()) } returns null
        coEvery { vedtaksvurderingKlient.hentVedtak(any(), any()) } returns opprettBehandlingVedtak()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns opprettGrunnlag()
        coEvery { beregningKlient.hentBeregning(any(), any()) } returns opprettBeregningSoeskenjustering()
        coEvery { trygdetidService.finnTrygdetidsgrunnlag(any(), any(), any()) } returns opprettTrygdetid()

        val utbetalingsinfo =
            runBlocking {
                service.finnUtbetalingsinfo(BEHANDLING_ID, YearMonth.now(), BRUKERTokenInfo)
            }

        Assertions.assertEquals(2, utbetalingsinfo.antallBarn)
        Assertions.assertTrue(utbetalingsinfo.soeskenjustering)

        coVerify(exactly = 1) { beregningKlient.hentBeregning(any(), any()) }
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

    private fun opprettBeregningSoeskenjustering() =
        mockk<BeregningDTO> {
            every { beregningsperioder } returns
                listOf(
                    opprettBeregningsperiode(
                        YearMonth.now(),
                        beloep = 3063,
                        soeskenFlokk = listOf("barn2"),
                    ),
                )
        }

    private fun opprettBehandlingVedtak() =
        mockk<VedtakNyDto> {
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
        mockk<VedtakNyDto> {
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
    )

    private fun lagBehandling() =
        DetaljertBehandling(
            id = UUID.randomUUID(),
            sak = 1L,
            sakType = SakType.BARNEPENSJON,
            behandlingOpprettet = LocalDateTime.now(),
            soeknadMottattDato = LocalDateTime.now(),
            innsender = null,
            soeker = "123",
            gjenlevende = listOf(),
            avdoed = listOf(),
            soesken = listOf(),
            status = BehandlingStatus.OPPRETTET,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            virkningstidspunkt = null,
            boddEllerArbeidetUtlandet = null,
            revurderingsaarsak = null,
            prosesstype = Prosesstype.MANUELL,
            revurderingInfo = null,
            enhet = "1111",
            kilde = Vedtaksloesning.GJENNY,
        )

    private fun opprettTrygdetid() = null

    private companion object {
        private val GRUNNLAGSOPPLYSNING_PDL = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null)
        private val STATISK_UUID = UUID.randomUUID()
        private val BEHANDLING_ID = UUID.randomUUID()
        private const val ENHET = "0000"
        private const val SAKSBEHANDLER_IDENT = "Z1235"
        private val BRUKERTokenInfo = BrukerTokenInfo.of("321", SAKSBEHANDLER_IDENT, null, null, null)
        private const val ATTESTANT_IDENT = "Z54321"
        private const val SAK_ID = 123L
        private val BREV_BEREGNINGSPERIODE =
            no.nav.etterlatte.brev.behandling.Beregningsperiode(
                YearMonth.now().atDay(1),
                null,
                Kroner(10000),
                1,
                Kroner(3063),
                10,
                null,
                false,
            )
    }
}
