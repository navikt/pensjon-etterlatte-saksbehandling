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
            validate { it.requireKey("opplysning") }
            validate { it.requireKey("sak") }
            validate { it.rejectKey("grunnlag") }
            validate { it.rejectKey("@event_name") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: no.nav.helse.rapids_rivers.JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            if (Kontekst.get().AppUser !is Self) {
                logger.warn("AppUser i kontekst er ikke Self i R&R-flyten")
            }

            try {

                val opplysninger: List<Grunnlagsopplysning<ObjectNode>> =
                    objectMapper.readValue(packet["opplysning"].toJson())!!

                // Send melding om behov som er avhengig av en anne opplysning
                opplysninger.forEach {
                    if (it.opplysningType === Opplysningstyper.AVDOED_PDL_V1) {
                        sendAvdoedInntektBehov(it, context, packet)
                    }
                }

                //TODO Her bør jeg vel lage en ny melding
                val grunnlag = grunnlag.opprettGrunnlag(packet["sak"].asLong(), opplysninger)
                packet["grunnlag"] = grunnlag
                packet["@grunnlag"] = grunnlag
                packet["@event_name"] = "GRUNNLAG:GRUNNLAGENDRET"
                context.publish(packet.toJson())
                logger.info("Lagt ut melding om grunnlagsendring")
            } catch (e: Exception) {
                logger.error("Spiser en melding fordi: " + e.message)
            }
        }

    private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()

    private fun sendAvdoedInntektBehov(
        grunnlagsopplysning: Grunnlagsopplysning<ObjectNode>,
        context: MessageContext,
        packet: JsonMessage
    ) {
        val pdlopplysninger = objectMapper.readValue<Person>(grunnlagsopplysning.opplysning.toString())
        if (pdlopplysninger.doedsdato != null) {
            val behov = JsonMessage.newMessage(
                mapOf(
                    "@behov" to Opplysningstyper.AVDOED_INNTEKT_V1,
                    "fnr" to pdlopplysninger.foedselsnummer.value,
                    "sak" to packet["sak"],
                    "doedsdato" to pdlopplysninger.doedsdato.toString(),
                    "@correlation_id" to packet["@correlation_id"]
                )
            )
            context.publish(behov.toJson())
        }
    }
}

