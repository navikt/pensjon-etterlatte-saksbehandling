package no.nav.etterlatte.vedtaksvurdering.rivers

import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakAldersovergangEvents
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class AldersovergangSjekkLoependeYtelseRiver(
    rapidsConnection: RapidsConnection,
    private val vedtakService: VedtakService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, VedtakAldersovergangEvents.SJEKK_LOEPENDE_YTELSE) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(DATO_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakId = packet.sakId
        val datoFoersteIAktuellBehandlingsmaaned = packet.dato
        logger.info("Sjekker løpende ytelse for sak $sakId, dato=$datoFoersteIAktuellBehandlingsmaaned")

        val loependeYtelse = vedtakService.harLoependeYtelserFra(sakId, datoFoersteIAktuellBehandlingsmaaned)
        logger.info("Sak $sakId, dato=$datoFoersteIAktuellBehandlingsmaaned har løpende ytelse: ${loependeYtelse.erLoepende}")

        packet.setEventNameForHendelseType(VedtakAldersovergangEvents.SJEKK_LOEPENDE_YTELSE_RESULTAT)
        packet["loependeYtelse"] = loependeYtelse.erLoepende
        context.publish(packet.toJson())
    }
}
