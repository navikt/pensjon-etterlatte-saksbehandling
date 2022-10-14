package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndretMedGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.rapidsandrivers.behovNameKey
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationIdKey
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
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
            validate { it.interestedIn(eventNameKey) }
            validate { it.interestedIn(behovNameKey) }
            validate { it.interestedIn("fnr") }
            validate { it.requireKey("opplysning") }
            validate { it.requireKey("sakId") }
            validate { it.rejectKey(BehandlingGrunnlagEndretMedGrunnlag.grunnlagKey) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val opplysningsTyper = Opplysningstype.values().map { it.name }

        if ((packet[eventNameKey].asText() == "OPPLYSNING:NY") || (
            opplysningsTyper.contains(
                    packet[behovNameKey].asText()
                )
            )
        ) {
            withLogContext(packet.correlationId) {
                try {
                    val sakId = packet["sakId"].asLong()
                    val opplysninger: List<Grunnlagsopplysning<JsonNode>> =
                        objectMapper.readValue(packet["opplysning"].toJson())!!
                    // Send melding om behov som er avhengig av en annen opplysning
                    opplysninger.forEach {
                        if (it.opplysningType === Opplysningstype.AVDOED_PDL_V1) {
                            sendAvdoedInntektBehov(it, context, packet)
                        }
                    }

                    grunnlagService.lagreNyeOpplysninger(
                        sakId,
                        packet["fnr"].textValue()?.let { Foedselsnummer.of(it) },
                        opplysninger
                    )

                    JsonMessage.newMessage(
                        eventName = "GRUNNLAG:GRUNNLAGENDRET",
                        map = mapOf(correlationIdKey to packet[correlationIdKey], "sakId" to sakId)
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

    private fun sendAvdoedInntektBehov(
        grunnlagsopplysning: Grunnlagsopplysning<JsonNode>,
        context: MessageContext,
        packet: JsonMessage
    ) {
        val pdlopplysninger = objectMapper.readValue<Person>(grunnlagsopplysning.opplysning.toString())
        if (pdlopplysninger.doedsdato != null) {
            val behov = JsonMessage.newMessage(
                mapOf(
                    behovNameKey to Opplysningstype.INNTEKT,
                    "fnr" to pdlopplysninger.foedselsnummer.value,
                    "sakId" to packet["sakId"],
                    "doedsdato" to pdlopplysninger.doedsdato.toString(),
                    correlationIdKey to packet[correlationIdKey]
                )
            )
            context.publish(behov.toJson())
        }
    }
}