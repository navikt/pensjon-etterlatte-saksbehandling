package no.nav.etterlatte.regulering

import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.rapidsandrivers.OmregningEvents
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.OPPRETT_VEDTAK
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.vedtaksvurdering.RapidUtsender
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.DATO_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLogging
import rapidsandrivers.sakId
import rapidsandrivers.withFeilhaandtering

internal class OpprettVedtakforespoerselRiver(
    rapidsConnection: RapidsConnection,
    private val vedtak: VedtakService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(OpprettVedtakforespoerselRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, OPPRETT_VEDTAK) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(DATO_KEY) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            // TODO EY-3232 - Fjerne
            validate { it.interestedIn(OmregningEvents.OMREGNING_NYE_REGLER) }
            validate { it.interestedIn(OmregningEvents.OMREGNING_NYE_REGLER_KJORING) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val sakId = packet.sakId
        logger.info("Leser opprett-vedtak forespoersel for sak $sakId")
        val behandlingId = packet.behandlingId

        withFeilhaandtering(packet, context, OPPRETT_VEDTAK) {
            // TODO EY-3232 - Fjerne
            val kjoringVariant = packet[OmregningEvents.OMREGNING_NYE_REGLER_KJORING].asText()
            val respons =
                if (kjoringVariant == MigreringKjoringVariant.MED_PAUSE.name) {
                    vedtak.opprettVedtakFattOgAttester(packet.sakId, behandlingId, MigreringKjoringVariant.MED_PAUSE)
                } else {
                    vedtak.opprettVedtakFattOgAttester(packet.sakId, behandlingId)
                }

            // TODO EY-3232 - Fjerne
            packet[OmregningEvents.OMREGNING_BRUTTO] = respons.vedtak.utbetalingsperioder.last().beloep!!.toInt()

            logger.info("Opprettet vedtak ${respons.vedtak.vedtakId} for sak: $sakId og behandling: $behandlingId")
            RapidUtsender.sendUt(respons, packet, context)
        }
    }
}
