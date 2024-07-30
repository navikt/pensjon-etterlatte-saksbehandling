package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType
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

abstract class ListenerMedLoggingOgFeilhaandtering : River.PacketListener {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg = sikkerlogger()

    abstract fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    )

    abstract fun kontekst(): Kontekst

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) = withLogContext(packet.correlationId) {
        withRetryOgFeilhaandtering(
            packet = packet,
            context = context,
            feilendeSteg = this.name(),
            kontekst = kontekst(),
        ) {
            haandterPakke(packet, context)
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerlogg.debug("Plukka ikke opp meldinga i ${context.rapidName()} fordi ${problems.toExtendedReport()}")
        super.onError(problems, context)
    }

    override fun onSevere(
        error: MessageProblems.MessageException,
        context: MessageContext,
    ) {
        sikkerlogg.error("Klarte ikke å håndtere meldinga i ${context.rapidName()} fordi ${error.problems.toExtendedReport()}", error)
        super.onSevere(error, context)
    }

    protected fun initialiserRiver(
        rapidsConnection: RapidsConnection,
        hendelsestype: EventnameHendelseType,
        block: River.() -> Unit = {},
    ) {
        logger.info("Initialiserer river for ${this.javaClass.simpleName}")
        require(kontekst() in setOf(Kontekst.MIGRERING, Kontekst.REGULERING, Kontekst.TEST)) {
            "Bruk heller ${ListenerMedLogging::class.simpleName}, denne her svelger feilmeldinger"
        }
        River(rapidsConnection)
            .apply {
                eventName(hendelsestype.lagEventnameForType())
                correlationId()
                block()
            }.register(this)
    }
}
