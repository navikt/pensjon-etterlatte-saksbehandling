package no.nav.etterlatte.utbetaling.iverksetting

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.utbetaling.common.UtbetalingEventDto
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.SendtTilOppdrag
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.UtbetalingForVedtakEksisterer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.UtbetalingslinjerForVedtakEksisterer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingsvedtak
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

data class KunneIkkeLeseVedtakException(val e: Exception) : RuntimeException(e)

class VedtakMottaker(
    rapidsConnection: RapidsConnection,
    private val utbetalingService: UtbetalingService
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            eventName(KafkaHendelseType.ATTESTERT.toString())
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
                val feilmelding = "En feil oppstod under prosessering av vedtak med vedtakId=$vedtakId"
                logger.error(feilmelding, e)
                sendUtbetalingFeiletEvent(context, vedtakId, null, feilmelding)

                if (feilSkalKastesVidere(e)) throw e
            }
        }

    private fun lesVedtak(packet: JsonMessage): Utbetalingsvedtak =
        try {
            val vedtak: VedtakDto = objectMapper.readValue(packet["vedtak"].toJson())
            Utbetalingsvedtak.fra(vedtak)
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
            UtbetalingEventDto(
                utbetalingResponse = UtbetalingResponseDto(
                    status = UtbetalingStatusDto.FEILET,
                    vedtakId = vedtakId,
                    behandlingId = behandlingId,
                    feilmelding = beskrivelse
                )
            ).toJson()
        )
    }

    private fun sendUtbetalingSendtEvent(context: MessageContext, utbetaling: Utbetaling) {
        context.publish(
            UtbetalingEventDto(
                utbetalingResponse = UtbetalingResponseDto(
                    status = UtbetalingStatusDto.valueOf(utbetaling.status().name),
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