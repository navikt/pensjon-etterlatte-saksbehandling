package no.nav.etterlatte.oppdrag

import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import no.nav.etterlatte.domain.Vedtak
import org.slf4j.LoggerFactory

class OppdragService(
    val oppdragMapper: OppdragMapper,
    val oppdragSender: OppdragSender,
    val utbetalingsoppdragDao: UtbetalingsoppdragDao,
) {
    fun opprettOgSendOppdrag(vedtak: Vedtak) {
        val oppdrag = oppdragMapper.oppdragFraVedtak(vedtak)

        logger.info("Oppretter utbetalingsoppdrag for sakId=${vedtak.sakId} med vedtakId=${vedtak.vedtakId}")
        utbetalingsoppdragDao.opprettUtbetalingsoppdrag(vedtak, oppdrag)

        logger.info("Sender oppdrag for sakId=${vedtak.sakId} med vedtakId=${vedtak.vedtakId}")
        oppdragSender.sendOppdrag(oppdrag)

        logger.info("Oppdaterer status")
        utbetalingsoppdragDao.oppdaterStatus(oppdrag.vedtakId(), UtbetalingsoppdragStatus.SENDT)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VedtakMottaker::class.java)
    }
}