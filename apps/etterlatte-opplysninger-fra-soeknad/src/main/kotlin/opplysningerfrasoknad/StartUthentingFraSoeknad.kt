package no.nav.etterlatte.opplysningerfrasoknad

import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import no.nav.etterlatte.opplysningerfrasoknad.opplysningsuthenter.Opplysningsuthenter
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
            eventName("soeknad_innsendt")
            correlationId()
            validate { it.requireKey(GyldigSoeknadVurdert.skjemaInfoKey) }
            validate { it.requireKey(GyldigSoeknadVurdert.sakIdKey) }
            validate { it.requireKey(GyldigSoeknadVurdert.behandlingIdKey) }
            validate { it.requireKey(GyldigSoeknadVurdert.gyldigInnsenderKey) }
            validate { it.requireKey(GyldigSoeknadVurdert.skjemaInfoTypeKey) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val opplysninger = opplysningsuthenter.lagOpplysningsListe(
                packet[GyldigSoeknadVurdert.skjemaInfoKey],
                SoeknadType.valueOf(packet[GyldigSoeknadVurdert.skjemaInfoTypeKey].textValue())
            )

            JsonMessage.newMessage(
                "OPPLYSNING:NY",
                mapOf(
                    "sakId" to packet[GyldigSoeknadVurdert.sakIdKey],
                    "behandlingId" to packet[GyldigSoeknadVurdert.behandlingIdKey],
                    "gyldigInnsender" to packet[GyldigSoeknadVurdert.gyldigInnsenderKey],
                    CORRELATION_ID_KEY to packet[CORRELATION_ID_KEY],
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