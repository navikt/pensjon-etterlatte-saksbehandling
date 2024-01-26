package no.nav.etterlatte.migrering.start

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.LOPENDE_JANUAR_2024_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_KJORING_VARIANT
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.MIGRER_SPESIFIKK_SAK
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.START_MIGRERING
import no.nav.etterlatte.rapidsandrivers.migrering.loependeJanuer2024
import no.nav.etterlatte.rapidsandrivers.migrering.migreringKjoringVariant
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.SAK_ID_FLERE_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import rapidsandrivers.sakIdFlere

internal class StartMigreringRiver(rapidsConnection: RapidsConnection) :
    ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, START_MIGRERING.lagEventnameForType()) {
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
                        EVENT_NAME_KEY to MIGRER_SPESIFIKK_SAK.lagEventnameForType(),
                        SAK_ID_KEY to it,
                        LOPENDE_JANUAR_2024_KEY to packet.loependeJanuer2024,
                        MIGRERING_KJORING_VARIANT to packet.migreringKjoringVariant.name,
                    ),
                )
            context.publish(melding.toJson())
        }
}
