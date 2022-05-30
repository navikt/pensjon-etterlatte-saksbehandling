package no.nav.etterlatte.utbetaling.iverksetting


import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.utbetaling.config.JmsConnectionFactory
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragJaxb
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.vedtakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import okio.ByteString.Companion.toByteString
import org.slf4j.LoggerFactory
import java.util.*
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
        runBlocking {
            delay(2000) // TODO race condition - kvittering kommer før utbetaling er lagret

            var oppdragXml: String? = null

            try {
                logger.info("Kvittering på utbetaling fra Oppdrag mottatt med id=${message.jmsMessageID}")
                oppdragXml = message.getBody(String::class.java)
                val oppdrag = OppdragJaxb.toOppdrag(oppdragXml)

                logger.info("Kvittering mottatt fra Oppdrag", kv("oppdragXml", oppdrag))
                logger.info("Kvittering mottatt fra Oppdrag", kv("oppdragXml", oppdragXml))

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
    }

    private fun oppdragAkseptert(oppdrag: Oppdrag) {
        logger.info("Utbetaling med id=${oppdrag.vedtakId()} godkjent")
        utbetalingService.oppdaterStatusOgPubliserKvittering(oppdrag, UtbetalingStatus.GODKJENT)
    }

    private fun oppdragAkseptertMedFeil(oppdrag: Oppdrag) {
        logger.info("Utbetaling med id=${oppdrag.vedtakId()} godkjent med feil")
        utbetalingService.oppdaterStatusOgPubliserKvittering(oppdrag, UtbetalingStatus.GODKJENT_MED_FEIL)
    }

    private fun oppdragAvvist(oppdrag: Oppdrag) {
        logger.info("Utbetaling med id=${oppdrag.vedtakId()} avvist")
        utbetalingService.oppdaterStatusOgPubliserKvittering(oppdrag, UtbetalingStatus.AVVIST)
    }

    private fun oppdragFeilet(oppdrag: Oppdrag, oppdragXml: String) {
        logger.info("Utbetaling med id=${oppdrag.vedtakId()} feilet", kv("oppdrag", oppdragXml))
        utbetalingService.oppdaterStatusOgPubliserKvittering(oppdrag, UtbetalingStatus.FEILET)
    }

    private fun oppdragFeiletUkjent(oppdrag: Oppdrag, oppdragXml: String) {
        logger.info(
            "Utbetaling med id=${oppdrag.vedtakId()} feilet med ukjent feil",
            kv("utbetaling/iverksetting", oppdragXml)
        )
        utbetalingService.oppdaterStatusOgPubliserKvittering(oppdrag, UtbetalingStatus.FEILET)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(KvitteringMottaker::class.java)
    }
}

