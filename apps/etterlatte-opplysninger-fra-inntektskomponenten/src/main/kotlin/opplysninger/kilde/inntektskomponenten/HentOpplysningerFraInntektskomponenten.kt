package no.nav.etterlatte.opplysninger.kilde.inntektskomponenten

import no.nav.etterlatte.InntektsKomponenten
import no.nav.etterlatte.OpplysningsBygger
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class HentOpplysningerFraInntektskomponenten(
    rapidsConnection: RapidsConnection,
    private val inntektsKomponentenService: InntektsKomponenten,
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
            val fnr = hentAvdoedFnr(barnePensjon)
            opplysningsBygger.byggOpplysninger(barnePensjon, inntektsKomponentenService.hentInntektListe(fnr))
        }

    private fun hentAvdoedFnr(barnepensjon: Barnepensjon): Foedselsnummer {
        val fnr = barnepensjon.foreldre.find { it.type === PersonType.AVDOED }?.foedselsnummer?.svar?.value
        if (fnr != null) {
            return Foedselsnummer.of(fnr)
        }
        throw Exception("Mangler fødselsnummer")
    }

    private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()
}
