package no.nav.etterlatte.regulering

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.rapidsandrivers.sakId
import no.nav.etterlatte.libs.common.rapidsandrivers.sakIdKey
import no.nav.etterlatte.rapidsandrivers.EventNames.OPPRETT_VEDTAK
import no.nav.etterlatte.rapidsandrivers.EventNames.TIL_UTBETALING
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.behandlingId
import rapidsandrivers.behandlingIdKey
import rapidsandrivers.datoKey

internal class OpprettVedtakforespoersel(
    rapidsConnection: RapidsConnection,
    private val vedtak: VedtakService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(OpprettVedtakforespoersel::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(OPPRETT_VEDTAK)
            validate { it.requireKey(sakIdKey) }
            validate { it.requireKey(datoKey) }
            validate { it.requireKey(behandlingIdKey) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val sakId = packet.sakId
            logger.info("Leser opprett-vedtak forespoersel for sak $sakId")

            val behandlingId = packet.behandlingId
            val respons = vedtak.upsertVedtak(behandlingId)

            packet.eventName = TIL_UTBETALING
            context.publish(packet.toJson())
            logger.info("Opprettet vedtak ${respons.vedtakId} og sendte $TIL_UTBETALING for sak: $sakId og behandling: $behandlingId") // ktlint-disable
        }
}