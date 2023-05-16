package no.nav.etterlatte.opplysninger.kilde.pdl

import MigreringGrunnlagRequest
import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_GRUNNLAG_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.PERSONGALLERI
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.PERSONGALLERI_GRUNNLAG
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

class MigreringHendelser(
    rapidsConnection: RapidsConnection,
    private val pdl: Pdl
) : River.PacketListener {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            correlationId()
            eventName(PERSONGALLERI)
            validate { it.requireKey(OPPLYSNING_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireKey("sakType") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val persongalleri = packet.hendelseData.persongalleri
        val sakType = objectMapper.treeToValue(packet["sakType"], SakType::class.java)
        logger.info("Behandler migrerings-persongalleri mot PDL")

        val soeker = lagEnkelopplysningerFraPDL(
            person = pdl.hentPerson(persongalleri.soeker, PersonRolle.BARN, sakType),
            personDTO = pdl.hentOpplysningsperson(persongalleri.soeker, PersonRolle.BARN, sakType),
            opplysningsbehov = Opplysningstype.SOEKER_PDL_V1,
            fnr = Folkeregisteridentifikator.of(persongalleri.soeker)
        ) as List<Grunnlagsopplysning<JsonNode>>

        val gjenlevende = persongalleri.gjenlevende.map {
            it to lagEnkelopplysningerFraPDL(
                person = pdl.hentPerson(it, PersonRolle.GJENLEVENDE, sakType),
                personDTO = pdl.hentOpplysningsperson(it, PersonRolle.GJENLEVENDE, sakType),
                opplysningsbehov = Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                fnr = Folkeregisteridentifikator.of(it)
            ) as List<Grunnlagsopplysning<JsonNode>>
        }

        val avdoede = persongalleri.avdoed.map {
            it to lagEnkelopplysningerFraPDL(
                person = pdl.hentPerson(it, PersonRolle.AVDOED, sakType),
                personDTO = pdl.hentOpplysningsperson(it, PersonRolle.AVDOED, sakType),
                opplysningsbehov = Opplysningstype.AVDOED_PDL_V1,
                fnr = Folkeregisteridentifikator.of(it)
            ) as List<Grunnlagsopplysning<JsonNode>>
        }

        packet[MIGRERING_GRUNNLAG_KEY] = MigreringGrunnlagRequest(
            soeker = Pair(persongalleri.soeker, soeker),
            gjenlevende = gjenlevende,
            avdoede = avdoede
        )
        packet.eventName = PERSONGALLERI_GRUNNLAG
        context.publish(packet.toJson())

        logger.info("Ferdig med å behandle migrering-persongalleri mot PDL")
    }
}