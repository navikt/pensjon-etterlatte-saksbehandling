package no.nav.etterlatte.grunnlag.rivers

import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangService
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Ventehendelser
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class TattAvVentUnder20River(
    rapidsConnection: RapidsConnection,
    private val aldersovergangService: AldersovergangService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Ventehendelser.TATT_AV_VENT) {
            validate { it.requireKey(SAK_ID_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val alder = aldersovergangService.hentAlder(packet.sakId, PersonRolle.BARN) ?: return

        if (alder >= 20) {
            logger.error("Søker i sak ${packet.sakId} har fylt 20 år. Tar ikke av vent. Skal ikkje skje, må følges opp manuelt")
            return
        }

        packet.setEventNameForHendelseType(Ventehendelser.TATT_AV_VENT_UNDER_20_SJEKKA)
        context.publish(packet.toJson())
    }
}
