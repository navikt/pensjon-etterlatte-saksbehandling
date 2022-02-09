package no.nav.etterlatte.opplysninger.kilde.pdl

import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.person.Person
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.*

internal class LeggTilOpplysnignerFraPdl(
    rapidsConnection: RapidsConnection,
    private val behandlinger: Behandling,
    private val pdl: Pdl,
    private val opplysningsBygger: OpplysningsBygger
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "ey_fordelt") }
            validate { it.requireKey("@skjema_info") }
            validate { it.requireKey("@lagret_soeknad_id") }
            validate { it.requireKey("@fnr_soeker") }
            validate { it.requireValue("@soeknad_fordelt", true) }
            validate { it.requireKey("@sak_id") }
            validate { it.requireKey("@behandling_id") }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        behandlinger.leggTilOpplysninger(UUID.fromString(packet["@behandling_id"].asText()), opplysningsBygger.byggOpplysninger(pdl.hentPdlModell()))
    }
}

interface Behandling {
    fun leggTilOpplysninger(behandling: UUID, opplysninger: List<Behandlingsopplysning<out Any>>)
}

interface Pdl {
    fun hentPdlModell(): Person
}

interface OpplysningsBygger {
    fun byggOpplysninger(pdldata: Person):List<Behandlingsopplysning<out Any>>
}