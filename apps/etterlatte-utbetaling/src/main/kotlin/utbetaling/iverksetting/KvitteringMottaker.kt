package no.nav.etterlatte.utbetaling.iverksetting

import jakarta.jms.Message
import jakarta.jms.MessageListener
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.etterlatte.sikkerLogg
import no.nav.etterlatte.utbetaling.common.EVENT_NAME_OPPDATERT
import no.nav.etterlatte.utbetaling.common.UTBETALING_RESPONSE
import no.nav.etterlatte.utbetaling.common.UtbetalingEventDto
import no.nav.etterlatte.utbetaling.config.EtterlatteJmsConnectionFactory
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragJaxb
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.sakId
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.vedtakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.OppdaterKvitteringResultat.KvitteringOppdatert
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.OppdaterKvitteringResultat.UgyldigStatus
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.OppdaterKvitteringResultat.UtbetalingFinnesIkke
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.slf4j.LoggerFactory

class KvitteringMottaker(
    private val rapidsConnection: RapidsConnection,
    private val utbetalingService: UtbetalingService,
    jmsConnectionFactory: EtterlatteJmsConnectionFactory,
    queue: String
) : MessageListener {

    init {
        jmsConnectionFactory.start(
            listener = {
                logger.error("En feil oppstod med tilkobling mot MQ: ${it.message}", it)
            },
            queue = queue,
            messageListener = this
        )
    }

    override fun onMessage(message: Message) {
        withLogContext {
            runBlocking {
                var oppdragXml: String? = null

                try {
                    logger.info("Kvittering på utbetaling fra Oppdrag mottatt med id=${message.jmsMessageID}")
                    oppdragXml = message.getBody(String::class.java)
                    val oppdrag = OppdragJaxb.toOppdrag(oppdragXml)

                    sikkerLogg.info(
                        "Kvittering på utbetaling fra Oppdrag mottatt med id=${message.jmsMessageID}",
                        kv("oppdragXml", oppdragXml)
                    )

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
                        logger.error(it + ", se detaljer i sikker logg")
                        sikkerLogg.error(it, kv("oppdragXml", oppdragXml), e)
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
        logger.error("Utbetaling med vedtakId=${utbetaling.vedtakId.value} godkjent med feil")
        sendUtbetalingEvent(utbetaling)
    }

    private fun oppdragAvvist(utbetaling: Utbetaling) {
        logger.error("Utbetaling med vedtakId=${utbetaling.vedtakId.value} avvist")
        sendUtbetalingEvent(utbetaling)
    }

    private fun oppdragFeilet(utbetaling: Utbetaling, oppdragXml: String) {
        logger.error("Utbetaling med vedtakId=${utbetaling.vedtakId.value} feilet, se sikkerlogg for kvittering")
        sikkerLogg.error(
            "Utbetaling med vedtakId=${utbetaling.vedtakId.value} feilet, kvittering:",
            kv("oppdrag", oppdragXml)
        )
        sendUtbetalingEvent(utbetaling)
    }

    private fun sendUtbetalingEvent(utbetaling: Utbetaling) = rapidsConnection.publish(
        utbetaling.sakId.value.toString(),
        UtbetalingEventDto(
            utbetalingResponse = UtbetalingResponseDto(
                status = UtbetalingStatusDto.valueOf(utbetaling.status().name),
                vedtakId = utbetaling.vedtakId.value,
                behandlingId = utbetaling.behandlingId.value,
                feilmelding = utbetaling.kvitteringFeilmelding()
            )
        ).toMessage()
    )

    private fun sendUtbetalingFeiletEvent(
        feilmelding: String,
        oppdrag: Oppdrag? = null,
        utbetaling: Utbetaling? = null
    ) = rapidsConnection.publish(
        oppdrag?.sakId() ?: "",
        UtbetalingEventDto(
            utbetalingResponse = UtbetalingResponseDto(
                status = UtbetalingStatusDto.FEILET,
                vedtakId = oppdrag?.vedtakId(),
                behandlingId = utbetaling?.behandlingId?.value,
                feilmelding = feilmelding
            )
        ).toMessage()
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

    private fun UtbetalingResponseDto.toMessage(): Map<String, Any> {
        val utbetalingResponse = mutableMapOf<String, Any>(
            "status" to this.status
        )

        this.vedtakId?.let { utbetalingResponse.put("vedtakId", it) }
        this.behandlingId?.let { utbetalingResponse.put("behandlingId", it) }
        this.feilmelding?.let { utbetalingResponse.put("feilmelding", it) }

        return utbetalingResponse
    }

    private fun UtbetalingEventDto.toMessage(): String {
        return JsonMessage.newMessage(
            mapOf(
                EVENT_NAME_KEY to EVENT_NAME_OPPDATERT,
                UTBETALING_RESPONSE to this.utbetalingResponse.toMessage()
            )
        ).toJson()
    }
}