package no.nav.etterlatte.samordning.vedtak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equalityMatcher
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldHave
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth.now
import java.util.UUID

class SamordningVedtakServiceTest {
    private val vedtakKlient = mockk<VedtaksvurderingKlient>()
    private val samordningVedtakService = SamordningVedtakService(vedtakKlient)

    @AfterEach
    fun after() {
        clearAllMocks()
    }

    @Test
    fun `skal kaste feil hvis vedtak ikke gjelder omstillingsstoenad`() {
        coEvery { vedtakKlient.hentVedtak(123L, ORGNO) } returns
            vedtak(
                sakstype = SakType.BARNEPENSJON,
            )

        shouldThrow<VedtakFeilSakstypeException> {
            runBlocking {
                samordningVedtakService.hentVedtak(123L, ORGNO)
            }
        }
    }

    @Test
    fun `skal mappe vedtak med to perioder, hvor nr 1 er lukket og nr 2 er aapen`() {
        coEvery { vedtakKlient.hentVedtak(456L, ORGNO) } returns
            vedtak(
                vedtakId = 456L,
                beregning = beregning(trygdetid = 32),
                avkorting =
                    avkorting(
                        Periode(fom = now(), tom = now()),
                        Periode(fom = now().plusMonths(1), tom = null),
                    ),
            )

        val result =
            runBlocking {
                samordningVedtakService.hentVedtak(456L, ORGNO)
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
    }

    @Test
    fun `skal hente to vedtak`() {
        val virkFom = LocalDate.now()

        coEvery { vedtakKlient.hentVedtaksliste(virkFom, fnr = FNR, organisasjonsnummer = ORGNO) } returns
            listOf(
                vedtak(
                    vedtakId = 123L,
                    beregning = beregning(trygdetid = 32),
                    avkorting = avkorting(),
                ),
                vedtak(
                    vedtakId = 234L,
                    beregning = beregning(trygdetid = 40),
                    avkorting = avkorting(),
                ),
            )

        val vedtaksliste =
            runBlocking {
                samordningVedtakService.hentVedtaksliste(virkFom, FNR, ORGNO)
            }

        vedtaksliste shouldHaveSize 2
    }

    companion object {
        const val ORGNO = "123456789"
        const val FNR = "10518209200"
    }
}

fun vedtak(
    vedtakId: Long? = null,
    sakstype: SakType = SakType.OMSTILLINGSSTOENAD,
    beregning: BeregningDTO? = null,
    avkorting: AvkortingDto? = null,
): VedtakSamordningDto =
    VedtakSamordningDto(
        vedtakId = vedtakId ?: 5678L,
        status = VedtakStatus.ATTESTERT,
        virkningstidspunkt = now(),
        sak = VedtakSak(ident = "123", sakstype, id = 1234L),
        behandling = mockk<Behandling>(),
        type = VedtakType.INNVILGELSE,
        vedtakFattet = null,
        attestasjon = null,
        beregning = beregning?.let { objectMapper.valueToTree(it) },
        avkorting = avkorting?.let { objectMapper.valueToTree(it) },
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
    )

fun avkorting(vararg perioder: Periode) =
    AvkortingDto(
        avkortingGrunnlag = emptyList(),
        avkortetYtelse = perioder.map { avkortetYtelse(it) },
        tidligereAvkortetYtelse = emptyList(),
    )

private fun avkortetYtelse(periode: Periode) =
    AvkortetYtelseDto(
        id = null,
        fom = periode.fom,
        tom = periode.tom,
        type = "",
        ytelseFoerAvkorting = 13000,
        avkortingsbeloep = 1000,
        ytelseEtterAvkorting = 12000,
        restanse = 0,
    )
