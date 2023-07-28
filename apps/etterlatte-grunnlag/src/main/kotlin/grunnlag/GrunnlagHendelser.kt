package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.EventNames.FEILA
import no.nav.etterlatte.rapidsandrivers.migrering.VILKAARSVURDERT_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rapidsandrivers.FNR_KEY
import rapidsandrivers.GRUNNLAG_OPPDATERT
import rapidsandrivers.OPPLYSNING_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.migrering.ListenerMedLogging

class GrunnlagHendelser(
    rapidsConnection: RapidsConnection,
    private val grunnlagService: GrunnlagService
) : ListenerMedLogging() {
    private val logger: Logger = LoggerFactory.getLogger(GrunnlagHendelser::class.java)

    init {
        River(rapidsConnection).apply {
            correlationId()
            validate { it.interestedIn(EVENT_NAME_KEY) }
            validate { it.interestedIn(BEHOV_NAME_KEY) }
            validate { it.interestedIn(FNR_KEY) }
            validate { it.requireKey(OPPLYSNING_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.rejectValue(EVENT_NAME_KEY, GRUNNLAG_OPPDATERT) }
            validate { it.rejectValue(EVENT_NAME_KEY, FEILA) }
            validate { it.rejectValue(VILKAARSVURDERT_KEY, true) }
        }.register(this)
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        val eventName = packet[EVENT_NAME_KEY].asText()
        val opplysningType = packet[BEHOV_NAME_KEY].asText()

        if (eventName == "OPPLYSNING:NY" || opplysningType in OPPLYSNING_TYPER) {
            try {
                val sakId = packet[SAK_ID_KEY].asLong()
                val opplysninger: List<Grunnlagsopplysning<JsonNode>> =
                    objectMapper.readValue(packet[OPPLYSNING_KEY].toJson())!!

                val fnr = packet[FNR_KEY].textValue()
                if (fnr == null) {
                    grunnlagService.lagreNyeSaksopplysninger(
                        sakId,
                        opplysninger
                    )
                } else {
                    grunnlagService.lagreNyePersonopplysninger(
                        sakId,
                        Folkeregisteridentifikator.of(fnr),
                        opplysninger
                    )
                }
                packet.eventName = GRUNNLAG_OPPDATERT
                context.publish(packet.toJson())
            } catch (e: Exception) {
                logger.error("Spiser en melding på grunn av feil", e)
            }
        }
    }

    companion object {
        private val OPPLYSNING_TYPER = Opplysningstype.values().map { it.name }
    }
}