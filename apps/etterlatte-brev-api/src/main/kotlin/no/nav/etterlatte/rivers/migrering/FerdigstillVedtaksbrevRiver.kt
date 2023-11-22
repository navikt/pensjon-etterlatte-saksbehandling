package no.nav.etterlatte.rivers.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.rivers.BrevEventTypes
import no.nav.etterlatte.token.Systembruker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import java.util.UUID

internal class FerdigstillVedtaksbrevRiver(
    rapidsConnection: RapidsConnection,
    private val service: VedtaksbrevService,
) : ListenerMedLoggingOgFeilhaandtering(BrevEventTypes.PDF_GENERERT.toString()) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevEventTypes.PDF_GENERERT.toString()) {
            validate { it.requireKey("vedtak.behandlingId") }
            validate { it.requireValue(KILDE_KEY, Vedtaksloesning.PESYS.name) }
            validate { it.rejectValue(BREV_FERDIGSTILT, true) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = UUID.fromString(packet["vedtak.behandlingId"].asText())
        logger.info("Oppretter vedtaksbrev for behandling $behandlingId")
        runBlocking {
            service.ferdigstillVedtaksbrev(behandlingId, Systembruker("migrering", "migrering"), true)
        }
        logger.info("Har oppretta vedtaksbrev for behandling $behandlingId")
        packet[BREV_FERDIGSTILT] = true
        context.publish(packet.toJson())
    }
}

private const val BREV_FERDIGSTILT = "BREV_FERDIGSTILT"
