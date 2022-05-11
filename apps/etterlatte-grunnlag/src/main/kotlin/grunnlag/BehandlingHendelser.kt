package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory


enum class BehandlingHendelserType {
    OPPRETTET, GRUNNLAGENDRET, AVBRUTT
}

class BehandlingHendelser(
    rapidsConnection: RapidsConnection,
    private val grunnlag: GrunnlagFactory,
    //private val datasource: DataSource
) : River.PacketListener {


    private val logger: Logger = LoggerFactory.getLogger(GrunnlagHendelser::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "BEHANDLING:OPPRETTET") }
            //TODO Finne ut hva denne er?
            validate { it.requireKey("persongalleri") }
            validate { it.requireKey("saksId") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val persongalleri = objectMapper.treeToValue<Persongalleri>(packet["persongalleri"])!!
            grunnlag.opprett(packet["saksId"].asLong())

            //TODO dette må jeg gjøre smartere, ellers må Persongalleri restruktureres
            context.publish(
                JsonMessage.newMessage(
                mapOf(
                    "@behov" to Opplysningstyper.SOEKER_PDL_V1,
                    "sak" to packet["saksId"],
                    "fnr" to persongalleri.soeker,
                    "rolle" to Opplysningstyper.SOEKER_PDL_V1.personRolle!!,
                    "@correlation_id" to packet["@correlation_id"]
                )
            ).toJson())

            context.publish(
                JsonMessage.newMessage(
                    mapOf(
                        "@behov" to Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1,
                        "sak" to packet["saksId"],
                        "fnr" to persongalleri.soeker,
                        "rolle" to Opplysningstyper.SOEKER_PDL_V1.personRolle!!,
                        "@correlation_id" to packet["@correlation_id"]
                    )
                ).toJson())

        }

    private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()
}
private val Opplysningstyper.personRolle: PersonRolle? get() = when(this){
    Opplysningstyper.AVDOED_SOEKNAD_V1, Opplysningstyper.AVDOED_PDL_V1 -> PersonRolle.AVDOED
    Opplysningstyper.SOEKER_SOEKNAD_V1, Opplysningstyper.SOEKER_PDL_V1 -> PersonRolle.BARN
    Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1 -> PersonRolle.GJENLEVENDE
    else -> null
}

