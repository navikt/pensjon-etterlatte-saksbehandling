package no.nav.etterlatte.regulering

import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.OPPRETT_VEDTAK
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.vedtaksvurdering.RapidUtsender
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.DATO_KEY
import rapidsandrivers.OMREGNING_BRUTTO
import rapidsandrivers.OMREGNING_NYE_REGLER
import rapidsandrivers.OMREGNING_NYE_REGLER_PAUSE
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
            validate { it.interestedIn(OMREGNING_NYE_REGLER) }
            validate { it.interestedIn(OMREGNING_NYE_REGLER_PAUSE) }
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
            val pause = packet[OMREGNING_NYE_REGLER_PAUSE].asBoolean()
            val respons =
                if (pause) {
                    vedtak.opprettVedtakFattOgAttester(packet.sakId, behandlingId, MigreringKjoringVariant.MED_PAUSE)
                } else {
                    vedtak.opprettVedtakFattOgAttester(packet.sakId, behandlingId)
                }
            if (packet[OMREGNING_NYE_REGLER].asBoolean()) {
                packet[OMREGNING_BRUTTO] = respons.vedtak.utbetalingsperioder.last().beloep!!.toInt() // TODO ??
            }
            logger.info("Opprettet vedtak ${respons.vedtak.vedtakId} for sak: $sakId og behandling: $behandlingId")
            RapidUtsender.sendUt(respons, packet, context)
        }
    }
}
