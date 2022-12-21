package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class FerdigstillVedtaksbrev(rapidsConnection: RapidsConnection, private val service: VedtaksbrevService) :
    River.PacketListener {
    private val logger = LoggerFactory.getLogger(FerdigstillVedtaksbrev::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(KafkaHendelseType.ATTESTERT.toString())
            validate { it.requireKey("vedtak") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        try {
            withLogContext {
                val vedtak: Vedtak = objectMapper.readValue(packet["vedtak"].toJson())
                logger.info("Nytt vedtak med id ${vedtak.vedtakId} er attestert. Ferdigstiller vedtaksbrev.")

                val brevId = service.sendTilDistribusjon(vedtak)

                logger.info("Vedtaksbrev for vedtak med id ${vedtak.vedtakId} er ferdigstilt (brevId=$brevId)")
            }
        } catch (e: Exception) {
            logger.error("Feil ved ferdigstilling av vedtaksbrev: ", e)
            throw e
        }
    }
}