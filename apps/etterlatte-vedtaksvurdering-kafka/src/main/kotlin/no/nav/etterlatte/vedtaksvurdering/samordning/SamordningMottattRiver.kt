package no.nav.etterlatte.vedtaksvurdering.samordning

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import org.slf4j.LoggerFactory

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

        try {
            vedtaksvurderingService.samordnetVedtak(vedtakId)?.let {
                logger.info("Behandlet samordning ferdig for vedtak [behandlingId=${it.behandlingId}]")
            } ?: {
                logger.info("Samordning skippet for vedtak [vedtakId=$vedtakId]")
            }
        } catch (e: Exception) {
            logger.error("Feil ved oppdatering av vedtak til SAMORDNET [vedtakId=$vedtakId]")
            throw e
        }
    }
}
