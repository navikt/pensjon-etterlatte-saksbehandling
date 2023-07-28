package rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class ListenerMedLogging(private val rapidsConnection: RapidsConnection) : River.PacketListener {

    protected val logger: Logger = LoggerFactory.getLogger(this::class.java)
    fun initialiser(spesifikkInitialisering: River.() -> Unit) {
        val navn = this.javaClass.simpleName
        logger.info("Initialiserer rapid for $navn")
        River(rapidsConnection).apply {
            correlationId()
            spesifikkInitialisering()
        }.register(this)
        logger.info("Initialisert ferdig rapid for $navn")
    }

    abstract fun haandterPakke(packet: JsonMessage, context: MessageContext): Any
    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            haandterPakke(packet, context)
        }
}