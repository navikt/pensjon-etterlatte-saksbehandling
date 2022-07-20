package no.nav.etterlatte.opplysningerfrasoknad

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class StartUthentingFraSoeknad(
    rapidsConnection: RapidsConnection,
    private val opplysningsuthenter: Opplysningsuthenter,
) : River.PacketListener {
    private val logger: Logger = LoggerFactory.getLogger(StartUthentingFraSoeknad::class.java)
    private val rapid = rapidsConnection

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "FORDELER:FORDELT") }
            validate { it.requireValue("@soeknad_fordelt", true) }
            validate { it.requireKey("@skjema_info") }
            validate { it.requireKey("@sak_id") }
            validate { it.requireKey("@behandling_id") }
            validate { it.requireKey("@gyldig_innsender") }
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            val opplysninger = opplysningsuthenter.lagOpplysningsListe(packet["@skjema_info"])

            JsonMessage.newMessage(
                mapOf(
                    "sak" to packet["@sak_id"],
                    "@behandling_id" to packet["@behandling_id"],
                    "@gyldig_innsender" to packet["@gyldig_innsender"],
                    "@correlation_id" to packet["@correlation_id"],
                    "opplysning" to opplysninger
                )
            ).apply {
                try {
                    rapid.publish("OpplysningerFraSoeknad", toJson())
                } catch (err: Exception) {
                    logger.error("Kunne ikke publisere opplysninger fra soeknad", err)
                }
            }
            logger.info("Opplysninger hentet fra søknad")
        }
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()
