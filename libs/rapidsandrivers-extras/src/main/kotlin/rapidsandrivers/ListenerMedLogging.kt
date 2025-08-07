package no.nav.etterlatte.rapidsandrivers

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.etterlatte.libs.common.event.EventnameHendelseType
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import org.slf4j.LoggerFactory

abstract class ListenerMedLogging : River.PacketListener {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg = sikkerlogger()

    abstract fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ): Any

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) = withLogContext(packet.correlationId) {
        try {
            haandterPakke(packet, context)
        } catch (e: Exception) {
            logger.warn("Fikk feil under handtering av melding. Se sikkerlogg for hele meldinga", e)
            sikkerlogg.warn("Fikk feil under handtering av melding. Meldinga var ${packet.toJson()}", e)
            throw e
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        sikkerlogg.debug("Plukka ikke opp meldinga i ${context.rapidName()} fordi ${problems.toExtendedReport()}")
        super.onError(problems, context, metadata)
    }

    protected fun initialiserRiver(
        rapidsConnection: RapidsConnection,
        hendelsestype: EventnameHendelseType,
        block: River.() -> Unit = {},
    ) {
        initialiserRiverUtenEventName(rapidsConnection, block = {
            correlationId()
            eventName(hendelsestype.lagEventnameForType())
            block()
        })
    }

    protected fun initialiserRiverUtenEventName(
        rapidsConnection: RapidsConnection,
        block: River.() -> Unit = {},
    ) {
        logger.info("Initialiserer river for ${this.javaClass.simpleName}")
        River(rapidsConnection)
            .apply {
                correlationId()
                block()
            }.register(this)
    }

    open fun kontekst(): Kontekst? = null
}
