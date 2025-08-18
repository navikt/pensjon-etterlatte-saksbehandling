package no.nav.etterlatte.opplysningerfrasoknad

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.event.SoeknadInnsendtHendelseType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.opplysningerfrasoknad.uthenter.Opplysningsuthenter
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.Behandlingssteg
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.OPPLYSNING_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class StartUthentingFraSoeknadRiver(
    rapidsConnection: RapidsConnection,
    private val opplysningsuthenter: Opplysningsuthenter,
) : ListenerMedLogging() {
    private val logger: Logger = LoggerFactory.getLogger(StartUthentingFraSoeknadRiver::class.java)
    private val rapid = rapidsConnection

    init {
        initialiserRiver(rapidsConnection, SoeknadInnsendtHendelseType.EVENT_NAME_BEHANDLINGBEHOV) {
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

        val verdier =
            mutableMapOf(
                EventNames.NY_OPPLYSNING.lagParMedEventNameKey(),
                SAK_ID_KEY to packet[GyldigSoeknadVurdert.sakIdKey],
                BEHANDLING_ID_KEY to packet[GyldigSoeknadVurdert.behandlingIdKey],
                CORRELATION_ID_KEY to packet[CORRELATION_ID_KEY],
                OPPLYSNING_KEY to opplysninger,
            )
        packet[Behandlingssteg.KEY].takeUnless { it.isMissingNode }?.also { verdier[Behandlingssteg.KEY] = it }
        JsonMessage.newMessage(verdier).apply {
            try {
                rapid.publish(packet[BEHANDLING_ID_KEY].toString(), toJson())
            } catch (err: Exception) {
                logger.error("Kunne ikke publisere opplysninger fra soeknad", err)
            }
        }
        logger.info("Opplysninger hentet fra søknad")
    }
}
