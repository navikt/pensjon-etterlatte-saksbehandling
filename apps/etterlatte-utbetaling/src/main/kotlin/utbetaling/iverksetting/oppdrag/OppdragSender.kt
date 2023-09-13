package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import no.nav.etterlatte.mq.EtterlatteJmsConnectionFactory
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory

class OppdragSender(
    private val jmsConnectionFactory: EtterlatteJmsConnectionFactory,
    private val queue: String,
    private val replyQueue: String
) {
    fun sendOppdrag(oppdrag: Oppdrag): String {
        logger.info("Sender utbetaling til Oppdrag")
        logger.info(
            "Sender oppdrag for sakId=${oppdrag.oppdrag110.fagsystemId} med " +
                "vedtakId=${oppdrag.oppdrag110.oppdragsLinje150.first().vedtakId} til oppdrag"
        )

        val xml = OppdragJaxb.toXml(oppdrag)
        jmsConnectionFactory.sendMedSvar(
            xml = xml,
            queue = queue,
            replyQueue = replyQueue
        ).also { logger.info("Utbetaling overført til oppdrag") }
        return xml
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OppdragSender::class.java)
    }
}