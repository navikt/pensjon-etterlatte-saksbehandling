package no.nav.etterlatte.utbetaling.iverksetting


import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.utbetaling.config.JmsConnectionFactory
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragJaxb
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.vedtakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.OppdaterKvitteringResultat.KvitteringOppdatert
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.OppdaterKvitteringResultat.UgyldigStatus
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.OppdaterKvitteringResultat.UtbetalingFinnesIkke
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.helse.rapids_rivers.RapidsConnection
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory
import javax.jms.ExceptionListener
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.Session


class KvitteringMottaker(
    private val rapidsConnection: RapidsConnection,
    private val utbetalingService: UtbetalingService,
    jmsConnectionFactory: JmsConnectionFactory,
    queue: String,
) : MessageListener {

    init {
        val connection = jmsConnectionFactory.connection().apply {
            exceptionListener = ExceptionListener {
                logger.error("En feil oppstod med tilkobling mot MQ: ${it.message}", it)
            }
        }.also { it.start() }

        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        val consumer = session.createConsumer(session.createQueue(queue))
        consumer.messageListener = this
    }

    override fun onMessage(message: Message) {
        withLogContext {
            runBlocking {
                delay(1000) // TODO race condition - kvittering kommer før utbetaling er lagret

                var oppdragXml: String? = null

                try {
                    logger.info("Kvittering på utbetaling fra Oppdrag mottatt med id=${message.jmsMessageID}")
                    oppdragXml = message.getBody(String::class.java)
                    val oppdrag = OppdragJaxb.toOppdrag(oppdragXml)

                    logger.info("Kvittering mottatt fra Oppdrag", kv("oppdragXml", oppdragXml))

                    when (val resultat = utbetalingService.oppdaterKvittering(oppdrag)) {
                        is KvitteringOppdatert -> {
                            when (oppdrag.mmel.alvorlighetsgrad) {
                                "00" -> oppdragGodkjent(oppdrag)
                                "04" -> oppdragGodkjentMedFeil(oppdrag)
                                "08" -> oppdragAvvist(oppdrag)
                                "12" -> oppdragFeilet(oppdrag, oppdragXml)
                                else -> oppdragFeilet(oppdrag, oppdragXml)
                            }
                        }
                        is UtbetalingFinnesIkke -> {
                            val feilmelding = "Finner ingen utbetaling for vedtakId=${resultat.vedtakId}"
                            logger.error(feilmelding)
                            sendUtbetalingEvent(UtbetalingStatus.FEILET, oppdrag.vedtakId(), feilmelding)
                        }
                        is UgyldigStatus -> {
                            val feilmelding = """
                                Utbetalingen for vedtakId=${oppdrag.vedtakId()} har feil status (${resultat.status})
                            """.trimIndent()
                            logger.error(feilmelding)
                            sendUtbetalingEvent(UtbetalingStatus.FEILET, oppdrag.vedtakId(), feilmelding)
                        }
                    }

                    logger.info("Melding med id=${message.jmsMessageID} er lest og behandlet")

                } catch (e: Exception) {
                    val feilmelding = "En feil oppstod under prosessering av kvittering fra Oppdrag"
                    logger.error(feilmelding, kv("oppdragXml", oppdragXml), e)
                    sendUtbetalingEvent(UtbetalingStatus.FEILET, feilmelding = feilmelding)
                }
            }
        }
    }

    private fun oppdragGodkjent(oppdrag: Oppdrag) {
        logger.info("Utbetaling med vedtakId=${oppdrag.vedtakId()} godkjent")
        sendUtbetalingEvent(UtbetalingStatus.GODKJENT, oppdrag.vedtakId())
    }

    private fun oppdragGodkjentMedFeil(oppdrag: Oppdrag) {
        logger.info("Utbetaling med vedtakId=${oppdrag.vedtakId()} godkjent med feil")
        sendUtbetalingEvent(UtbetalingStatus.GODKJENT_MED_FEIL, oppdrag.vedtakId(), oppdrag.kvitteringFeilmelding())
    }

    private fun oppdragAvvist(oppdrag: Oppdrag) {
        logger.info("Utbetaling med vedtakId=${oppdrag.vedtakId()} avvist")
        sendUtbetalingEvent(UtbetalingStatus.AVVIST, oppdrag.vedtakId(), oppdrag.kvitteringFeilmelding())
    }

    private fun oppdragFeilet(oppdrag: Oppdrag, oppdragXml: String) {
        logger.info("Utbetaling med vedtakId=${oppdrag.vedtakId()} feilet", kv("oppdrag", oppdragXml))
        sendUtbetalingEvent(UtbetalingStatus.FEILET, oppdrag.vedtakId(), oppdrag.kvitteringFeilmelding())
    }

    private fun sendUtbetalingEvent(status: UtbetalingStatus, vedtakId: Long? = null, feilmelding: String? = null) =
        rapidsConnection.publish("key",
            UtbetalingEvent(
                utbetalingResponse = UtbetalingResponse(
                    status = status,
                    vedtakId = vedtakId,
                    feilmelding = feilmelding
                )
            ).toJson()
        )

    private fun Oppdrag.kvitteringFeilmelding() = "${this.mmel.kodeMelding} ${this.mmel.beskrMelding}"

    companion object {
        private val logger = LoggerFactory.getLogger(KvitteringMottaker::class.java)
    }
}

