package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory


enum class GrunnlagHendelserType {
    OPPRETTET, GRUNNLAGENDRET, AVBRUTT
}

class GrunnlagHendelser(
    rapidsConnection: RapidsConnection,
    private val grunnlag: GrunnlagFactory,
    //private val datasource: DataSource
) : River.PacketListener {


    private val logger: Logger = LoggerFactory.getLogger(GrunnlagHendelser::class.java)

    init {
        River(rapidsConnection).apply {
            //validate { it.demandValue("@event_name", "ny_opplysning") }
            validate { it.requireKey("opplysning") }
            validate { it.requireKey("sak") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: no.nav.helse.rapids_rivers.JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            if(Kontekst.get().AppUser !is Self){ logger.warn("AppUser i kontekst er ikke Self i R&R-flyten") }

            //TODO fjerne spor av liste
            val lagretGrunnlag = inTransaction {
                val gjeldendeGrunnlag = grunnlag.hent(packet["sak"].asLong())
                val opplysninger: Grunnlagsopplysning<ObjectNode> =
                    objectMapper.readValue(packet["opplysning"].toJson())!!
                gjeldendeGrunnlag.leggTilGrunnlagListe(listOf( opplysninger))
                gjeldendeGrunnlag
            }

            //TODO Her bør jeg vel lage en ny melding
            packet["grunnlag"] = lagretGrunnlag.serialiserbarUtgave()
            packet["@event_name"] = "GRUNNLAG:GRUNNLAGENDRET"
            context.publish(packet.toJson())
            logger.info("Lagt ut melding om grunnlagsendring")
        }

    private fun no.nav.helse.rapids_rivers.JsonMessage.correlationId(): String? = get("@correlation_id").textValue()
}

