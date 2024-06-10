package no.nav.etterlatte.utbetaling.avstemming.regulering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.utbetaling.VedtaksvurderingKlient
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingslinje
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingslinjeId
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.UUID

class VerifiserReguleringssummer(
    private val repository: UtbetalingDao,
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun verifiserAlle() {
//        repository.hentUtbetalinger()
    }

    fun verifiser(vedtakId: Long) {
        val utbetaling = repository.hentUtbetaling(vedtakId = vedtakId)
        if (utbetaling == null) {
            logger.warn("Ingen utbetaling for vedtak $vedtakId. Returnerer")
            return
        }

        val linjer = utbetaling.utbetalingslinjer.sortedBy { it.opprettet }

        val map = mutableMapOf<UtbetalingslinjeId, MutableList<Utbetalingslinje>>()
        val utbetalingslinjeNull = UtbetalingslinjeId(-1)
        linjer.forEach { map[it.id] = mutableListOf() }
            .also { map[utbetalingslinjeNull] = mutableListOf() }
        linjer.forEach {
            map[it.erstatterId ?: utbetalingslinjeNull]!!.add(it)
        }

        map.entries.filter { it.value.isNotEmpty() }.forEach {
            runBlocking {
                sammenlignLinjer(it.value)
            }
        }
    }

    private suspend fun sammenlignLinjer(utbetalingslinjer: MutableList<Utbetalingslinje>) {
        val vedtak = vedtaksvurderingKlient.hentVedtak(UUID.randomUUID(), Systembruker.automatiskJobb)
        val innhold = vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto
        val perioderFraVedtak = innhold.utbetalingsperioder
        perioderFraVedtak.forEach { fraVedtak ->
            val korresponderendeUtbetalingslinje =
                utbetalingslinjer.filter { YearMonth.from(it.periode.fra) == fraVedtak.periode.fom }
                    .maxByOrNull { it.opprettet }
            if (korresponderendeUtbetalingslinje == null) {
                throw IllegalStateException(
                    "Mangler korresponderende utbetalingslinje for periode ${fraVedtak.periode} for vedtak ${vedtak.id}",
                )
            }
            check(fraVedtak.beloep == korresponderendeUtbetalingslinje.beloep)
            check(fraVedtak.periode.tom == korresponderendeUtbetalingslinje.periode.til?.let { YearMonth.from(it) })
        }
    }
}
