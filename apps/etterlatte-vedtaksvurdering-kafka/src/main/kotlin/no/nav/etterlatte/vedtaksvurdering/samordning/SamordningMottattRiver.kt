package no.nav.etterlatte.vedtaksvurdering.samordning

import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.ListenerMedLogging

internal class SamordningMottattRiver(
    rapidsConnection: RapidsConnection,
    private val vedtaksvurderingService: VedtakService,
) : ListenerMedLogging() {
    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseHendelseType.SAMORDNING_MOTTATT) {
            validate { it.requireKey("vedtakId") }
        }
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val vedtakId = packet["vedtakId"].asText()
        logger.info("Behandle mottatt samordning for vedtak [vedtakId=$vedtakId]")

        if (vedtakId == "11547") {
            logger.info("Vedtak 11547 er allerede iverksatt, hopper over melding")
            return
        }

        try {
            val samordnetVedtak = vedtaksvurderingService.samordnetVedtak(vedtakId)
            logger.info("Behandlet samordning ferdig for vedtak [behandlingId=${samordnetVedtak.behandlingId}]")
        } catch (e: Exception) {
            logger.error("Feil ved oppdatering av vedtak til SAMORDNET [vedtakId=$vedtakId]")
            throw e
        }
    }
}
