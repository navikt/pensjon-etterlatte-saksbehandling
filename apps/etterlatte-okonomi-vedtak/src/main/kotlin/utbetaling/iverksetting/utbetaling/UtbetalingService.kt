package no.nav.etterlatte.utbetaling.iverksetting.utbetaling

import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragMapper
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragSender
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.vedtakId
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.etterlatte.utbetaling.common.Tidspunkt
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import java.time.Clock

class UtbetalingService(
    val oppdragMapper: OppdragMapper,
    val oppdragSender: OppdragSender,
    val utbetalingDao: UtbetalingDao,
    val rapidsConnection: RapidsConnection,
    val clock: Clock
) {
    fun iverksettUtbetaling(vedtak: Vedtak, attestasjon: Attestasjon): Utbetaling {
        val opprettetTidspunkt = Tidspunkt.now(clock)
        val oppdrag = oppdragMapper.oppdragFraVedtak(vedtak, attestasjon, opprettetTidspunkt)

        logger.info("Sender oppdrag for sakId=${vedtak.sakId} med vedtakId=${vedtak.vedtakId} til oppdrag")
        oppdragSender.sendOppdrag(oppdrag)
        return utbetalingDao.opprettUtbetaling(vedtak, oppdrag, opprettetTidspunkt)
    }

    fun utbetalingEksisterer(vedtak: Vedtak) =
        utbetalingDao.hentUtbetaling(vedtak.vedtakId) != null

    fun oppdaterKvittering(oppdrag: Oppdrag): Utbetaling {
        logger.info("Oppdaterer kvittering for oppdrag med id=${oppdrag.vedtakId()}")
        return utbetalingDao.oppdaterKvittering(oppdrag, Tidspunkt.now(clock))
    }

    fun oppdaterStatusOgPubliserKvittering(oppdrag: Oppdrag, status: UtbetalingStatus) =
        utbetalingDao.oppdaterStatus(oppdrag.vedtakId(), status, Tidspunkt.now(clock))
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