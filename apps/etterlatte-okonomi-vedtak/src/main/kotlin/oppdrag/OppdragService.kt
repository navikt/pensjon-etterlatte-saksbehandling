package no.nav.etterlatte.oppdrag

import no.nav.etterlatte.domain.Utbetalingsoppdrag
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.helse.rapids_rivers.RapidsConnection
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class OppdragService(
    val oppdragMapper: OppdragMapper,
    val oppdragSender: OppdragSender,
    val utbetalingsoppdragDao: UtbetalingsoppdragDao,
    val rapidsConnection: RapidsConnection
) {
    fun opprettOgSendOppdrag(vedtak: Vedtak, attestasjon: Attestasjon): Utbetalingsoppdrag {
        val oppdrag = oppdragMapper.oppdragFraVedtak(vedtak, attestasjon)

        logger.info("Sender oppdrag for sakId=${vedtak.sakId} med vedtakId=${vedtak.vedtakId} til oppdrag")
        oppdragSender.sendOppdrag(oppdrag)
        val opprettetTidspunkt = LocalDateTime.now()
        return utbetalingsoppdragDao.opprettUtbetalingsoppdrag(vedtak, oppdrag, opprettetTidspunkt)
    }

    fun oppdragEksistererFraFor(vedtak: Vedtak) =
        utbetalingsoppdragDao.hentUtbetalingsoppdrag(vedtak.vedtakId) != null

    fun oppdaterKvittering(oppdrag: Oppdrag): Utbetalingsoppdrag {
        logger.info("Oppdaterer kvittering for oppdrag med id=${oppdrag.vedtakId()}")
        return utbetalingsoppdragDao.oppdaterKvittering(oppdrag)
    }

    fun oppdaterStatusOgPubliserKvittering(oppdrag: Oppdrag, status: UtbetalingsoppdragStatus) =
        utbetalingsoppdragDao.oppdaterStatus(oppdrag.vedtakId(), status)
            .also { rapidsConnection.publish("key", utbetalingEvent(oppdrag, status)) }

    private fun utbetalingEvent(oppdrag: Oppdrag, status: UtbetalingsoppdragStatus) = mapOf(
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
        private val logger = LoggerFactory.getLogger(OppdragService::class.java)
    }

}