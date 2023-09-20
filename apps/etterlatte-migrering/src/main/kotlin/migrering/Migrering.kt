package no.nav.etterlatte.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.migrering.pen.PenKlient
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.MIGRER_SPESIFIKK_SAK
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.START_MIGRERING
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import rapidsandrivers.sakId

internal class Migrering(rapidsConnection: RapidsConnection, private val penKlient: PenKlient) :
    ListenerMedLoggingOgFeilhaandtering(START_MIGRERING) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(hendelsestype)
            correlationId()
        }.register(this)
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) = runBlocking {
        penKlient.hentAlleSaker()
    }.also { logger.info("Hentet ${it.size} saker") }
        .forEach {
            packet.eventName = MIGRER_SPESIFIKK_SAK
            packet.sakId = it.id
            context.publish(packet.toJson())
        }
}
