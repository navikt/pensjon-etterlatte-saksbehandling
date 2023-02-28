package no.nav.etterlatte.regulering

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.EventNames.OPPRETT_VEDTAK
import no.nav.etterlatte.rapidsandrivers.EventNames.TIL_UTBETALING
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.DATO_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.sakId
import rapidsandrivers.withFeilhaandtering

internal class OpprettVedtakforespoersel(
    rapidsConnection: RapidsConnection,
    private val vedtak: VedtakService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(OpprettVedtakforespoersel::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(OPPRETT_VEDTAK)
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(DATO_KEY) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val sakId = packet.sakId
            logger.info("Leser opprett-vedtak forespoersel for sak $sakId")

            val behandlingId = packet.behandlingId
            withFeilhaandtering(packet, context, OPPRETT_VEDTAK) {
                val respons = vedtak.upsertVedtak(behandlingId)
                logger.info("Opprettet vedtak ${respons.vedtakId}")
            }

            withFeilhaandtering(packet, context, EventNames.FATT_VEDTAK) {
                val fattetVedtak = vedtak.fattVedtak(behandlingId)
                logger.info("Fattet vedtak ${fattetVedtak.vedtakId}")

                packet.eventName = TIL_UTBETALING
                context.publish(packet.toJson())
                logger.info("Fattet vedtak for ${fattetVedtak.vedtakId} og sendte $TIL_UTBETALING for sak: $sakId og behandling: $behandlingId") // ktlint-disable
            }
        }
}