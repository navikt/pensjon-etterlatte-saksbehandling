package no.nav.etterlatte.vedtaksvurdering.rivers

import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.vedtak.VedtakAldersovergangStepEvents
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class AldersovergangRiver(
    rapidsConnection: RapidsConnection,
    private val vedtakService: VedtakService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, EventNames.ALDERSOVERGANG) {
            validate { it.requireKey(ALDERSOVERGANG_STEG_KEY) }
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
        val type = packet[ALDERSOVERGANG_TYPE_KEY].asText()
        val step = packet[ALDERSOVERGANG_STEG_KEY].asText()
        val hendelseId = packet[ALDERSOVERGANG_ID_KEY].asText()
        val sakId = packet.sakId

        withLogContext(
            correlationId = getCorrelationId(),
            mapOf(
                "hendelseId" to hendelseId,
                "sakId" to sakId.toString(),
                "type" to type,
            ),
        ) {
            if (step == VedtakAldersovergangStepEvents.IDENTIFISERT_SAK.name) {
                val behandlingsdato = packet.dato
                logger.info("Sjekker løpende ytelse for sak $sakId, behandlingsmåned=$behandlingsdato")

                val loependeYtelse = vedtakService.harLoependeYtelserFra(sakId, behandlingsdato)
                logger.info("Sak $sakId, behandlingsmåned=$behandlingsdato har løpende ytelse: ${loependeYtelse.erLoepende}")

                packet[ALDERSOVERGANG_STEG_KEY] = VedtakAldersovergangStepEvents.VURDERT_LOEPENDE_YTELSE.name
                packet[HENDELSE_DATA_KEY] = loependeYtelse.erLoepende
                context.publish(packet.toJson())
            }
        }
    }
}
