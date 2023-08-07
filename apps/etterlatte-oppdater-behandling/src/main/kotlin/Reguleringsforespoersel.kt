package no.nav.etterlatte

import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames.FINN_LOEPENDE_YTELSER
import no.nav.etterlatte.rapidsandrivers.EventNames.REGULERING_EVENT_NAME
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import rapidsandrivers.DATO_KEY
import rapidsandrivers.dato
import rapidsandrivers.migrering.RiverMedLoggingOgFeilhaandtering
import rapidsandrivers.sakId
import rapidsandrivers.tilbakestilteBehandlinger

internal class Reguleringsforespoersel(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService
) : RiverMedLoggingOgFeilhaandtering(rapidsConnection, REGULERING_EVENT_NAME) {

    override fun River.eventName() = eventName(hendelsestype)

    override fun River.validation() {
        validate { it.requireKey(DATO_KEY) }
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        logger.info("Leser reguleringsfoerespoersel for dato ${packet.dato}")

        val tilbakemigrerte =
            behandlingService.migrerAlleTempBehandlingerTilbakeTilVilkaarsvurdert().also { sakIdListe ->
                logger.info(
                    "Tilbakemigrert ${sakIdListe.ider.size} behandlinger:\n" +
                        sakIdListe.ider.joinToString("\n") { "Sak ${it.sakId} - ${it.behandlingId}" }
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