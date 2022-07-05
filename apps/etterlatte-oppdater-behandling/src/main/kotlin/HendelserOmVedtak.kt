package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class HendelserOmVedtak(
    rapidsConnection: RapidsConnection,
    private val behandlinger: Behandling,

    ) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(OppdaterBehandling::class.java)

    private val vedtakhendelser = listOf(
        "VEDTAK:FATTET", "VEDTAK:ATTESTERT", "VEDTAK:UNDERKJENT", "VEDTAK:AVKORTET",
        "VEDTAK:BEREGNET", "VEDTAK:VILKAARSVURDERT"
    )

    init {
        River(rapidsConnection).apply {
            validate { it.demandAny("@event", vedtakhendelser) }
            validate { it.requireKey("@eventtimestamp") }
            validate { it.requireKey("@behandlingId") }
            validate { it.requireKey("@vedtakId") }
            validate { it.requireKey("@sakId") }
            validate { it.interestedIn("@correlation_id") }
            validate { it.rejectKey("@feil") }
            validate { it.interestedIn("@saksbehandler", "@kommentar", "@valgtBegrunnelse")}
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val behandling = UUID.fromString(packet["@behandlingId"].textValue())
            val hendelse = packet["@event"].textValue()
            val vedtakId = packet["@vedtakId"].longValue()
            val saksbehandler = packet["@saksbehandler"].textValue()
            val kommentar = packet["@kommentar"].textValue()
            val valgtBegrunnelse = packet["@valgtBegrunnelse"].textValue()
            val inntruffet = objectMapper.treeToValue<Tidspunkt>(packet["@eventtimestamp"])




            logger.info("""Oppdaterer behandling $behandling med hendelse  $hendelse""")
            try {
                behandlinger.vedtakHendelse(
                    UUID.fromString(packet["@behandlingId"].textValue()),
                    hendelse.split(":").last(),
                    vedtakId,
                    inntruffet,
                    saksbehandler,
                    kommentar,
                    valgtBegrunnelse
                )
            } catch (e: Exception){
                //TODO endre denne
                println("spiser en melding fordi: $e")
            }
        }
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()