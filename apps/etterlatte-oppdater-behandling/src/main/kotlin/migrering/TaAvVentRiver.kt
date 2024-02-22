package no.nav.etterlatte.migrering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.VentefristGaarUtRequest
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.etterlatte.rapidsandrivers.migrering.Ventehendelser
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection

internal class TaAvVentRiver(rapidsConnection: RapidsConnection, private val behandlingService: BehandlingService) :
    ListenerMedLoggingOgFeilhaandtering() {
    init {
        initialiserRiver(rapidsConnection, Ventehendelser.TA_AV_VENT) {
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(DATO_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        behandlingService.taAvVent(
            VentefristGaarUtRequest(
                dato = packet.dato,
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                oppgaveKilde = OppgaveKilde.GJENOPPRETTING,
            ),
        )
    }
}
