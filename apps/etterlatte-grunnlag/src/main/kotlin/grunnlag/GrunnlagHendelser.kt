package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GrunnlagHendelser(
    rapidsConnection: RapidsConnection,
    private val grunnlagService: GrunnlagService
) : River.PacketListener {
    private val logger: Logger = LoggerFactory.getLogger(GrunnlagHendelser::class.java)

    init {
        River(rapidsConnection).apply {
            correlationId()
            validate { it.interestedIn(EVENT_NAME_KEY) }
            validate { it.interestedIn(BEHOV_NAME_KEY) }
            validate { it.interestedIn("fnr") }
            validate { it.requireKey("opplysning") }
            validate { it.requireKey("sakId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val opplysningsTyper = Opplysningstype.values().map { it.name }

        if ((packet[EVENT_NAME_KEY].asText() == "OPPLYSNING:NY") || (
                opplysningsTyper.contains(
                    packet[BEHOV_NAME_KEY].asText()
                )
                )
        ) {
            withLogContext(packet.correlationId) {
                try {
                    val sakId = packet["sakId"].asLong()
                    val opplysninger: List<Grunnlagsopplysning<JsonNode>> =
                        objectMapper.readValue(packet["opplysning"].toJson())!!

                    val fnr = packet["fnr"].textValue()
                    if (fnr == null) {
                        grunnlagService.lagreNyeSaksopplysninger(
                            sakId,
                            opplysninger
                        )
                    } else {
                        grunnlagService.lagreNyePersonopplysninger(
                            sakId,
                            Foedselsnummer.of(fnr),
                            opplysninger
                        )
                    }

                    JsonMessage.newMessage(
                        eventName = "GRUNNLAG:GRUNNLAGENDRET",
                        map = mapOf(CORRELATION_ID_KEY to packet[CORRELATION_ID_KEY], "sakId" to sakId)
                    ).also {
                        context.publish(it.toJson())
                        logger.info("Lagt ut melding om grunnlagsendring")
                    }
                } catch (e: Exception) {
                    logger.error("Spiser en melding på grunn av feil", e)
                }
            }
        }
    }
}