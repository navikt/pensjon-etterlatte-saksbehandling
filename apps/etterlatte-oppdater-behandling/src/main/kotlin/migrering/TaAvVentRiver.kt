package no.nav.etterlatte.migrering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.VentefristGaarUtRequest
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.OPPGAVE_ID_FLERE_KEY
import no.nav.etterlatte.rapidsandrivers.OPPGAVE_KEY
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
import org.slf4j.LoggerFactory
import java.util.UUID

internal class TaAvVentRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
    private val featureToggleService: FeatureToggleService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

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
        if (!featureToggleService.isEnabled(VentFeatureToggle.TaAvVent, false)) {
            logger.warn("Ta av vent er skrudd av. Avbryter fra TaAvVentRiver")
            return
        }
        val oppgavetyper = setOf(OppgaveType.valueOf(packet[OPPGAVETYPE_KEY].asText()))
        val oppgavekilder = setOf(OppgaveKilde.valueOf(packet[OPPGAVEKILDE_KEY].asText()))
        logger.info("Tar av vent med oppgavetyper $oppgavetyper og oppgavekilder $oppgavekilder for ${packet.dato}")
        val respons =
            behandlingService.taAvVent(
                VentefristGaarUtRequest(
                    dato = packet.dato,
                    type = oppgavetyper,
                    oppgaveKilde = oppgavekilder,
                    oppgaver = packet[OPPGAVE_ID_FLERE_KEY].map { it.asUUID() },
                ),
            )
        logger.info("Tok ${respons.behandlinger.size} oppgaver av vent")
        logger.debug("Oppgavene tatt av vent er {}", respons.behandlinger.map { it.oppgaveID })
        respons.behandlinger.forEach {
            if (!featureToggleService.isEnabled(VentFeatureToggle.TaAvVent, false)) {
                logger.warn("Ta av vent er skrudd av. Avbryter fra TaAvVentRiver for oppgave ${it.oppgaveID} i sak ${packet.sakId}")
                return
            }
            packet.sakId = it.sakId
            packet.behandlingId = UUID.fromString(it.referanse)
            packet[OPPGAVEKILDE_KEY] = it.oppgavekilde
            packet[OPPGAVE_KEY] = it.oppgaveID
            packet.eventName = Ventehendelser.TATT_AV_VENT.lagEventnameForType()
            logger.debug("Oppgave {} for behandling {} tatt av vent. Sender melding videre", packet[OPPGAVE_KEY], packet.behandlingId)
            context.publish(packet.toJson())
        }
    }
}

enum class VentFeatureToggle(private val key: String) : FeatureToggle {
    TaAvVent("ta-av-vent"),
    ;

    override fun key() = key
}
