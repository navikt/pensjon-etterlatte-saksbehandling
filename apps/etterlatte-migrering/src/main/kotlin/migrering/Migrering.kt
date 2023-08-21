package no.nav.etterlatte.migrering

import migrering.Sakmigrerer
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.START_MIGRERING
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering

internal class Migrering(
    rapidsConnection: RapidsConnection,
    private val pesysRepository: PesysRepository,
    private val sakmigrerer: Sakmigrerer
) :
    ListenerMedLoggingOgFeilhaandtering(START_MIGRERING) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(hendelsestype)
            correlationId()
        }.register(this)
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) =
        pesysRepository.hentSaker().also { logger.info("Hentet ${it.size} saker") }
            .forEach { sakmigrerer.migrerSak(packet, it, context) }
}