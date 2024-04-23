package no.nav.etterlatte.opplysningerfrasoknad

import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.event.SoeknadInnsendtHendelseType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.opplysningerfrasoknad.opplysningsuthenter.Opplysningsuthenter
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.Behandlingssteg
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.OPPLYSNING_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class StartUthentingFraSoeknadRiver(
    rapidsConnection: RapidsConnection,
    private val opplysningsuthenter: Opplysningsuthenter,
) : ListenerMedLogging() {
    private val logger: Logger = LoggerFactory.getLogger(StartUthentingFraSoeknadRiver::class.java)
    private val rapid = rapidsConnection

    init {
        initialiserRiverUtenEventName(rapidsConnection) {
            validate {
                it.demandAny(
                    EVENT_NAME_KEY,
                    SoeknadInnsendtHendelseType.entries.map { it.lagEventnameForType() },
                )
            }
            validate { it.requireKey(GyldigSoeknadVurdert.skjemaInfoKey) }
            validate { it.requireKey(GyldigSoeknadVurdert.sakIdKey) }
            validate { it.requireKey(GyldigSoeknadVurdert.behandlingIdKey) }
            validate { it.requireKey(GyldigSoeknadVurdert.skjemaInfoTypeKey) }
            validate { it.interestedIn(Behandlingssteg.KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val opplysninger =
            opplysningsuthenter.lagOpplysningsListe(
                packet[GyldigSoeknadVurdert.skjemaInfoKey],
                SoeknadType.valueOf(packet[GyldigSoeknadVurdert.skjemaInfoTypeKey].textValue()),
            )

        JsonMessage.newMessage(
            mapOf(
                EventNames.NY_OPPLYSNING.lagParMedEventNameKey(),
                SAK_ID_KEY to packet[GyldigSoeknadVurdert.sakIdKey],
                BEHANDLING_ID_KEY to packet[GyldigSoeknadVurdert.behandlingIdKey],
                CORRELATION_ID_KEY to packet[CORRELATION_ID_KEY],
                OPPLYSNING_KEY to opplysninger,
                Behandlingssteg.KEY to packet[Behandlingssteg.KEY],
            ),
        ).apply {
            try {
                rapid.publish(packet[BEHANDLING_ID_KEY].toString(), toJson())
            } catch (err: Exception) {
                logger.error("Kunne ikke publisere opplysninger fra soeknad", err)
            }
        }
        logger.info("Opplysninger hentet fra søknad")
    }
}
