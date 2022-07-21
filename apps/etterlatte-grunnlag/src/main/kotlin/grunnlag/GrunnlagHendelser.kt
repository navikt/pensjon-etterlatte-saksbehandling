package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.rapidsandrivers.*
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GrunnlagHendelser(
    rapidsConnection: RapidsConnection,
    private val grunnlag: GrunnlagService,
) : River.PacketListener {
    private val logger: Logger = LoggerFactory.getLogger(GrunnlagHendelser::class.java)

    init {
        River(rapidsConnection).apply {
            correlationId()
            validate { it.interestedIn(eventNameKey) }
            validate { it.interestedIn(behovNameKey) }
            validate { it.requireKey("opplysning") }
            validate { it.requireKey("sakId") }
            validate { it.rejectKey("grunnlag") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val opplysninger = Opplysningstyper.values().map { it.name }

        if ((packet[eventNameKey].asText() == "OPPLYSNING:NY")
            || (opplysninger.contains(packet[behovNameKey].asText()))) {

            withLogContext(packet.correlationId) {
                if (Kontekst.get().AppUser !is Self) {
                    logger.warn("AppUser i kontekst er ikke Self i R&R-flyten")
                }

                try {
                    val opplysninger: List<Grunnlagsopplysning<ObjectNode>> = objectMapper.readValue(packet["opplysning"].toJson())!!

                    // Send melding om behov som er avhengig av en annen opplysning
                    opplysninger.forEach {
                        if (it.opplysningType === Opplysningstyper.AVDOED_PDL_V1) {
                            sendAvdoedInntektBehov(it, context, packet)
                        }
                    }

                    val grunnlag = grunnlag.opprettGrunnlag(packet["sakId"].asLong(), opplysninger)

                    JsonMessage.newMessage("GRUNNLAG:GRUNNLAGENDRET",
                        mapOf(
                            "grunnlag" to grunnlag,
                            correlationIdKey to packet[correlationIdKey],
                            "sakId" to packet["sakId"],
                        )
                    ).apply {
                        context.publish(toJson())
                        logger.info("Lagt ut melding om grunnlagsendring")
                    }
                } catch (e: Exception) {
                    logger.error("Spiser en melding fordi: " + e.message)
                }
            }

        } else {
            return
        }
    }

    private fun sendAvdoedInntektBehov(
        grunnlagsopplysning: Grunnlagsopplysning<ObjectNode>,
        context: MessageContext,
        packet: JsonMessage
    ) {
        val pdlopplysninger = objectMapper.readValue<Person>(grunnlagsopplysning.opplysning.toString())
        if (pdlopplysninger.doedsdato != null) {
            val behov = JsonMessage.newMessage(
                mapOf(
                    behovNameKey to Opplysningstyper.AVDOED_INNTEKT_V1,
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

