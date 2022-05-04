package no.nav.etterlatte.opplysninger.kilde.pdl

import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class BesvarOpplysningsbehov(
    rapidsConnection: RapidsConnection,
    private val pdl: Pdl,
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(BesvarOpplysningsbehov::class.java)
    init {
        River(rapidsConnection).apply {
            validate { it.requireKey("@behov") }
            validate { it.requireKey("behandling") }
            validate { it.requireKey("fnr") }
            validate { it.requireKey("rolle") }
            validate { it.rejectKey("opplysning") }
            validate { it.interestedIn("@correlation_id") }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {

            if(packet["@behov"].asText() in listOf(Opplysningstyper.AVDOED_PDL_V1.name,Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1.name,Opplysningstyper.SOEKER_PDL_V1.name)){
                val personRolle = objectMapper.treeToValue(packet["rolle"], PersonRolle::class.java)!!
                val behandling = objectMapper.treeToValue(packet["@behov"], Opplysningstyper::class.java)!!
                val pdlInfo = pdl.hentPdlModell(packet["fnr"].asText(), personRolle)
                packet["opplysning"] = personOpplysning(pdlInfo, behandling)
                context.publish(packet.toJson())
                logger.info("Svarte på et behov av type: " + behandling.name)
            } else {
                logger.info("Så et behov jeg ikke kunne svare på")
            }

        }
    }

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()

fun personOpplysning(
    personPdl: Person,
    opplysningsType: Opplysningstyper,
): Grunnlagsopplysning<Person> {
    return lagOpplysning(opplysningsType, personPdl )
}