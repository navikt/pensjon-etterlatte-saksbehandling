package no.nav.etterlatte.brev.hentinformasjon

import com.fasterxml.jackson.databind.JsonNode
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
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.util.UUID

internal class BehandlingServiceTest {
    private val vedtaksvurderingKlient = mockk<VedtaksvurderingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val beregningKlient = mockk<BeregningKlient>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val trygdetidService = mockk<TrygdetidService>()
    private val vilkaarsvurderingKlient = mockk<VilkaarsvurderingKlient>()

    private val service =
        BehandlingService(
            vedtaksvurderingKlient,
            grunnlagKlient,
            beregningKlient,
            behandlingKlient,
            trygdetidService,
            vilkaarsvurderingKlient,
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
    fun `SakOgBehandling fungerer som forventet`() {
        coEvery {
            behandlingKlient.hentSak(any(), any())
        } returns Sak("ident", SakType.BARNEPENSJON, SAK_ID, ENHET)
        coEvery {
            behandlingKlient.hentSisteIverksatteBehandling(any(), any())
        } throws BehandlingKlientException("har ikke tidligere behandling")
        coEvery { behandlingKlient.hentEtterbetaling(any(), any()) } returns null
        coEvery { vedtaksvurderingKlient.hentVedtak(any(), any()) } returns opprettVedtak()
        coEvery { grunnlagKlient.hentGrunnlag(SAK_ID, BRUKERTokenInfo) } returns opprettGrunnlag()
        coEvery { beregningKlient.hentBeregning(any(), any()) } returns opprettBeregning()
        coEvery { trygdetidService.finnTrygdetid(any(), any()) } returns opprettTrygdetid()
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns opprettVilkaarsvurdering()
        coEvery { beregningKlient.hentGrunnbeloep(any()) } returns opprettGrunnbeloep()

        val behandling =
            runBlocking {
                service.hentBehandling(SAK_ID, BEHANDLING_ID, BRUKERTokenInfo)
            }

        Assertions.assertEquals(SAK_ID, behandling.sakId)
        Assertions.assertEquals(BEHANDLING_ID, behandling.behandlingId)
        Assertions.assertEquals(Spraak.NB, behandling.spraak)
        with(behandling.personerISak.soeker) {
            Assertions.assertEquals("Søker", fornavn)
            Assertions.assertEquals("Mellom", mellomnavn)
            Assertions.assertEquals("Barn", etternavn)
        }
        Assertions.assertEquals("Død Mellom Far", behandling.personerISak.avdoed.navn)
        Assertions.assertEquals(VedtakType.INNVILGELSE, behandling.vedtak.type)
        Assertions.assertEquals(123L, behandling.vedtak.id)
        Assertions.assertEquals(ENHET, behandling.vedtak.ansvarligEnhet)
        Assertions.assertEquals(SAKSBEHANDLER_IDENT, behandling.vedtak.saksbehandlerIdent)
        Assertions.assertEquals(ATTESTANT_IDENT, behandling.vedtak.attestantIdent)
        Assertions.assertEquals(YearMonth.now().atDay(1), behandling.utbetalingsinfo!!.virkningsdato)
        Assertions.assertEquals(120_000, behandling.grunnbeloep.grunnbeloep)

        coVerify(exactly = 1) {
            vedtaksvurderingKlient.hentVedtak(BEHANDLING_ID, any())
            grunnlagKlient.hentGrunnlag(SAK_ID, any())
            beregningKlient.hentBeregning(BEHANDLING_ID, any())
            beregningKlient.hentGrunnbeloep(any())
        }
    }

    @Test
    fun `FinnUtbetalingsinfo returnerer korrekt informasjon`() {
        coEvery {
            behandlingKlient.hentSak(any(), any())
        } returns Sak("ident", SakType.BARNEPENSJON, SAK_ID, ENHET)
        coEvery {
            behandlingKlient.hentSisteIverksatteBehandling(any(), any())
        } returns SisteIverksatteBehandling(UUID.randomUUID())
        coEvery { behandlingKlient.hentEtterbetaling(any(), any()) } returns null
        coEvery { vedtaksvurderingKlient.hentVedtak(any(), any()) } returns opprettVedtak()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns opprettGrunnlag()
        coEvery { beregningKlient.hentBeregning(any(), any()) } returns opprettBeregning()
        coEvery { trygdetidService.finnTrygdetid(any(), any()) } returns opprettTrygdetid()
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns opprettVilkaarsvurdering()
        coEvery { beregningKlient.hentGrunnbeloep(any()) } returns mockk()

        val behandling =
            runBlocking {
                service.hentBehandling(SAK_ID, BEHANDLING_ID, BRUKERTokenInfo)
            }

        Assertions.assertEquals(1, behandling.utbetalingsinfo!!.antallBarn)
        Assertions.assertEquals(Kroner(3063), behandling.utbetalingsinfo!!.beloep)
        Assertions.assertEquals(YearMonth.now().atDay(1), behandling.utbetalingsinfo!!.virkningsdato)
        Assertions.assertEquals(false, behandling.utbetalingsinfo!!.soeskenjustering)
        Assertions.assertEquals(
            listOf(BREV_BEREGNINGSPERIODE),
            behandling.utbetalingsinfo!!.beregningsperioder,
        )

        coVerify(exactly = 1) {
            vedtaksvurderingKlient.hentVedtak(BEHANDLING_ID, any())
            grunnlagKlient.hentGrunnlag(SAK_ID, any())
            beregningKlient.hentBeregning(BEHANDLING_ID, any())
            beregningKlient.hentGrunnbeloep(any())
        }
    }

    @Test
    fun `FinnUtbetalingsinfo returnerer korrekt antall barn ved soeskenjustering`() {
        coEvery {
            behandlingKlient.hentSak(any(), any())
        } returns Sak("ident", SakType.BARNEPENSJON, SAK_ID, ENHET)
        coEvery {
            behandlingKlient.hentSisteIverksatteBehandling(any(), any())
        } returns SisteIverksatteBehandling(UUID.randomUUID())
        coEvery { behandlingKlient.hentEtterbetaling(any(), any()) } returns null
        coEvery { vedtaksvurderingKlient.hentVedtak(any(), any()) } returns opprettVedtak()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns opprettGrunnlag()
        coEvery { beregningKlient.hentBeregning(any(), any()) } returns opprettBeregningSoeskenjustering()
        coEvery { trygdetidService.finnTrygdetid(any(), any()) } returns opprettTrygdetid()
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns opprettVilkaarsvurdering()
        coEvery { beregningKlient.hentGrunnbeloep(any()) } returns mockk()

        val behandling =
            runBlocking {
                service.hentBehandling(SAK_ID, BEHANDLING_ID, BRUKERTokenInfo)
            }

        Assertions.assertEquals(2, behandling.utbetalingsinfo!!.antallBarn)
        Assertions.assertTrue(behandling.utbetalingsinfo!!.soeskenjustering)

        coVerify(exactly = 1) {
            vedtaksvurderingKlient.hentVedtak(any(), any())
            grunnlagKlient.hentGrunnlag(any(), any())
            beregningKlient.hentBeregning(any(), any())
            beregningKlient.hentGrunnbeloep(any())
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

    private fun opprettVedtak() =
        mockk<VedtakDto> {
            every { sak } returns VedtakSak("ident", SakType.BARNEPENSJON, SAK_ID)
            every { behandling.id } returns BEHANDLING_ID
            every { behandling.revurderingsaarsak } returns null
            every { behandling.revurderingInfo } returns null
            every { behandling.type } returns BehandlingType.FØRSTEGANGSBEHANDLING
            every { vedtakId } returns 123L
            every { type } returns VedtakType.INNVILGELSE
            every { status } returns VedtakStatus.OPPRETTET
            every { virkningstidspunkt } returns YearMonth.now()
            every { vedtakFattet } returns VedtakFattet(SAKSBEHANDLER_IDENT, ENHET, Tidspunkt.now())
            every { attestasjon } returns Attestasjon(ATTESTANT_IDENT, ENHET, Tidspunkt.now())
        }

    private fun opprettGrunnlag() =
        GrunnlagTestData(
            opplysningsmapSakOverrides =
                mapOf(
                    Opplysningstype.SPRAAK to opprettOpplysning(Spraak.NB.toJsonNode()),
                    Opplysningstype.PERSONGALLERI_V1 to
                        opprettOpplysning(
                            Persongalleri(soeker = FNR.value, innsender = "innsender").toJsonNode(),
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

    private fun opprettTrygdetid() = null

    private fun opprettVilkaarsvurdering(): VilkaarsvurderingDto {
        val mock = mockk<VilkaarsvurderingDto>()
        every { mock.resultat } returns
            VilkaarsvurderingResultat(
                VilkaarsvurderingUtfall.OPPFYLT,
                "",
                LocalDateTime.now(),
                "saksbehandler",
            )
        return mock
    }

    private fun opprettGrunnbeloep() =
        Grunnbeloep(
            dato = YearMonth.of(2023, Month.SEPTEMBER),
            grunnbeloep = 120_000,
            grunnbeloepPerMaaned = 10_000,
            omregningsfaktor = null,
        )

    private companion object {
        private val FNR = Folkeregisteridentifikator.of("11057523044")
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
                false,
            )
    }
}
