package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class FerdigstillVedtaksbrev(rapidsConnection: RapidsConnection, private val brevService: BrevService) :
    River.PacketListener {
    private val logger = LoggerFactory.getLogger(FerdigstillVedtaksbrev::class.java)

    init {
        River(rapidsConnection).apply {
            eventName("VEDTAK:ATTESTERT")
            validate { it.requireKey("vedtak") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext {
            val vedtak: Vedtak = objectMapper.readValue(packet["vedtak"].toJson())
            logger.info("Nytt vedtak med id ${vedtak.vedtakId} er attestert. Ferdigstiller vedtaksbrev.")

            val brev = brevService.ferdigstillAttestertVedtak(vedtak)

            logger.info("Vedtaksbrev for vedtak med id ${vedtak.vedtakId} er ferdigstilt (brevId = ${brev.id})")
        }
    }
}