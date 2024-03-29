package no.nav.etterlatte.samordning.vedtak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equalityMatcher
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldHave
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningDto
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningPeriode
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.YearMonth.now
import java.util.UUID

const val ORGNO = "123456789"
const val FNR = "10518209200"
const val TPNR_SPK = "3010"

class SamordningVedtakServiceTest {
    private val vedtakKlient = mockk<VedtaksvurderingKlient>()
    private val tpKlient = mockk<TjenestepensjonKlient>()
    private val samordningVedtakService = SamordningVedtakService(vedtakKlient, tpKlient)

    private val tpnrSPK = Tjenestepensjonnummer(TPNR_SPK)

    @AfterEach
    fun after() {
        clearAllMocks()
    }

    @Test
    fun `skal kaste feil hvis vedtak ikke gjelder omstillingsstoenad`() {
        val vedtak =
            vedtak(
                sakstype = SakType.BARNEPENSJON,
            )

        val callerContext = MaskinportenTpContext(tpnrSPK, ORGNO)

        coEvery { vedtakKlient.hentVedtak(123L, callerContext) } returns vedtak
        coEvery { tpKlient.harTpYtelseOnDate(FNR, tpnr = tpnrSPK, now().atStartOfMonth()) } returns true

        shouldThrow<VedtakFeilSakstypeException> {
            runBlocking {
                samordningVedtakService.hentVedtak(123L, callerContext)
            }
        }
    }

    @Test
    fun `skal mappe vedtak med to perioder, hvor nr 1 er lukket og nr 2 er aapen`() {
        val vedtak =
            vedtak(
                vedtakId = 456L,
                beregning = beregning(trygdetid = 32),
                utbetalingsperioder =
                    listOf(
                        VedtakSamordningPeriode(
                            fom = now(),
                            tom = now(),
                            ytelseFoerAvkorting = 13000,
                            ytelseEtterAvkorting = 12000,
                        ),
                        VedtakSamordningPeriode(
                            fom = now().plusMonths(1),
                            tom = null,
                            ytelseFoerAvkorting = 13000,
                            ytelseEtterAvkorting = 12000,
                        ),
                    ),
            )
        coEvery { vedtakKlient.hentVedtak(456L, MaskinportenTpContext(tpnrSPK, ORGNO)) } returns vedtak
        coEvery { tpKlient.harTpYtelseOnDate(FNR, tpnr = tpnrSPK, now().atStartOfMonth()) } returns true

        val result =
            runBlocking {
                samordningVedtakService.hentVedtak(
                    456L,
                    MaskinportenTpContext(tpnr = tpnrSPK, organisasjonsnr = ORGNO),
                )
            }

        result.vedtakId shouldBe 456L
        result.type shouldBe SamordningVedtakType.START
        result.sakstype shouldBe "OMS"
        result.anvendtTrygdetid shouldBe 32

        result.perioder shouldHave
            equalityMatcher(
                listOf(
                    SamordningVedtakPeriode(
                        fom = now().atStartOfMonth(),
                        tom = now().atEndOfMonth(),
                        omstillingsstoenadBrutto = 13000,
                        omstillingsstoenadNetto = 12000,
                    ),
                    SamordningVedtakPeriode(
                        fom = now().plusMonths(1).atStartOfMonth(),
                        tom = null,
                        omstillingsstoenadBrutto = 13000,
                        omstillingsstoenadNetto = 12000,
                    ),
                ),
            )

        coVerify { tpKlient.harTpYtelseOnDate(FNR, tpnr = tpnrSPK, now().atStartOfMonth()) }
    }

    @Test
    fun `skal hente to vedtak`() {
        val fomDato = now().atStartOfMonth()
        val vedtakliste =
            listOf(
                vedtak(
                    vedtakId = 123L,
                    beregning = beregning(trygdetid = 32),
                    utbetalingsperioder = emptyList(),
                ),
                vedtak(
                    vedtakId = 234L,
                    beregning = beregning(trygdetid = 40),
                    utbetalingsperioder = emptyList(),
                ),
            )

        coEvery { vedtakKlient.hentVedtaksliste(fomDato, fnr = FNR, MaskinportenTpContext(tpnrSPK, ORGNO)) } returns vedtakliste
        coEvery { tpKlient.harTpForholdByDate(FNR, tpnr = tpnrSPK, fomDato = fomDato) } returns true

        val vedtaksliste =
            runBlocking {
                samordningVedtakService.hentVedtaksliste(
                    fomDato,
                    Folkeregisteridentifikator.of(FNR),
                    MaskinportenTpContext(tpnrSPK, ORGNO),
                )
            }

        vedtaksliste shouldHaveSize 2

        coVerify { tpKlient.harTpForholdByDate(FNR, tpnr = tpnrSPK, fomDato = fomDato) }
    }
}

fun vedtak(
    vedtakId: Long? = null,
    sakstype: SakType = SakType.OMSTILLINGSSTOENAD,
    beregning: BeregningDTO? = null,
    utbetalingsperioder: List<VedtakSamordningPeriode> = emptyList(),
): VedtakSamordningDto =
    VedtakSamordningDto(
        vedtakId = vedtakId ?: 5678L,
        fnr = FNR,
        status = VedtakStatus.ATTESTERT,
        virkningstidspunkt = now(),
        sak = VedtakSak(ident = "123", sakstype, id = 1234L),
        behandling = Behandling(id = UUID.randomUUID(), type = BehandlingType.FØRSTEGANGSBEHANDLING),
        type = VedtakType.INNVILGELSE,
        vedtakFattet = null,
        attestasjon = null,
        perioder = utbetalingsperioder,
        beregning = beregning?.let { objectMapper.valueToTree(it) },
    )

fun beregning(trygdetid: Int = 40) =
    BeregningDTO(
        beregningId = UUID.randomUUID(),
        behandlingId = UUID.randomUUID(),
        type = Beregningstype.OMS,
        grunnlagMetadata = Metadata(1, 1),
        beregnetDato = Tidspunkt.now(),
        beregningsperioder =
            listOf(
                Beregningsperiode(
                    datoFOM = now(),
                    utbetaltBeloep = 13345,
                    grunnbelopMnd = 9885,
                    grunnbelop = 118620,
                    trygdetid = trygdetid,
                ),
            ),
        overstyrBeregning = null,
    )
