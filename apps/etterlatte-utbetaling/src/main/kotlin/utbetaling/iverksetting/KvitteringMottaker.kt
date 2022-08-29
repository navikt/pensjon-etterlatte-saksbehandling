package no.nav.etterlatte.utbetaling.iverksetting

import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.utbetaling.config.JmsConnectionFactory
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragJaxb
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.sakId
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.vedtakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.OppdaterKvitteringResultat.KvitteringOppdatert
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.OppdaterKvitteringResultat.UgyldigStatus
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.OppdaterKvitteringResultat.UtbetalingFinnesIkke
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
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
    queue: String
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
                var oppdragXml: String? = null

                try {
                    logger.info("Kvittering på utbetaling fra Oppdrag mottatt med id=${message.jmsMessageID}")
                    oppdragXml = message.getBody(String::class.java)
                    val oppdrag = OppdragJaxb.toOppdrag(oppdragXml)

                    logger.info("Kvittering mottatt fra Oppdrag", kv("oppdragXml", oppdragXml))

                    when (val resultat = utbetalingService.oppdaterKvittering(oppdrag)) {
                        is KvitteringOppdatert -> {
                            oppdrag.haandterMottattKvittering(resultat.utbetaling, oppdragXml)
                        }
                        is UtbetalingFinnesIkke -> {
                            "Finner ingen utbetaling for vedtakId=${resultat.vedtakId}".also {
                                logger.error(it)
                                sendUtbetalingFeiletEvent(it, oppdrag, null)
                            }
                        }
                        is UgyldigStatus -> {
                            """Utbetalingen for vedtakId=${oppdrag.vedtakId()} har feil status (${resultat.status})
                            """.trimIndent().also {
                                logger.error(it)
                                sendUtbetalingFeiletEvent(it, oppdrag, resultat.utbetaling)
                            }
                        }
                    }

                    logger.info("Melding med id=${message.jmsMessageID} er lest og behandlet")
                } catch (e: Exception) {
                    "En feil oppstod under prosessering av kvittering fra Oppdrag".also {
                        logger.error(it, kv("oppdragXml", oppdragXml), e)
                        sendUtbetalingFeiletEvent(it)
                    }
                }
            }
        }
    }

    private fun oppdragGodkjent(utbetaling: Utbetaling) {
        logger.info("Utbetaling med vedtakId=${utbetaling.vedtakId.value} godkjent")
        sendUtbetalingEvent(utbetaling)
    }

    private fun oppdragGodkjentMedFeil(utbetaling: Utbetaling) {
        logger.info("Utbetaling med vedtakId=${utbetaling.vedtakId.value} godkjent med feil")
        sendUtbetalingEvent(utbetaling)
    }

    private fun oppdragAvvist(utbetaling: Utbetaling) {
        logger.info("Utbetaling med vedtakId=${utbetaling.vedtakId.value} avvist")
        sendUtbetalingEvent(utbetaling)
    }

    private fun oppdragFeilet(utbetaling: Utbetaling, oppdragXml: String) {
        logger.info("Utbetaling med vedtakId=${utbetaling.vedtakId.value} feilet", kv("oppdrag", oppdragXml))
        sendUtbetalingEvent(utbetaling)
    }

    private fun sendUtbetalingEvent(utbetaling: Utbetaling) =
        rapidsConnection.publish(
            utbetaling.sakId.value.toString(),
            UtbetalingEvent(
                utbetalingResponse = UtbetalingResponse(
                    status = utbetaling.status(),
                    vedtakId = utbetaling.vedtakId.value,
                    behandlingId = utbetaling.behandlingId.value,
                    feilmelding = utbetaling.kvitteringFeilmelding()
                )
            ).toJson()
        )

    private fun sendUtbetalingFeiletEvent(
        feilmelding: String,
        oppdrag: Oppdrag? = null,
        utbetaling: Utbetaling? = null
    ) =
        rapidsConnection.publish(
            oppdrag?.sakId() ?: "",
            UtbetalingEvent(
                utbetalingResponse = UtbetalingResponse(
                    status = UtbetalingStatus.FEILET,
                    vedtakId = oppdrag?.vedtakId(),
                    behandlingId = utbetaling?.behandlingId?.value,
                    feilmelding = feilmelding
                )
            ).toJson()
        )

    private fun Utbetaling.kvitteringFeilmelding() = when (this.kvittering?.alvorlighetsgrad) {
        "00" -> null
        else -> "${this.kvittering?.kode} ${this.kvittering?.beskrivelse}"
    }

    private fun Oppdrag.haandterMottattKvittering(utbetaling: Utbetaling, oppdragXml: String) =
        when (this.mmel.alvorlighetsgrad) {
            "00" -> oppdragGodkjent(utbetaling)
            "04" -> oppdragGodkjentMedFeil(utbetaling)
            "08" -> oppdragAvvist(utbetaling)
            "12" -> oppdragFeilet(utbetaling, oppdragXml)
            else -> oppdragFeilet(utbetaling, oppdragXml)
        }

    companion object {
        private val logger = LoggerFactory.getLogger(KvitteringMottaker::class.java)
    }
}