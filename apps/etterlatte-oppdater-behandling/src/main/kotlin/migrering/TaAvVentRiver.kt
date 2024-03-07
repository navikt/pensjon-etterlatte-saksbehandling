package no.nav.etterlatte.migrering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.VentefristGaarUtRequest
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.OPPGAVE_ID_FLERE_KEY
import no.nav.etterlatte.rapidsandrivers.asUUID
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.etterlatte.rapidsandrivers.migrering.OPPGAVEKILDE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.OPPGAVETYPE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Ventehendelser
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection

internal class TaAvVentRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) :
    ListenerMedLoggingOgFeilhaandtering() {
    init {
        initialiserRiver(rapidsConnection, Ventehendelser.TA_AV_VENT) {
            validate { it.interestedIn(OPPGAVE_ID_FLERE_KEY) }
            validate { it.requireKey(DATO_KEY) }
            validate { it.requireKey(OPPGAVEKILDE_KEY) }
            validate { it.requireKey(OPPGAVETYPE_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val respons =
            behandlingService.taAvVent(
                VentefristGaarUtRequest(
                    dato = packet.dato,
                    type = setOf(OppgaveType.valueOf(packet[OPPGAVETYPE_KEY].asText())),
                    oppgaveKilde = setOf(OppgaveKilde.valueOf(packet[OPPGAVEKILDE_KEY].asText())),
                    oppgaver = packet[OPPGAVE_ID_FLERE_KEY].map { it.asUUID() },
                ),
            )
        respons.behandlinger.forEach {
            packet.sakId = it.sakId
            packet.behandlingId = it.behandlingId
            packet[OPPGAVEKILDE_KEY] = it.oppgavekilde
            packet.eventName = Ventehendelser.TATT_AV_VENT.lagEventnameForType()
            context.publish(packet.toJson())
        }
    }
}
