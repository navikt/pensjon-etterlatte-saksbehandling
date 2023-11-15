package no.nav.etterlatte.rivers

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import no.nav.etterlatte.rapidsandrivers.migrering.BREV_OPPRETTA_MIGRERING
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.hendelseData
import no.nav.etterlatte.token.Systembruker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import java.util.UUID

internal class OpprettVedtaksbrevForMigreringRiver(
    rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService,
) : ListenerMedLoggingOgFeilhaandtering(BrevEventTypes.OPPRETTET.name) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseType.ATTESTERT.toString()) {
            validate { it.requireKey("vedtak.behandling.id") }
            validate { it.requireKey("vedtak.sak.id") }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireValue(KILDE_KEY, Vedtaksloesning.PESYS.name) }
            validate { it.requireValue(BREV_OPPRETTA_MIGRERING, false) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakId = packet["vedtak.sak.id"].asLong()
        logger.info("Oppretter vedtaksbrev i sak $sakId")
        val behandlingId = packet["vedtak.behandling.id"].asUUID()
        val brukerTokenInfo = Systembruker("migrering", "migrering")
        runBlocking {
            val vedtaksbrev: Brev = service.opprettVedtaksbrev(sakId, behandlingId, brukerTokenInfo)
            val hendelseData = packet.hendelseData
            service.genererPdf(vedtaksbrev.id, brukerTokenInfo, hendelseData)
            service.ferdigstillVedtaksbrev(vedtaksbrev.behandlingId!!, brukerTokenInfo, true)
        }
        logger.info("Har oppretta vedtaksbrev i sak $sakId")
        packet[BREV_OPPRETTA_MIGRERING] = true
        context.publish(packet.toJson())
    }
}

fun JsonNode.asUUID(): UUID = UUID.fromString(this.asText())
