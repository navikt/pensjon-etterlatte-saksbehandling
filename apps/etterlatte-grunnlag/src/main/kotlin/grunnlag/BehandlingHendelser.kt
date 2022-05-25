package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BehandlingHendelser(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {


    private val logger: Logger = LoggerFactory.getLogger(GrunnlagHendelser::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event", "BEHANDLING:OPPRETTET") }
            validate { it.requireKey("innsender") }
            validate { it.requireKey("soeker") }
            validate { it.requireKey("gjenlevende") }
            validate { it.requireKey("avdoed") }
            validate { it.requireKey("sak") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            if(Kontekst.get().AppUser !is Self){ logger.warn("AppUser i kontekst er ikke Self i R&R-flyten") }

            //TODO dette må jeg gjøre smartere, ellers må Persongalleri restruktureres
            context.publish(
                JsonMessage.newMessage(
                mapOf(
                    "@behov" to Opplysningstyper.SOEKER_PDL_V1,
                    "sak" to packet["sak"],
                    "fnr" to packet["soeker"],
                    "rolle" to Opplysningstyper.SOEKER_PDL_V1.personRolle!!,
                    "@correlation_id" to packet["@correlation_id"]
                )
            ).toJson())
             packet["gjenlevende"].forEach { fnr ->
                context.publish(
                    JsonMessage.newMessage(
                        mapOf(
                            "@behov" to Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1,
                            "sak" to packet["sak"],
                            "fnr" to fnr.asText(),
                            "rolle" to Opplysningstyper.SOEKER_PDL_V1.personRolle!!,
                            "@correlation_id" to packet["@correlation_id"]
                        )
                    ).toJson()
                )
            }
            packet["avdoed"].forEach { fnr ->
                context.publish(
                    JsonMessage.newMessage(
                        mapOf(
                            "@behov" to Opplysningstyper.AVDOED_PDL_V1,
                            "sak" to packet["sak"],
                            "fnr" to fnr.asText(),
                            "rolle" to Opplysningstyper.AVDOED_PDL_V1.personRolle!!,
                            "@correlation_id" to packet["@correlation_id"]
                        )
                    ).toJson()
                )
            }

        }

    private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()
}
private val Opplysningstyper.personRolle: PersonRolle? get() = when(this){
    Opplysningstyper.AVDOED_SOEKNAD_V1, Opplysningstyper.AVDOED_PDL_V1 -> PersonRolle.AVDOED
    Opplysningstyper.SOEKER_SOEKNAD_V1, Opplysningstyper.SOEKER_PDL_V1 -> PersonRolle.BARN
    Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1 -> PersonRolle.GJENLEVENDE
    else -> null
}

