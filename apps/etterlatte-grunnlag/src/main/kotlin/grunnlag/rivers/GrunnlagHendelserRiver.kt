package no.nav.etterlatte.grunnlag.rivers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.FNR_KEY
import no.nav.etterlatte.rapidsandrivers.GRUNNLAG_OPPDATERT
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.OPPLYSNING_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.VILKAARSVURDERT_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class GrunnlagHendelserRiver(
    rapidsConnection: RapidsConnection,
    private val grunnlagService: GrunnlagService,
) : ListenerMedLogging() {
    init {
        initialiserRiverUtenEventName(rapidsConnection) {
            validate { it.interestedIn(EVENT_NAME_KEY) }
            validate { it.interestedIn(BEHOV_NAME_KEY) }
            validate { it.interestedIn(FNR_KEY) }
            validate { it.requireKey(OPPLYSNING_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.rejectValue(EVENT_NAME_KEY, GRUNNLAG_OPPDATERT) }
            validate { it.rejectValue(EVENT_NAME_KEY, EventNames.FEILA.lagEventnameForType()) }
            validate { it.rejectValue(VILKAARSVURDERT_KEY, true) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val eventName = packet[EVENT_NAME_KEY].asText()
        val opplysningType = packet[BEHOV_NAME_KEY].asText()

        if (eventName == EventNames.NY_OPPLYSNING.eventname || opplysningType in OPPLYSNING_TYPER) {
            val sakId = packet[SAK_ID_KEY].asLong().let { SakId(it) }
            val behandlingId = packet[BEHANDLING_ID_KEY].let { UUID.fromString(it.asText()) }

            val opplysninger: List<Grunnlagsopplysning<JsonNode>> =
                objectMapper.readValue(packet[OPPLYSNING_KEY].toJson())!!

            val fnr = packet[FNR_KEY].textValue()
            if (fnr == null) {
                grunnlagService.lagreNyeSaksopplysninger(
                    sakId,
                    behandlingId,
                    opplysninger,
                )
            } else {
                grunnlagService.lagreNyePersonopplysninger(
                    sakId,
                    behandlingId,
                    Folkeregisteridentifikator.of(fnr),
                    opplysninger,
                )
            }
            packet.eventName = GRUNNLAG_OPPDATERT
            context.publish(packet.toJson())
        }
    }

    companion object {
        private val OPPLYSNING_TYPER = Opplysningstype.entries.map { it.name }
    }
}
