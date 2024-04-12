package no.nav.etterlatte.migrering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.OPPGAVE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Ventehendelser
import no.nav.etterlatte.rapidsandrivers.oppgaveId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.Kontekst

internal class OppdaterMerknadRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Ventehendelser.TATT_AV_VENT_FYLT_20) {
            validate { it.requireKey(OPPGAVE_KEY) }
        }
    }

    override fun kontekst() = Kontekst.MIGRERING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Oppdaterer merknad for oppgave ${packet.oppgaveId}")
        behandlingService.oppdaterStatusOgMerknad(packet.oppgaveId, "Gjenopptak - Manuell behandling pga aldersovergang")
        logger.info("Oppdaterte merknad for oppgave ${packet.oppgaveId}")
    }
}
