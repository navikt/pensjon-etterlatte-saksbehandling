package no.nav.etterlatte.opplysningerfrasoknad

import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationIdKey
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class StartUthentingFraSoeknad(
    rapidsConnection: RapidsConnection,
    private val opplysningsuthenter: Opplysningsuthenter
) : River.PacketListener {
    private val logger: Logger = LoggerFactory.getLogger(StartUthentingFraSoeknad::class.java)
    private val rapid = rapidsConnection

    init {
        River(rapidsConnection).apply {
            eventName(GyldigSoeknadVurdert.eventName)
            correlationId()
            validate { it.requireKey(GyldigSoeknadVurdert.skjemaInfoKey) }
            validate { it.requireKey(GyldigSoeknadVurdert.sakIdKey) }
            validate { it.requireKey(GyldigSoeknadVurdert.behandlingIdKey) }
            validate { it.requireKey(GyldigSoeknadVurdert.gyldigInnsenderKey) }
            // TODO legg til skjemainfo type
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val opplysninger = opplysningsuthenter.lagOpplysningsListe(
                packet[GyldigSoeknadVurdert.skjemaInfoKey],
                SoeknadType.BARNEPENSJON // TODO bruk type fra packet
            )

            JsonMessage.newMessage(
                "OPPLYSNING:NY",
                mapOf(
                    "sakId" to packet[GyldigSoeknadVurdert.sakIdKey],
                    "behandlingId" to packet[GyldigSoeknadVurdert.behandlingIdKey],
                    "gyldigInnsender" to packet[GyldigSoeknadVurdert.gyldigInnsenderKey],
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