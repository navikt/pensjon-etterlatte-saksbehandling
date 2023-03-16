package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames.FINN_LOEPENDE_YTELSER
import no.nav.etterlatte.rapidsandrivers.EventNames.REGULERING_EVENT_NAME
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.DATO_KEY
import rapidsandrivers.dato
import rapidsandrivers.sakId
import rapidsandrivers.tilbakestilt

internal class Reguleringsforespoersel(
    rapidsConnection: RapidsConnection,
    private val behandlingService: Behandling
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(Reguleringsforespoersel::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(REGULERING_EVENT_NAME)
            validate { it.requireKey(DATO_KEY) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            logger.info("Leser reguleringsfoerespoersel for dato ${packet.dato}")

            val tilbakemigrerte = behandlingService.migrerAlleTempBehandlingerTilbakeTilVilkaarsvurdert()
            behandlingService.hentAlleSaker().saker.forEach {
                packet.eventName = FINN_LOEPENDE_YTELSER
                packet.tilbakestilt = tilbakemigrerte.contains(it.id)
                packet.sakId = it.id
                context.publish(packet.toJson())
            }
        }
}