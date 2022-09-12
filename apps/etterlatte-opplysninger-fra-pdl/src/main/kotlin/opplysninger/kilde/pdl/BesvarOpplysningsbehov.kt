package no.nav.etterlatte.opplysninger.kilde.pdl

import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.behovNameKey
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class BesvarOpplysningsbehov(
    rapidsConnection: RapidsConnection,
    private val pdl: Pdl
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(BesvarOpplysningsbehov::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.requireKey(behovNameKey) }
            validate { it.requireKey("sakId") }
            validate { it.requireKey("fnr") }
            validate { it.requireKey("rolle") }
            validate { it.rejectKey("opplysning") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            if (packet[behovNameKey].asText() in listOf(
                    Opplysningstyper.AVDOED_PDL_V1.name,
                    Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1.name,
                    Opplysningstyper.SOEKER_PDL_V1.name
                )
            ) {
                val fnr = packet["fnr"].textValue()
                val personRolle = objectMapper.treeToValue(packet["rolle"], PersonRolle::class.java)!!
                val behandling = objectMapper.treeToValue(packet[behovNameKey], Opplysningstyper::class.java)!!
                val person = pdl.hentPerson(fnr, personRolle)
                val opplysningsperson = pdl.hentOpplysningsperson(fnr, personRolle)

                packet["opplysning"] = listOf(lagOpplysning(behandling, person)) +
                    lagOpplysninger(opplysningsperson, behovNameTilPersonRolle(behandling), Foedselsnummer.of(fnr))
                context.publish(packet.toJson())

                logger.info("Svarte på et behov av type: " + behandling.name)
            } else {
                logger.info("Så et behov jeg ikke kunne svare på")
            }
        }
}

private fun behovNameTilPersonRolle(opplysningstyper: Opplysningstyper): PersonRolle = when (opplysningstyper) {
    Opplysningstyper.AVDOED_PDL_V1 -> PersonRolle.AVDOED
    Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1 -> PersonRolle.GJENLEVENDE
    Opplysningstyper.SOEKER_PDL_V1 -> PersonRolle.BARN
    else -> throw Exception("Ugyldig opplysningsbehov")
}

interface Pdl {
    fun hentPerson(foedselsnummer: String, rolle: PersonRolle): Person
    fun hentOpplysningsperson(foedselsnummer: String, rolle: PersonRolle): PersonDTO
}