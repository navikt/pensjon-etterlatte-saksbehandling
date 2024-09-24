package no.nav.etterlatte.regulering

import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.Omregningshendelse
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.TILBAKESTILTE_BEHANDLINGER_KEY
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.etterlatte.rapidsandrivers.tilbakestilteBehandlinger
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class LoependeYtelserforespoerselRiver(
    rapidsConnection: RapidsConnection,
    private val vedtak: VedtakService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, ReguleringHendelseType.SAK_FUNNET) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(DATO_KEY) }
            validate { it.requireKey(TILBAKESTILTE_BEHANDLINGER_KEY) }
        }
    }

    override fun kontekst() = Kontekst.REGULERING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
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
        if (respons.underSamordning) {
            throw SakErUnderSamordning()
        }

        // TODO denne er egentlig ikke nødvendig å legge til før neste River men gjøres her for å slippe
        // å legge til behandling-model i beregning-kafka kun for denne klassen.
        packet[HENDELSE_DATA_KEY] =
            Omregningshendelse(
                sakId = sakId,
                fradato = respons.dato,
                revurderingaarsak = Revurderingaarsak.REGULERING,
            )
        respons.sisteLoependeBehandlingId?.let { b -> packet[BEHANDLING_VI_OMREGNER_FRA_KEY] = b }
        if (respons.erLoepende) {
            packet.setEventNameForHendelseType(ReguleringHendelseType.LOEPENDE_YTELSE_FUNNET)
            context.publish(packet.toJson())
            logger.info("Grunnbeløpsreguleringmelding ble sendt for sak $sakId. Dato=${respons.dato}")
        } else {
            packet.setEventNameForHendelseType(ReguleringHendelseType.YTELSE_IKKE_LOEPENDE)
            context.publish(packet.toJson())
            logger.info("Grunnbeløpsreguleringmelding ble ikke sendt for sak $sakId. Dato=${respons.dato}")
        }
    }
}

class SakErUnderSamordning : Exception("Sak er under samordning og kan ikke reguleres")
