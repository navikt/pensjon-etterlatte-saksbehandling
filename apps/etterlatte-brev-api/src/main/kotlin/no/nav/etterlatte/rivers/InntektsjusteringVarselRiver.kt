package no.nav.etterlatte.rivers

import no.nav.etterlatte.libs.common.event.InntektsjusteringInnsendt
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.InntektsjusteringHendelseEvents
import rapidsandrivers.InntektsjusteringHendelseType

internal class InntektsjusteringVarselRiver(
    rapidsConnection: RapidsConnection,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, InntektsjusteringHendelseType.SEND_VARSEL) {
            validate { it.requireKey(InntektsjusteringHendelseEvents.inntektsaar) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val inntektsaar = packet[InntektsjusteringInnsendt.inntektsaar].textValue()
        logger.info("Starter varsel om inntektsjustering for $inntektsaar")

        // TODO: hente alle saker som skal varsles

        // TODO: sende varsel om inntektsjustering pr sak

        // TODO: marker som varslet?

        // TODO: rapporter tilbake?
    }
}
