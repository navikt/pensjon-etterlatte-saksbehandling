package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

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
            validate { it.demandValue("@event_name", "ny_opplysning") }
            validate { it.requireKey("opplysning") }
            validate { it.requireKey("saksId") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: no.nav.helse.rapids_rivers.JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val gjeldendeGrunnlag = grunnlag.hent(packet["saksId"].asLong())
            val opplysning = objectMapper.treeToValue<Grunnlagsopplysning<ObjectNode>>(packet["opplysning"])!!
            gjeldendeGrunnlag.leggTilGrunnlagListe(listOf(opplysning))
            logger.info("Har gjort et forsøk på å legge til en opplysning tror jeg")
        }

    private fun no.nav.helse.rapids_rivers.JsonMessage.correlationId(): String? = get("@correlation_id").textValue()
}

