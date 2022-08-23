package no.nav.etterlatte.opplysninger.kilde.inntektskomponenten

import no.nav.etterlatte.Aareg
import no.nav.etterlatte.InntektsKomponenten
import no.nav.etterlatte.OpplysningsBygger
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.rapidsandrivers.behovNameKey
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class HentOpplysningerFraInntektskomponenten(
    rapidsConnection: RapidsConnection,
    private val inntektsKomponentenService: InntektsKomponenten,
    private val aaregService: Aareg,
    private val opplysningsBygger: OpplysningsBygger
) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(HentOpplysningerFraInntektskomponenten::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.requireKey(behovNameKey) }
            validate { it.requireKey("sakId") }
            validate { it.requireKey("fnr") }
            validate { it.requireKey("doedsdato") }
            validate { it.rejectKey("opplysning") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            if (packet["@behov"].asText() in listOf(Opplysningstyper.AVDOED_INNTEKT_V1.name)) {
                try {
                    val fnr = Foedselsnummer.of(packet["fnr"].asText())
                    val doedsdato = LocalDate.parse(packet["doedsdato"].asText())
                    val arbeidsforhold = aaregService.hentArbeidsforhold(fnr)
                    val inntektliste = inntektsKomponentenService.hentInntektListe(fnr, doedsdato)
                    val opplysninger = opplysningsBygger.byggOpplysninger(inntektliste, arbeidsforhold)
                    packet["opplysning"] = opplysninger
                    context.publish(packet.toJson())
                } catch (e: Exception) {
                    logger.info(e.message)
                }
            }
        }
}