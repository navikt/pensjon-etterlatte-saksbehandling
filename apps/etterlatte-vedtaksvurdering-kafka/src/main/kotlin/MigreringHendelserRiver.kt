package no.nav.etterlatte

import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.VEDTAK
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLogging
import rapidsandrivers.sakId
import rapidsandrivers.withFeilhaandtering

internal class MigreringHendelserRiver(
    rapidsConnection: RapidsConnection,
    private val vedtak: VedtakService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, VEDTAK) {
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet.behandlingId
        logger.info("Oppretter, fatter og attesterer vedtak for migrer behandling $behandlingId")

        withFeilhaandtering(packet, context, VEDTAK) {
            val respons = vedtak.opprettVedtakFattOgAttester(packet.sakId, behandlingId)
            logger.info("Opprettet vedtak ${respons.vedtakId} for migrert behandling: $behandlingId")
        }
    }
}
