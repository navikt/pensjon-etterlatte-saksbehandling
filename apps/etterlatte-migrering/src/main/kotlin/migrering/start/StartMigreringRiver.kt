package no.nav.etterlatte.migrering.start

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.rapidsandrivers.migrering.LOPENDE_JANUAR_2024_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_KJORING_VARIANT
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.loependeJanuer2024
import no.nav.etterlatte.rapidsandrivers.migrering.migreringKjoringVariant
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import rapidsandrivers.SAK_ID_FLERE_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.sakIdFlere

internal class StartMigreringRiver(rapidsConnection: RapidsConnection) :
    ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.START_MIGRERING) {
            validate { it.requireKey(SAK_ID_FLERE_KEY) }
            validate { it.requireKey(LOPENDE_JANUAR_2024_KEY) }
            validate { it.requireKey(MIGRERING_KJORING_VARIANT) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) = runBlocking {
        packet.sakIdFlere
    }.also { logger.info("Hentet ${it.size} saker") }
        .forEach {
            val melding =
                JsonMessage.newMessage(
                    mapOf(
                        Migreringshendelser.MIGRER_SPESIFIKK_SAK.lagParMedEventNameKey(),
                        SAK_ID_KEY to it,
                        LOPENDE_JANUAR_2024_KEY to packet.loependeJanuer2024,
                        MIGRERING_KJORING_VARIANT to packet.migreringKjoringVariant.name,
                    ),
                )
            context.publish(melding.toJson())
        }
}
