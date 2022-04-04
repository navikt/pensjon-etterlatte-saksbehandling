package no.nav.etterlatte.oppdrag


import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.common.Jaxb
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.RapidsConnection
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import javax.jms.Connection
import javax.jms.Session


class KvitteringMottaker(
    private val rapidsConnection: RapidsConnection,
    private val utbetalingsoppdragDao: UtbetalingsoppdragDao,
    jmsConnection: Connection,
    queue: String,
) {
    private val session = jmsConnection.createSession(false, Session.CLIENT_ACKNOWLEDGE)
    private val consumer = session.createConsumer(session.createQueue(queue))

    init {
        withLogContext {
            consumer.setMessageListener { message ->
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

                    message.acknowledge()
                    logger.info("Melding med id=${message.jmsMessageID} er lest og behandlet")

                } catch (t: Throwable) {
                    logger.info("Mottatt melding fra MQ: $oppdragXml") // TODO hva med fnr og slikt i denne meldingen? securelogs?
                    logger.error("Feilet under mottak av kvittering på utbetalingsoppdrag fra Oppdrag", t)
                }
            }
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

