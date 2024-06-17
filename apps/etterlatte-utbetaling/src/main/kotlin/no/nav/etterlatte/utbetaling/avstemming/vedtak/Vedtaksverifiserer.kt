package no.nav.etterlatte.utbetaling.avstemming.vedtak

import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.utbetaling.VedtaksvurderingKlient
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinje
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.YearMonth

class Vedtaksverifiserer(
    private val repository: UtbetalingDao,
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun verifiserAlle() {
        repository.hentUtbetalinger().forEach {
            try {
                verifiser(it)
            } catch (e: Exception) {
                logger.warn("Klarte ikke å verifisere match mellom utbetaling og vedtak for vedtak $it", e)
                logger.debug("Fortsetter med å verifisere neste vedtak")
            }
        }
    }

    suspend fun verifiser(vedtakId: Long) {
        val utbetaling = repository.hentUtbetaling(vedtakId = vedtakId)
        if (utbetaling == null) {
            logger.warn("Ingen utbetaling for vedtak $vedtakId. Returnerer")
            return
        }
        val vedtak =
            retryOgPakkUt {
                vedtaksvurderingKlient.hentVedtak(
                    utbetaling.behandlingId.value,
                    Systembruker.automatiskJobb,
                )
            }
        sammenlignLinjer(utbetaling, vedtak)
    }

    private fun sammenlignLinjer(
        utbetaling: Utbetaling,
        vedtak: VedtakDto,
    ) {
        if (vedtak.innhold is VedtakInnholdDto.VedtakBehandlingDto) {
            (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto).utbetalingsperioder.forEach { fraVedtak ->
                val korresponderendeUtbetalingslinje =
                    utbetaling.utbetalingslinjer
                        .maxByOrNull { YearMonth.from(it.periode.fra) == fraVedtak.periode.fom }
                        ?: throw IllegalStateException(
                            "Mangler korresponderende utbetalingslinje for periode ${fraVedtak.periode} for vedtak ${vedtak.id}",
                        )
                verifiserAtStemmerOverens(vedtak.id, fraVedtak, korresponderendeUtbetalingslinje)
            }
        }
    }

    private fun verifiserAtStemmerOverens(
        vedtakId: Long,
        fraVedtak: Utbetalingsperiode,
        korresponderendeUtbetalingslinje: Utbetalingslinje,
    ) {
        check(sammenlignBeloep(fraVedtak, korresponderendeUtbetalingslinje)) {
            "Beløp fra vedtak $vedtakId var ${fraVedtak.beloep}, men i utbetalingslinje ${korresponderendeUtbetalingslinje.beloep}"
        }
        check(fraVedtak.periode.tom == korresponderendeUtbetalingslinje.periode.til?.let { YearMonth.from(it) }) {
            "Tom-periode fra vedtak $vedtakId var ${fraVedtak.periode.tom}, men i utbetalingslinje ${
                korresponderendeUtbetalingslinje.periode.til?.let {
                    YearMonth.from(
                        it,
                    )
                }
            }"
        }
    }

    private fun sammenlignBeloep(
        fraVedtak: Utbetalingsperiode,
        korresponderendeUtbetalingslinje: Utbetalingslinje,
    ): Boolean {
        if (fraVedtak.type == UtbetalingsperiodeType.OPPHOER) {
            return true
        }
        if (fraVedtak.beloep == null && korresponderendeUtbetalingslinje.beloep == null) {
            return true
        }
        if (fraVedtak.beloep == null || korresponderendeUtbetalingslinje.beloep == null) {
            return false
        }
        return fraVedtak.beloep!!.minus(korresponderendeUtbetalingslinje.beloep).abs() < BigDecimal.ONE
    }
}
