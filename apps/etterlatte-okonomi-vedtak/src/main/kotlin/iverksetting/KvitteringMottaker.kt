package no.nav.etterlatte.iverksetting


import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.config.JmsConnectionFactory
import no.nav.etterlatte.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.iverksetting.oppdrag.vedtakId
import no.nav.etterlatte.iverksetting.oppdrag.OppdragJaxb
import no.nav.etterlatte.iverksetting.utbetaling.UtbetalingService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import javax.jms.ExceptionListener
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.Session


class KvitteringMottaker(
    private val utbetalingService: UtbetalingService,
    jmsConnectionFactory: JmsConnectionFactory,
    queue: String,
) : MessageListener {

    init {
        withLogContext {
            val connection = jmsConnectionFactory.connection().apply {
                // TODO hvordan forsøke å sette opp connection på nytt?
                exceptionListener = ExceptionListener {
                    logger.error("En feil oppstod med tilkobling mot MQ: ${it.message}", it)
                }
            }.also { it.start() }

            val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            val consumer = session.createConsumer(session.createQueue(queue))
            consumer.messageListener = this
        }
    }

    override fun onMessage(message: Message) {
        var oppdragXml: String? = null

        try {
            logger.info("Kvittering på utbetalingsoppdrag fra Oppdrag mottatt med id=${message.jmsMessageID}")
            oppdragXml = message.getBody(String::class.java)
            val oppdrag = OppdragJaxb.toOppdrag(oppdragXml)

            utbetalingService.oppdaterKvittering(oppdrag)

            when (oppdrag.mmel.alvorlighetsgrad) {
                "00" -> oppdragAkseptert(oppdrag)
                "04" -> oppdragAkseptertMedFeil(oppdrag)
                "08" -> oppdragAvvist(oppdrag)
                "12" -> oppdragFeilet(oppdrag, oppdragXml)
                else -> oppdragFeiletUkjent(oppdrag, oppdragXml)
            }

            logger.info("Melding med id=${message.jmsMessageID} er lest og behandlet")

        } catch (t: Throwable) {
            logger.error("Feilet under mottak av kvittering fra Oppdrag", kv("oppdragXml", oppdragXml), t)
        }
    }

    private fun oppdragAkseptert(oppdrag: Oppdrag) {
        logger.info("Utbetalingsoppdrag med id=${oppdrag.vedtakId()} akseptert")
        utbetalingService.oppdaterStatusOgPubliserKvittering(oppdrag, UtbetalingStatus.GODKJENT)
    }

    private fun oppdragAkseptertMedFeil(oppdrag: Oppdrag) {
        logger.info("Utbetalingsoppdrag med id=${oppdrag.vedtakId()} akseptert med feil")
        utbetalingService.oppdaterStatusOgPubliserKvittering(oppdrag, UtbetalingStatus.GODKJENT_MED_FEIL)
    }

    private fun oppdragAvvist(oppdrag: Oppdrag) {
        logger.info("Utbetalingsoppdrag med id=${oppdrag.vedtakId()} avvist")
        utbetalingService.oppdaterStatusOgPubliserKvittering(oppdrag, UtbetalingStatus.AVVIST)
    }

    private fun oppdragFeilet(oppdrag: Oppdrag, oppdragXml: String) {
        logger.info("Utbetalingsoppdrag med id=${oppdrag.vedtakId()} feilet", kv("iverksetting", oppdragXml))
        utbetalingService.oppdaterStatusOgPubliserKvittering(oppdrag, UtbetalingStatus.FEILET)
    }

    private fun oppdragFeiletUkjent(oppdrag: Oppdrag, oppdragXml: String) {
        // TODO bør denne håndteres på noen annen måte?
        logger.info(
            "Utbetalingsoppdrag med id=${oppdrag.vedtakId()} feilet med ukjent feil",
            kv("iverksetting", oppdragXml)
        )
        utbetalingService.oppdaterStatusOgPubliserKvittering(oppdrag, UtbetalingStatus.FEILET)
    }


    companion object {
        private val logger = LoggerFactory.getLogger(KvitteringMottaker::class.java)
    }
}

