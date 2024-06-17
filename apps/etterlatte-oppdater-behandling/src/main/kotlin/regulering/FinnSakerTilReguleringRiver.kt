package no.nav.etterlatte.regulering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.Duration

class FinnSakerTilReguleringRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, ReguleringHendelseType.FINN_SAKER_TIL_REGULERING) {
            validate { it.requireKey(DATO_KEY) }
            validate { it.requireKey(KJOERING) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Finner saker til regulering for dato ${packet.dato}")
        val kjoering = packet[KJOERING].asText()

        kjoerIBatch(
            logger = logger,
            antall = Int.MAX_VALUE,
            finnSaker = { antall ->
                behandlingService.hentAlleSaker(kjoering = kjoering, antall = antall)
            },
            haandterSaker = { saker ->
                saker.saker.forEach { sak ->
                    behandlingService.lagreKjoering(sak.id, KjoeringStatus.KLAR_TIL_REGULERING, kjoering)
                }
            },
            venteperiode = Duration.ofSeconds(1),
        )
        logger.info("Finner saker til regulering for dato ${packet.dato} er ferdig håndtert")
    }

    override fun kontekst() = Kontekst.REGULERING
}
