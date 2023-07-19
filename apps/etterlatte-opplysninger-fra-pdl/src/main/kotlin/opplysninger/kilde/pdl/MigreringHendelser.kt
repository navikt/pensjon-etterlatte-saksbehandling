package no.nav.etterlatte.opplysninger.kilde.pdl

import MigreringGrunnlagRequest
import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_GRUNNLAG_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.HENT_PDL
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.PDLOPPSLAG_UTFOERT
import no.nav.etterlatte.rapidsandrivers.migrering.hendelseData
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.OPPLYSNING_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.withFeilhaandtering

class MigreringHendelser(
    rapidsConnection: RapidsConnection,
    private val pdlKlientInterface: PdlKlientInterface
) : River.PacketListener {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            correlationId()
            eventName(HENT_PDL)
            validate { it.requireKey(OPPLYSNING_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireKey("sakType") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, HENT_PDL) {
                val fnr = packet["fnr"].textValue()
                val personRolle = objectMapper.treeToValue(packet["rolle"], PersonRolle::class.java)!!
                val opplysningstype = objectMapper.treeToValue(packet[BEHOV_NAME_KEY], Opplysningstype::class.java)!!
                val saktype = objectMapper.treeToValue(packet["sakType"], SakType::class.java)
                val person = pdlKlientInterface.hentPerson(fnr, personRolle, saktype)
                val opplysningsperson = pdlKlientInterface.hentOpplysningsperson(fnr, personRolle, saktype)

                packet["opplysning"] = lagEnkelopplysningerFraPDL(
                    person = person,
                    personDTO = opplysningsperson,
                    opplysningstype = opplysningstype,
                    fnr = Folkeregisteridentifikator.of(fnr)
                )

                val persongalleri = packet.hendelseData.persongalleri
                val sakType = objectMapper.treeToValue(packet["sakType"], SakType::class.java)
                logger.info("Behandler migrerings-persongalleri mot PDL")

                val soeker = lagEnkelopplysningerFraPDL(
                    person = pdlKlientInterface.hentPerson(persongalleri.soeker, PersonRolle.BARN, sakType),
                    personDTO = pdlKlientInterface.hentOpplysningsperson(
                        persongalleri.soeker,
                        PersonRolle.BARN,
                        sakType
                    ),
                    opplysningstype = Opplysningstype.SOEKER_PDL_V1,
                    fnr = Folkeregisteridentifikator.of(persongalleri.soeker)
                ) as List<Grunnlagsopplysning<JsonNode>>

                val gjenlevende = persongalleri.gjenlevende.map {
                    it to lagEnkelopplysningerFraPDL(
                        person = pdlKlientInterface.hentPerson(it, PersonRolle.GJENLEVENDE, sakType),
                        personDTO = pdlKlientInterface.hentOpplysningsperson(it, PersonRolle.GJENLEVENDE, sakType),
                        opplysningstype = Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                        fnr = Folkeregisteridentifikator.of(it)
                    ) as List<Grunnlagsopplysning<JsonNode>>
                }

                val avdoede = persongalleri.avdoed.map {
                    it to lagEnkelopplysningerFraPDL(
                        person = pdlKlientInterface.hentPerson(it, PersonRolle.AVDOED, sakType),
                        personDTO = pdlKlientInterface.hentOpplysningsperson(it, PersonRolle.AVDOED, sakType),
                        opplysningstype = Opplysningstype.AVDOED_PDL_V1,
                        fnr = Folkeregisteridentifikator.of(it)
                    ) as List<Grunnlagsopplysning<JsonNode>>
                }

                packet[MIGRERING_GRUNNLAG_KEY] = MigreringGrunnlagRequest(
                    soeker = Pair(persongalleri.soeker, soeker),
                    gjenlevende = gjenlevende,
                    avdoede = avdoede
                )
                packet.eventName = PDLOPPSLAG_UTFOERT
                context.publish(packet.toJson())

                logger.info("Ferdig med å behandle migrering-persongalleri mot PDL")
            }
        }
    }
}