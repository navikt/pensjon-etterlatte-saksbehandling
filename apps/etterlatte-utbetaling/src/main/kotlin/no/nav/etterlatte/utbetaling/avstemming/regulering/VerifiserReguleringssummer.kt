package no.nav.etterlatte.utbetaling.avstemming.regulering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.utbetaling.VedtaksvurderingKlient
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinje
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingslinjeId
import org.slf4j.LoggerFactory
import java.time.YearMonth

class VerifiserReguleringssummer(
    private val repository: UtbetalingDao,
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun verifiserAlle() {
        repository.hentUtbetalinger()
    }

    suspend fun verifiser(vedtakId: Long) {
        val utbetaling = repository.hentUtbetaling(vedtakId = vedtakId)
        if (utbetaling == null) {
            logger.warn("Ingen utbetaling for vedtak $vedtakId. Returnerer")
            return
        }
        val vedtak = vedtaksvurderingKlient.hentVedtak(utbetaling.behandlingId.value, Systembruker.automatiskJobb)
        sammenlignLinjer(utbetaling, vedtak)
//        verifiserMotForrigeUtbetaling(utbetaling, vedtak)
    }

    private fun verifiserMotForrigeUtbetaling(
        utbetaling: Utbetaling,
        vedtak: VedtakDto,
    ) {
        val linjer = utbetaling.utbetalingslinjer.sortedBy { it.opprettet }

        val map = mutableMapOf<UtbetalingslinjeId, MutableList<Utbetalingslinje>>()
        val utbetalingslinjeNull = UtbetalingslinjeId(-1)
        linjer.forEach {
            val key = it.erstatterId ?: utbetalingslinjeNull
            map.putIfAbsent(key, mutableListOf())
            map[key]!!.add(it)
        }

        map.values.filter { it.isNotEmpty() }.forEach {
            runBlocking {
                sammenlignLinjer(vedtak = vedtak, it)
            }
        }
    }

    private fun sammenlignLinjer(
        vedtak: VedtakDto,
        utbetalingslinjer: MutableList<Utbetalingslinje>,
    ) {
        (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto).utbetalingsperioder.forEach { fraVedtak ->
            val korresponderendeUtbetalingslinje =
                utbetalingslinjer
                    .filter { YearMonth.from(it.periode.fra) == fraVedtak.periode.fom }
                    .maxByOrNull { it.opprettet }
            if (korresponderendeUtbetalingslinje == null) {
                throw IllegalStateException(
                    "Mangler korresponderende utbetalingslinje for periode ${fraVedtak.periode} for vedtak ${vedtak.id}",
                )
            }
            verifiserAtStemmerOverens(vedtak.id, fraVedtak, korresponderendeUtbetalingslinje)
        }
    }

    private fun sammenlignLinjer(
        utbetaling: Utbetaling,
        vedtak: VedtakDto,
    ) {
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

    private fun verifiserAtStemmerOverens(
        vedtakId: Long,
        fraVedtak: Utbetalingsperiode,
        korresponderendeUtbetalingslinje: Utbetalingslinje,
    ) {
        check(fraVedtak.beloep == korresponderendeUtbetalingslinje.beloep) {
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
}
