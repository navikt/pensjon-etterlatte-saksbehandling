package no.nav.etterlatte.vedtaksvurdering.rivers

import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.vedtak.VedtakAldersovergangStepEvents
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_STEP_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames
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
        initialiserRiver(rapidsConnection, EventNames.ALDERSOVERANG) {
            validate { it.requireValue(ALDERSOVERGANG_STEP_KEY, VedtakAldersovergangStepEvents.SJEKK_LOEPENDE_YTELSE.name) }
            validate { it.requireKey(ALDERSOVERGANG_TYPE_KEY) }
            validate { it.requireKey(ALDERSOVERGANG_ID_KEY) }
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

        packet["ao_step"] = VedtakAldersovergangStepEvents.LOEPENDE_YTELSE_RESULTAT.name
        packet["loependeYtelse"] = loependeYtelse.erLoepende
        context.publish(packet.toJson())
    }
}
