package no.nav.etterlatte.opplysninger.kilde.pdl

import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.*

internal class LeggTilOpplysningerFraPdl(
    rapidsConnection: RapidsConnection,
    private val behandlinger: Behandling,
    private val pdl: Pdl,
    private val opplysningsBygger: OpplysningsBygger
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "ey_fordelt") }
            validate { it.requireKey("@skjema_info") }
            validate { it.requireValue("@skjema_info.versjon", "2") }
            validate { it.requireKey("@lagret_soeknad_id") }
            validate { it.requireKey("@fnr_soeker") }
            validate { it.requireValue("@soeknad_fordelt", true) }
            validate { it.requireKey("@sak_id") }
            validate { it.requireKey("@behandling_id") }
            validate { it.interestedIn("@correlation_id") }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val barnePensjon = objectMapper.treeToValue(packet["@skjema_info"], Barnepensjon::class.java)!!
            behandlinger.leggTilOpplysninger(UUID.fromString(packet["@behandling_id"].asText()), opplysningsBygger.byggOpplysninger(barnePensjon, pdl))
        }
    }


interface Behandling {
    fun leggTilOpplysninger(behandling: UUID, opplysninger: List<Grunnlagsopplysning<out Any>>)
}

interface Pdl {
    fun hentPdlModell(foedselsnummer: String, rolle: PersonRolle): Person
}

interface OpplysningsBygger {
    fun byggOpplysninger(barnepensjon: Barnepensjon, pdl: Pdl):List<Grunnlagsopplysning<out Any>>
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()