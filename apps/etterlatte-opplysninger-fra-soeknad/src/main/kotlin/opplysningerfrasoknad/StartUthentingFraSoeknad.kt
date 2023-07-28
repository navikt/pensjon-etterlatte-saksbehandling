package no.nav.etterlatte.opplysningerfrasoknad

import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.event.SoeknadInnsendt
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.opplysningerfrasoknad.opplysningsuthenter.Opplysningsuthenter
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLogging

internal class StartUthentingFraSoeknad(
    rapidsConnection: RapidsConnection,
    private val opplysningsuthenter: Opplysningsuthenter
) : ListenerMedLogging() {
    private val logger: Logger = LoggerFactory.getLogger(StartUthentingFraSoeknad::class.java)
    private val rapid = rapidsConnection

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAny(
                    EVENT_NAME_KEY,
                    listOf(SoeknadInnsendt.eventNameInnsendt, SoeknadInnsendt.eventNameBehandlingBehov)
                )
            }
            correlationId()
            validate { it.requireKey(GyldigSoeknadVurdert.skjemaInfoKey) }
            validate { it.requireKey(GyldigSoeknadVurdert.sakIdKey) }
            validate { it.requireKey(GyldigSoeknadVurdert.behandlingIdKey) }
            validate { it.requireKey(GyldigSoeknadVurdert.skjemaInfoTypeKey) }
        }.register(this)
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        val opplysninger = opplysningsuthenter.lagOpplysningsListe(
            packet[GyldigSoeknadVurdert.skjemaInfoKey],
            SoeknadType.valueOf(packet[GyldigSoeknadVurdert.skjemaInfoTypeKey].textValue())
        )

        JsonMessage.newMessage(
            "OPPLYSNING:NY",
            mapOf(
                "sakId" to packet[GyldigSoeknadVurdert.sakIdKey],
                "behandlingId" to packet[GyldigSoeknadVurdert.behandlingIdKey],
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