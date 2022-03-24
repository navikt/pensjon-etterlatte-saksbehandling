package no.nav.etterlatte.vedtaksoversetter


import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import javax.jms.Connection
import javax.jms.Session


internal class KvitteringMottaker(
    private val rapidsConnection: RapidsConnection,
    jmsConnection: Connection,
    queue: String,
) {
    private val session = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    private val consumer = session.createConsumer(session.createQueue(queue))

    init {
        withLogContext {
            consumer.setMessageListener { message ->
               try {
                   logger.info("Kvittering på utbetalingsoppdrag fra Oppdrag mottatt")
                   val oppdragXml = message.getBody(String::class.java)
                   val oppdrag = Jaxb.toOppdrag(oppdragXml)

                   when (oppdrag.mmel.alvorlighetsgrad) {
                       "00", "04" -> oppdragGodkjent(oppdrag)
                       else -> oppdragFeilet(oppdrag, oppdragXml)
                   }
               } catch (t: Throwable) {
                   logger.error("Feilet under mottak av kvittering på utbetalingsoppdrag fra Oppdrag", t)
               }
            }
        }
    }

    private fun oppdragGodkjent(oppdrag: Oppdrag) {
        logger.info("Utbetalingsoppdrag med id=${oppdrag.oppdrag110.oppdragGjelderId} godkjent")
        // TODO lagre status i db og publiser melding på kafka
    }

    private fun oppdragFeilet(oppdrag: Oppdrag, oppdragXml: String) {
        logger.info("Utbetalingsoppdrag med id=${oppdrag.oppdrag110.oppdragGjelderId} feilet", kv("oppdrag", oppdragXml))
        // TODO lagre status i db og publiser melding på kafka
    }

    companion object {
        private val logger = LoggerFactory.getLogger(KvitteringMottaker::class.java)
    }

}

