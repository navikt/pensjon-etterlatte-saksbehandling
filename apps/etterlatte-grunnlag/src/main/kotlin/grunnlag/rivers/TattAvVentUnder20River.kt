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
import java.time.YearMonth

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
        val alderVedMaanedsslutt =
            aldersovergangService.hentAlder(packet.sakId, PersonRolle.BARN, YearMonth.now().atEndOfMonth()) ?: return

        if (alderVedMaanedsslutt >= 20) {
            logger.debug(
                "Søker i sak ${packet.sakId} har fylt 20 år innen utgangen av denne måneden, " +
                    "og vil da være $alderVedMaanedsslutt år. " +
                    "Oppgava er alt tatt av vent, men fatting av vedtak og oppfølging skjer manuelt av saksbehandler.",
            )
            packet.setEventNameForHendelseType(Ventehendelser.TATT_AV_VENT_FYLT_20)
            context.publish(packet.toJson())
            return
        }
        if (alderVedMaanedsslutt < 18) {
            logger.error(
                "Forventer at søker er mellom 18 og 20, " +
                    "men søker i sak ${packet.sakId} er $alderVedMaanedsslutt år innen utgangen av denne måneden. " +
                    "Avbryter, dette må følges opp av en utvikler. NB: Oppgaven er alt tatt av vent.",
            )
            return
        }

        packet.setEventNameForHendelseType(Ventehendelser.TATT_AV_VENT_UNDER_20_SJEKKA)
        context.publish(packet.toJson())
    }
}
