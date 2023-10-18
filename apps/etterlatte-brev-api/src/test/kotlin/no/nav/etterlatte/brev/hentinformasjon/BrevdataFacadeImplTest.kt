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
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
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
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.BeregnetTrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.GrunnlagOpplysningerDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
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
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

internal class BrevdataFacadeImplTest {
    private val vedtaksvurderingKlient = mockk<VedtaksvurderingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val beregningKlient = mockk<BeregningKlient>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val trygdetidKlient = mockk<TrygdetidKlient>()

    private val service =
        BrevdataFacade(
            vedtaksvurderingKlient,
            grunnlagKlient,
            beregningKlient,
            behandlingKlient,
            trygdetidKlient,
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
        coEvery { grunnlagKlient.hentGrunnlag(SAK_ID, BEHANDLING_ID, BRUKERTokenInfo) } returns opprettGrunnlag()
        coEvery { beregningKlient.hentBeregning(any(), any()) } returns opprettBeregning()
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns opprettTrygdetid()

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
        Assertions.assertEquals("Død Mellom Far", generellBrevData.personerISak.avdoed.navn)
        Assertions.assertEquals(VedtakType.INNVILGELSE, generellBrevData.forenkletVedtak.type)
        Assertions.assertEquals(123L, generellBrevData.forenkletVedtak.id)
        Assertions.assertEquals(ENHET, generellBrevData.forenkletVedtak.ansvarligEnhet)
        Assertions.assertEquals(SAKSBEHANDLER_IDENT, generellBrevData.forenkletVedtak.saksbehandlerIdent)
        Assertions.assertEquals(ATTESTANT_IDENT, generellBrevData.forenkletVedtak.attestantIdent)

        coVerify(exactly = 1) {
            grunnlagKlient.hentGrunnlag(SAK_ID, BEHANDLING_ID, any())
            vedtaksvurderingKlient.hentVedtak(any(), any())
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
        coEvery { grunnlagKlient.hentGrunnlag(any(), any(), any()) } returns opprettGrunnlag()
        coEvery { beregningKlient.hentBeregning(any(), any()) } returns opprettBeregning()
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns opprettTrygdetid()

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
            behandlingKlient.hentSak(any(), any())
        } returns Sak("ident", SakType.BARNEPENSJON, SAK_ID, ENHET)
        coEvery {
            behandlingKlient.hentSisteIverksatteBehandling(any(), any())
        } returns SisteIverksatteBehandling(UUID.randomUUID())
        coEvery { behandlingKlient.hentEtterbetaling(any(), any()) } returns null
        coEvery { vedtaksvurderingKlient.hentVedtak(any(), any()) } returns opprettVedtak()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any(), any()) } returns opprettGrunnlag()
        coEvery { beregningKlient.hentBeregning(any(), any()) } returns opprettBeregningSoeskenjustering()
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns opprettTrygdetid()

        val utbetalingsinfo =
            runBlocking {
                service.finnUtbetalingsinfo(BEHANDLING_ID, YearMonth.now(), BRUKERTokenInfo)
            }

        Assertions.assertEquals(2, utbetalingsinfo.antallBarn)
        Assertions.assertTrue(utbetalingsinfo.soeskenjustering)

        coVerify(exactly = 1) { beregningKlient.hentBeregning(any(), any()) }
    }

    @Test
    fun `henter trygdetid`() {
        val behandlingId = UUID.randomUUID()
        coEvery { trygdetidKlient.hentTrygdetid(any(), any()) } returns trygdetidDto(behandlingId)
        val trygdetid = runBlocking { service.finnTrygdetid(behandlingId, mockk()) }
        Assertions.assertEquals(3, trygdetid!!.aarTrygdetid)
        Assertions.assertEquals(6, trygdetid.maanederTrygdetid)
        Assertions.assertEquals("2", trygdetid.perioder[0].opptjeningsperiode)
    }

    private fun trygdetidDto(behandlingId: UUID) =
        TrygdetidDto(
            id = UUID.randomUUID(),
            behandlingId = behandlingId,
            beregnetTrygdetid =
                DetaljertBeregnetTrygdetidDto(
                    resultat =
                        DetaljertBeregnetTrygdetidResultat(
                            faktiskTrygdetidNorge = null,
                            faktiskTrygdetidTeoretisk = null,
                            fremtidigTrygdetidNorge = null,
                            fremtidigTrygdetidTeoretisk = null,
                            samletTrygdetidNorge = 42,
                            samletTrygdetidTeoretisk = null,
                            prorataBroek = null,
                            overstyrt = false,
                        ),
                    tidspunkt = Tidspunkt.now(),
                ),
            trygdetidGrunnlag =
                listOf(
                    TrygdetidGrunnlagDto(
                        id = UUID.randomUUID(),
                        type = "",
                        bosted = "Danmark",
                        periodeFra = LocalDate.of(2020, Month.MARCH, 5),
                        periodeTil = LocalDate.of(2023, Month.JANUARY, 1),
                        kilde = null,
                        beregnet =
                            BeregnetTrygdetidGrunnlagDto(
                                dager = 0,
                                maaneder = 10,
                                aar = 2,
                            ),
                        begrunnelse = null,
                        poengInnAar = false,
                        poengUtAar = false,
                        prorata = false,
                    ),
                ),
            opplysninger =
                GrunnlagOpplysningerDto(
                    avdoedDoedsdato = null,
                    avdoedFoedselsdato = null,
                    avdoedFylteSeksten = null,
                    avdoedFyllerSeksti = null,
                ),
            overstyrtNorskPoengaar = null,
            ident = null,
        )

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
        mockk<VedtakNyDto> {
            every { sak } returns VedtakSak("ident", SakType.BARNEPENSJON, SAK_ID)
            every { id } returns 123L
            every { type } returns VedtakType.INNVILGELSE
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
                false,
            )
    }
}
