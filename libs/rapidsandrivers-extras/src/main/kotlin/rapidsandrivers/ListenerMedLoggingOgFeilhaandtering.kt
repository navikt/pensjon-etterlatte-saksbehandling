package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

abstract class ListenerMedLoggingOgFeilhaandtering : River.PacketListener {
    private val logger = LoggerFactory.getLogger(this::class.java)

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

    protected fun initialiserRiver(
        rapidsConnection: RapidsConnection,
        hendelsestype: EventnameHendelseType,
        block: River.() -> Unit = {},
    ) {
        logger.info("Initialiserer river for ${this.javaClass.simpleName}")
        krev(
            kontekst() in
                setOf(
                    Kontekst.MIGRERING,
                    Kontekst.REGULERING,
                    Kontekst.OMREGNING,
                    Kontekst.TEST,
                ),
        ) {
            "Bruk heller ${ListenerMedLogging::class.simpleName}, denne her svelger feilmeldinger"
        }
        River(rapidsConnection)
            .apply {
                eventName(hendelsestype.lagEventnameForType())
                correlationId()
                block()
            }.register(this)
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
}
