package no.nav.etterlatte.rivers

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevEventTypes
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.kilde
import no.nav.etterlatte.token.Systembruker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import java.util.UUID

internal class OpprettVedtaksbrevForMigrering(
    rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService,
) : ListenerMedLoggingOgFeilhaandtering(BrevEventTypes.OPPRETTET.name) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(KafkaHendelseType.ATTESTERT.toString())
            validate { it.requireKey("vedtak.behandling.id") }
            validate { it.requireKey("vedtak.sak.id") }
            validate { it.interestedIn(KILDE_KEY) }
            correlationId()
        }.register(this)
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        packet.eventName = BrevEventTypes.FERDIGSTILT.name
        if (packet.erMigrering()) {
            val sakId = packet["vedtak.sak.id"].asLong()
            logger.info("Oppretter vedtaksbrev i sak $sakId")
            val behandlingId = packet["vedtak.behandling.id"].asUUID()
            val brukerTokenInfo = Systembruker("migrering", "migrering")
            runBlocking {
                val vedtaksbrev: Brev = service.opprettVedtaksbrev(sakId, behandlingId, brukerTokenInfo)
                service.genererPdf(vedtaksbrev.id, brukerTokenInfo)
            }
            logger.info("Har oppretta vedtaksbrev i sak $sakId")
        } else {
            logger.info("Er ikke migrering, så brevet fins allerede. Sender til journalføring.")
        }
        context.publish(packet.toJson())
    }
}

private fun JsonMessage.erMigrering(): Boolean = kilde == Vedtaksloesning.PESYS

fun JsonNode.asUUID(): UUID = UUID.fromString(this.asText())
