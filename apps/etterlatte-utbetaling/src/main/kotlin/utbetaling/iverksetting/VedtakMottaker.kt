package no.nav.etterlatte.utbetaling.iverksetting

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.domene.vedtak.VedtakType
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.SendtTilOppdrag
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.UtbetalingForVedtakEksisterer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.UtbetalingslinjerForVedtakEksisterer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingsvedtak
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

data class KunneIkkeLeseVedtakException(val e: Exception) : RuntimeException(e)

const val EVENT_NAME_OPPDATERT = "UTBETALING:OPPDATERT"

data class UtbetalingEvent(
    @JsonProperty(eventNameKey) val event: String = EVENT_NAME_OPPDATERT,
    @JsonProperty("utbetaling_response") val utbetalingResponse: UtbetalingResponse
)

data class UtbetalingResponse(
    val status: UtbetalingStatus,
    val vedtakId: Long? = null,
    val behandlingId: UUID? = null,
    val feilmelding: String? = null
)

class VedtakMottaker(
    rapidsConnection: RapidsConnection,
    private val utbetalingService: UtbetalingService
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            eventName("VEDTAK:ATTESTERT")
            validate { it.requireKey("vedtak") }
            validate {
                it.requireAny(
                    "vedtak.type",
                    listOf(VedtakType.INNVILGELSE.name, VedtakType.OPPHOER.name, VedtakType.ENDRING.name)
                )
            }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            var vedtakId: Long? = null
            try {
                val vedtak: Utbetalingsvedtak = lesVedtak(packet).also { vedtakId = it.vedtakId }
                logger.info("Attestert vedtak med vedtakId=${vedtak.vedtakId} mottatt")

                when (val resultat = utbetalingService.iverksettUtbetaling(vedtak)) {
                    is SendtTilOppdrag -> {
                        logger.info("Vedtak med vedtakId=${vedtak.vedtakId} sendt til oppdrag - avventer kvittering")
                        sendUtbetalingSendtEvent(context, resultat.utbetaling)
                    }
                    is UtbetalingForVedtakEksisterer -> {
                        val feilmelding =
                            "Vedtak med vedtakId=${vedtak.vedtakId} eksisterer fra før. " +
                                "behandlingId for nytt vedtak: ${vedtak.behandling.id} - " +
                                "behandlingId for tidligere utbetaling: " +
                                "${resultat.eksisterendeUtbetaling.behandlingId.value}"
                        feilmelding.let {
                            logger.error(it)
                            sendUtbetalingFeiletEvent(
                                context,
                                vedtak.vedtakId,
                                vedtak.behandling.id,
                                it
                            )
                        }
                    }
                    is UtbetalingslinjerForVedtakEksisterer -> {
                        (
                            "En eller flere utbetalingslinjer med id=[${resultat.utbetalingslinjeIDer()}] " +
                                "eksisterer fra før"
                            ).also {
                            logger.error(it)
                            sendUtbetalingFeiletEvent(
                                context,
                                vedtak.vedtakId,
                                vedtak.behandling.id,
                                it
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                val feilmelding = "En feil oppstod under prosessering av vedtak med vedtakId=$vedtakId: ${e.message}"
                logger.error(feilmelding, e)
                sendUtbetalingFeiletEvent(context, vedtakId, null, feilmelding)

                if (feilSkalKastesVidere(e)) throw e
            }
        }

    private fun lesVedtak(packet: JsonMessage): Utbetalingsvedtak =
        try {
            objectMapper.readValue(packet["vedtak"].toJson())
        } catch (e: Exception) {
            throw KunneIkkeLeseVedtakException(e)
        }

    private fun feilSkalKastesVidere(e: Exception): Boolean {
        return e !is KunneIkkeLeseVedtakException
    }

    private fun sendUtbetalingFeiletEvent(
        context: MessageContext,
        vedtakId: Long? = null,
        behandlingId: UUID?,
        beskrivelse: String
    ) {
        context.publish(
            UtbetalingEvent(
                utbetalingResponse = UtbetalingResponse(
                    status = UtbetalingStatus.FEILET,
                    vedtakId = vedtakId,
                    behandlingId = behandlingId,
                    feilmelding = beskrivelse
                )
            ).toJson()
        )
    }

    private fun sendUtbetalingSendtEvent(context: MessageContext, utbetaling: Utbetaling) {
        context.publish(
            UtbetalingEvent(
                utbetalingResponse = UtbetalingResponse(
                    status = utbetaling.status(),
                    vedtakId = utbetaling.vedtakId.value,
                    behandlingId = utbetaling.behandlingId.value
                )
            ).toJson()
        )
    }

    private fun UtbetalingslinjerForVedtakEksisterer.utbetalingslinjeIDer() =
        this.utbetalingslinjer.joinToString(",") { it.id.value.toString() }

    companion object {
        private val logger = LoggerFactory.getLogger(VedtakMottaker::class.java)
    }
}