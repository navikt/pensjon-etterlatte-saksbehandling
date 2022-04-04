package no.nav.etterlatte.oppdrag

import no.nav.etterlatte.domain.Attestasjon
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import no.nav.etterlatte.domain.Vedtak
import org.slf4j.LoggerFactory

class OppdragService(
    val oppdragMapper: OppdragMapper,
    val oppdragSender: OppdragSender,
    val utbetalingsoppdragDao: UtbetalingsoppdragDao,
) {
    fun opprettOgSendOppdrag(vedtak: Vedtak, attestasjon: Attestasjon) {
        val oppdrag = oppdragMapper.oppdragFraVedtak(vedtak, attestasjon)

        logger.info("Oppretter utbetalingsoppdrag for sakId=${vedtak.sakId} med vedtakId=${vedtak.vedtakId}")
        utbetalingsoppdragDao.opprettUtbetalingsoppdrag(vedtak, oppdrag)

        logger.info("Sender oppdrag for sakId=${vedtak.sakId} med vedtakId=${vedtak.vedtakId} til oppdrag")
        oppdragSender.sendOppdrag(oppdrag)

        utbetalingsoppdragDao.oppdaterStatus(oppdrag.vedtakId(), UtbetalingsoppdragStatus.SENDT)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VedtakMottaker::class.java)
    }
}