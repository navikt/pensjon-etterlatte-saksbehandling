package no.nav.etterlatte.opplysningerfrasoknad

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationIdKey
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
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
            eventName("GYLDIG_SOEKNAD:VURDERT")
            correlationId()
            validate { it.requireKey("@skjema_info") }
            validate { it.requireKey("sakId") }
            validate { it.requireKey("behandlingId") }
            validate { it.requireKey("gyldigInnsender") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val opplysninger = opplysningsuthenter.lagOpplysningsListe(packet["@skjema_info"])

            JsonMessage.newMessage("OPPLYSNING:NY",
                mapOf(
                    "sakId" to packet["sakId"],
                    "behandlingId" to packet["behandlingId"],
                    "gyldigInnsender" to packet["gyldigInnsender"],
                    correlationIdKey to packet[correlationIdKey],
                    "opplysning" to opplysninger
                )
            ).apply {
                try {
                    rapid.publish(packet["behandlingId"].toString(), toJson())
                } catch (err: Exception) {
                    logger.error("Kunne ikke publisere opplysninger fra soeknad", err)
                }
            }
            logger.info("Opplysninger hentet fra søknad")
        }
}
