package no.nav.etterlatte.regulering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.FINN_LOEPENDE_YTELSER
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.DATO_KEY
import rapidsandrivers.dato
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import rapidsandrivers.sakId
import rapidsandrivers.tilbakestilteBehandlinger

internal class ReguleringsforespoerselRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) : ListenerMedLoggingOgFeilhaandtering(ReguleringEvents.START_REGULERING) {
    private val logger = LoggerFactory.getLogger(ReguleringsforespoerselRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, hendelsestype) {
            validate { it.requireKey(DATO_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Leser reguleringsfoerespoersel for dato ${packet.dato}")

        val tilbakemigrerte =
            behandlingService.migrerAlleTempBehandlingerTilbakeTilVilkaarsvurdert().also { sakIdListe ->
                logger.info(
                    "Tilbakemigrert ${sakIdListe.ider.size} behandlinger:\n" +
                        sakIdListe.ider.joinToString("\n") { "Sak ${it.sakId} - ${it.behandlingId}" },
                )
            }
        behandlingService.hentAlleSaker().saker.forEach {
            packet.eventName = FINN_LOEPENDE_YTELSER
            packet.tilbakestilteBehandlinger = tilbakemigrerte.behandlingerForSak(it.id)
            packet.sakId = it.id
            context.publish(packet.toJson())
        }
    }
}
