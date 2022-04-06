package no.nav.etterlatte.oppdrag


import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.common.Jaxb
import no.nav.etterlatte.config.JmsConnectionFactory
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.RapidsConnection
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import javax.jms.ExceptionListener
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.Session


class KvitteringMottaker(
    private val rapidsConnection: RapidsConnection,
    private val utbetalingsoppdragDao: UtbetalingsoppdragDao,
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
            val oppdrag = Jaxb.toOppdrag(oppdragXml)

            utbetalingsoppdragDao.oppdaterKvittering(oppdrag)

            when (oppdrag.mmel.alvorlighetsgrad) {
                "00", "04" -> oppdragGodkjent(oppdrag)
                //"08" // noe saksbehandler må håndtere
                //"12" // hånteres av tjenesten
                else -> oppdragFeilet(oppdrag, oppdragXml)
            }

            logger.info("Melding med id=${message.jmsMessageID} er lest og behandlet")

        } catch (t: Throwable) {
            logger.error("Feilet under mottak av kvittering fra Oppdrag", kv("oppdragXml", oppdragXml), t)
        }
    }

    private fun oppdragGodkjent(oppdrag: Oppdrag) {
        logger.info("Utbetalingsoppdrag med id=${oppdrag.vedtakId()} godkjent")
        utbetalingsoppdragDao.oppdaterStatus(oppdrag.vedtakId(), UtbetalingsoppdragStatus.GODKJENT)
        rapidsConnection.publish(mapOf("@event_name" to "utbetaling_godkjent").toJson())
        // TODO utvid melding
    }

    private fun oppdragFeilet(oppdrag: Oppdrag, oppdragXml: String) {
        logger.info("Utbetalingsoppdrag med id=${oppdrag.vedtakId()} feilet", kv("oppdrag", oppdragXml))
        utbetalingsoppdragDao.oppdaterStatus(oppdrag.vedtakId(), UtbetalingsoppdragStatus.FEILET)
        rapidsConnection.publish(mapOf("@event_name" to "utbetaling_feilet").toJson())
        // TODO utvid melding
    }

    companion object {
        private val logger = LoggerFactory.getLogger(KvitteringMottaker::class.java)
    }

}

