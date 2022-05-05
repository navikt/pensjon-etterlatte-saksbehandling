package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragSender
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.vedtakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.helse.rapids_rivers.RapidsConnection
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class UtbetalingService(
    val oppdragMapper: OppdragMapper,
    val oppdragSender: OppdragSender,
    val utbetalingDao: UtbetalingDao,
    val rapidsConnection: RapidsConnection
) {
    fun iverksettUtbetaling(vedtak: Vedtak, attestasjon: Attestasjon): Utbetaling {
        val opprettetTidspunkt = LocalDateTime.now()
        val oppdrag = OppdragMapper.oppdragFraVedtak(vedtak, attestasjon, avstemmingNokkel = opprettetTidspunkt)

        logger.info("Sender oppdrag for sakId=${vedtak.sakId} med vedtakId=${vedtak.vedtakId} til oppdrag")
        oppdragSender.sendOppdrag(oppdrag)
        return utbetalingDao.opprettUtbetaling(vedtak, oppdrag, opprettetTidspunkt)
    }

    fun utbetalingEksisterer(vedtak: Vedtak) =
        utbetalingDao.hentUtbetaling(vedtak.vedtakId) != null

    fun oppdaterKvittering(oppdrag: Oppdrag): Utbetaling {
        logger.info("Oppdaterer kvittering for oppdrag med id=${oppdrag.vedtakId()}")
        return utbetalingDao.oppdaterKvittering(oppdrag)
    }

    fun oppdaterStatusOgPubliserKvittering(oppdrag: Oppdrag, status: UtbetalingStatus) =
        utbetalingDao.oppdaterStatus(oppdrag.vedtakId(), status)
            .also { rapidsConnection.publish("key", utbetalingEvent(oppdrag, status)) }

    private fun utbetalingEvent(oppdrag: Oppdrag, status: UtbetalingStatus) = mapOf(
        "@event_name" to "utbetaling_oppdatert",
        "@vedtakId" to oppdrag.vedtakId(),
        "@status" to status.name,
        "@beskrivelse" to oppdrag.kvitteringBeskrivelse()
    ).toJson()

    private fun Oppdrag.kvitteringBeskrivelse() = when (this.mmel.kodeMelding) {
        "00" -> "Utbetalingsoppdrag OK"
        else -> "${this.mmel.kodeMelding} ${this.mmel.beskrMelding}"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UtbetalingService::class.java)
    }

}