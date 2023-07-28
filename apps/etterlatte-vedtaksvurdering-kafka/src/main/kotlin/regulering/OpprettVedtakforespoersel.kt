package no.nav.etterlatte.regulering

import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.EventNames.FATT_VEDTAK
import no.nav.etterlatte.rapidsandrivers.EventNames.OPPRETT_VEDTAK
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.DATO_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLogging
import rapidsandrivers.sakId
import rapidsandrivers.withFeilhaandtering
import java.util.*

internal class OpprettVedtakforespoersel(
    rapidsConnection: RapidsConnection,
    private val vedtak: VedtakService
) : ListenerMedLogging(rapidsConnection) {
    private val logger = LoggerFactory.getLogger(OpprettVedtakforespoersel::class.java)

    init {
        initialiser {
            eventName(OPPRETT_VEDTAK)
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(DATO_KEY) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
        }
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        val sakId = packet.sakId
        logger.info("Leser opprett-vedtak forespoersel for sak $sakId")

        val behandlingId = packet.behandlingId
        withFeilhaandtering(packet, context, OPPRETT_VEDTAK) {
            val respons = vedtak.upsertVedtak(behandlingId)
            logger.info("Opprettet vedtak ${respons.vedtakId} for sak: $sakId og behandling: $behandlingId")
        }
            .takeIf { it.isSuccess }
            ?.let {
                fattVedtak(packet, context, behandlingId, sakId)
                    .takeIf { it.isSuccess }
                    ?.let { attester(packet, context, behandlingId, sakId) }
            }
    }

    private fun fattVedtak(
        packet: JsonMessage,
        context: MessageContext,
        behandlingId: UUID,
        sakId: Long
    ) = withFeilhaandtering(packet, context, FATT_VEDTAK) {
        val fattetVedtak = vedtak.fattVedtak(behandlingId)
        logger.info(
            "Fattet vedtak ${fattetVedtak.vedtakId} for sak: $sakId og behandling: $behandlingId"
        )
    }

    private fun attester(
        packet: JsonMessage,
        context: MessageContext,
        behandlingId: UUID,
        sakId: Long
    ) {
        withFeilhaandtering(packet, context, EventNames.ATTESTER) {
            val attestert = vedtak.attesterVedtak(behandlingId)
            logger.info(
                "Attesterte vedtak ${attestert.vedtakId} for sak: $sakId og behandling: $behandlingId"
            )
        }
    }
}