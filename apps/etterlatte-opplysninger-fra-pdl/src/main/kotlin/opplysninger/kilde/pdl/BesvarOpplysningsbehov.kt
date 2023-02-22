package no.nav.etterlatte.opplysninger.kilde.pdl

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
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
            validate { it.requireKey(BEHOV_NAME_KEY) }
            validate { it.requireKey("sakId") }
            validate { it.requireKey("fnr") }
            validate { it.requireKey("rolle") }
            validate { it.rejectKey("opplysning") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            if (packet[BEHOV_NAME_KEY].asText() in listOf(
                    Opplysningstype.AVDOED_PDL_V1.name,
                    Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1.name,
                    Opplysningstype.SOEKER_PDL_V1.name
                )
            ) {
                val fnr = packet["fnr"].textValue()
                val personRolle = objectMapper.treeToValue(packet["rolle"], PersonRolle::class.java)!!
                val opplysningstype = objectMapper.treeToValue(packet[BEHOV_NAME_KEY], Opplysningstype::class.java)!!
                val person = pdl.hentPerson(fnr, personRolle)
                val opplysningsperson = pdl.hentOpplysningsperson(fnr, personRolle)

                packet["opplysning"] = lagEnkelopplysningerFraPDL(
                    person = person,
                    personDTO = opplysningsperson,
                    opplysningsbehov = opplysningstype,
                    fnr = Foedselsnummer.of(fnr)
                )
                context.publish(packet.toJson())

                logger.info("Svarte på et behov av type: " + opplysningstype.name)
            } else {
                logger.info("Så et behov jeg ikke kunne svare på")
            }
        }
}

interface Pdl {
    fun hentPerson(foedselsnummer: String, rolle: PersonRolle): Person
    fun hentOpplysningsperson(foedselsnummer: String, rolle: PersonRolle): PersonDTO
}