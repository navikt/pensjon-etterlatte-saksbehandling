package rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.withFeilhaandtering

abstract class ListenerMedLoggingOgFeilhaandtering(protected val hendelsestype: String) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg = sikkerlogger()

    abstract fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    )

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) = withLogContext(packet.correlationId) {
        withFeilhaandtering(packet, context, hendelsestype) {
            haandterPakke(packet, context)
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerlogg.warn("Plukka ikke opp meldinga i ${context.rapidName()} fordi ${problems.toExtendedReport()}")
        super.onError(problems, context)
    }

    protected fun initialiserRiver(
        rapidsConnection: RapidsConnection,
        hendelsestype: String,
        block: River.() -> Unit = {},
    ) {
        logger.info("Initialiserer river for ${this.javaClass.simpleName}")
        River(rapidsConnection).apply {
            eventName(hendelsestype)
            correlationId()
            block()
        }.register(this)
    }
}
