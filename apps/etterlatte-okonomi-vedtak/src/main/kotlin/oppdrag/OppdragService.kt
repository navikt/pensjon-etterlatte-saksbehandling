package no.nav.etterlatte.oppdrag

import no.nav.etterlatte.domain.Utbetalingsoppdrag
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.helse.rapids_rivers.RapidsConnection
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory

class OppdragService(
    val oppdragMapper: OppdragMapper,
    val oppdragSender: OppdragSender,
    val utbetalingsoppdragDao: UtbetalingsoppdragDao,
    val rapidsConnection: RapidsConnection
) {
    fun opprettOgSendOppdrag(vedtak: Vedtak, attestasjon: Attestasjon): Utbetalingsoppdrag {
        val oppdrag = oppdragMapper.oppdragFraVedtak(vedtak, attestasjon)

        logger.info("Oppretter utbetalingsoppdrag for sakId=${vedtak.sakId} med vedtakId=${vedtak.vedtakId}")
        utbetalingsoppdragDao.opprettUtbetalingsoppdrag(vedtak, oppdrag)

        logger.info("Sender oppdrag for sakId=${vedtak.sakId} med vedtakId=${vedtak.vedtakId} til oppdrag")
        oppdragSender.sendOppdrag(oppdrag)

        return utbetalingsoppdragDao.oppdaterStatus(oppdrag.vedtakId(), UtbetalingsoppdragStatus.SENDT)
    }

    fun oppdaterKvittering(oppdrag: Oppdrag): Utbetalingsoppdrag {
        logger.info("Oppdaterer kvittering for oppdrag med id=${oppdrag.vedtakId()}")
        return utbetalingsoppdragDao.oppdaterKvittering(oppdrag)
    }

    fun oppdaterStatusOgPubliserKvittering(oppdrag: Oppdrag, status: UtbetalingsoppdragStatus) =
        utbetalingsoppdragDao.oppdaterStatus(oppdrag.vedtakId(), status)
            .also { rapidsConnection.publish(utbetalingEvent(oppdrag, status)) }

    private fun utbetalingEvent(oppdrag: Oppdrag, status: UtbetalingsoppdragStatus) = mapOf(
        "@event_name" to "utbetaling_oppdatert",
        "@status" to status.name,
        "@beskrivelse" to oppdrag.kvitteringBeskrivelse()
    ).toJson()

    private fun Oppdrag.kvitteringBeskrivelse() = "${this.mmel.kodeMelding} ${this.mmel.beskrMelding}"


    companion object {
        private val logger = LoggerFactory.getLogger(OppdragService::class.java)
    }
}