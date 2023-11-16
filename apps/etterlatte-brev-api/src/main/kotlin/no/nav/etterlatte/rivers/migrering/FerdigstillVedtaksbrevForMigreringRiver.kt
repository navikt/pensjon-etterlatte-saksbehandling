package no.nav.etterlatte.rivers.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import no.nav.etterlatte.rapidsandrivers.migrering.BREV_OPPRETTA_MIGRERING
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.rivers.BrevEventTypes
import no.nav.etterlatte.token.Systembruker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import java.util.UUID

internal class FerdigstillVedtaksbrevForMigreringRiver(
    rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService,
) : ListenerMedLoggingOgFeilhaandtering(BrevEventTypes.OPPRETTET.name) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseType.ATTESTERT.toString()) {
            validate { it.requireKey("vedtak.behandlingId") }
            validate { it.requireKey("vedtak.sak.id") }
            validate { it.requireValue(KILDE_KEY, Vedtaksloesning.PESYS.name) }
            validate { it.requireValue(BREV_OPPRETTA_MIGRERING, false) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakId = packet["vedtak.sak.id"].asLong()
        logger.info("Ferdigstill vedtaksbrev i sak $sakId")
        val behandlingId = UUID.fromString(packet["vedtak.behandlingId"].asText())
        val brukerTokenInfo = Systembruker("migrering", "migrering")
        runBlocking {
            service.ferdigstillVedtaksbrev(behandlingId, brukerTokenInfo, true)
        }
        logger.info("Har ferdigstilt vedtaksbrev i sak $sakId")
        packet[BREV_OPPRETTA_MIGRERING] = true
        context.publish(packet.toJson())
    }
}
