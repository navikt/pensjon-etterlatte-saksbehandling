package no.nav.etterlatte.regulering

import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames.FINN_LOEPENDE_YTELSER
import no.nav.etterlatte.rapidsandrivers.EventNames.OMREGNINGSHENDELSE
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.DATO_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.TILBAKESTILTE_BEHANDLINGER_KEY
import rapidsandrivers.dato
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import rapidsandrivers.sakId
import rapidsandrivers.tilbakestilteBehandlinger

internal class LoependeYtelserforespoersel(
    rapidsConnection: RapidsConnection,
    private val vedtak: VedtakService
) : ListenerMedLoggingOgFeilhaandtering(rapidsConnection, FINN_LOEPENDE_YTELSER) {
    private val logger = LoggerFactory.getLogger(LoependeYtelserforespoersel::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(hendelsestype)
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(DATO_KEY) }
            validate { it.requireKey(TILBAKESTILTE_BEHANDLINGER_KEY) }
            correlationId()
        }.register(this)
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        val sakId = packet.sakId
        logger.info("Leser reguleringsfoerespoersel for sak $sakId")

        val reguleringsdato = packet.dato

        val tilbakestilteBehandlinger = packet.tilbakestilteBehandlinger
        tilbakestilteBehandlinger.forEach {
            try {
                vedtak.tilbakestillVedtak(it)
                logger.info("Tilbakestilt vedtak for behandling $it")
            } catch (e: Exception) {
                logger.error("Tilbakestilling av vedtak feilet for behandling $it", e)
            }
        }

        val respons = vedtak.harLoependeYtelserFra(sakId, reguleringsdato)
        respons.takeIf { it.erLoepende }?.let {
            packet.eventName = OMREGNINGSHENDELSE
            packet[HENDELSE_DATA_KEY] = Omregningshendelse(
                sakId = sakId,
                fradato = it.dato,
                prosesstype = Prosesstype.AUTOMATISK
            )
            context.publish(packet.toJson())
            logger.info("Grunnlopesreguleringmelding ble sendt for sak $sakId. Dato=${respons.dato}")
        } ?: logger.info("Grunnlopesreguleringmelding ble ikke sendt for sak $sakId. Dato=${respons.dato}")
    }
}