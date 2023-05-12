package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.EventNames.FATT_VEDTAK
import no.nav.etterlatte.rapidsandrivers.EventNames.OPPRETT_VEDTAK
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.VEDTAK
import no.nav.etterlatte.regulering.VedtakService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.sakId
import rapidsandrivers.withFeilhaandtering
import java.util.*

internal class MigreringHendelser(
    rapidsConnection: RapidsConnection,
    private val vedtak: VedtakService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(VEDTAK)
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val behandlingId = packet.behandlingId
            val sakId = packet.sakId
            logger.info("Oppretter vedtak for migrer behandling $behandlingId")

            withFeilhaandtering(packet, context, OPPRETT_VEDTAK) {
                val respons = vedtak.upsertVedtak(behandlingId)
                logger.info("Opprettet vedtak ${respons.vedtakId} for migrert behandling: $behandlingId")
            }
                .takeIf { it.isSuccess }
                ?.let {
                    fattVedtak(packet, context, behandlingId, sakId)
                        .takeIf { it.isSuccess }
                        ?.let { attester(packet, context, behandlingId, sakId) }
                }
        }

    private fun fattVedtak(
        packet: JsonMessage,
        context: MessageContext,
        behandlingId: UUID,
        sakId: Long
    ) = withFeilhaandtering(packet, context, FATT_VEDTAK) {
        val fattetVedtak = vedtak.fattVedtak(behandlingId)
        logger.info(
            "Fattet vedtak ${fattetVedtak.vedtakId} for sak: $sakId og behandling: $behandlingId"
        )
    }

    private fun attester(
        packet: JsonMessage,
        context: MessageContext,
        behandlingId: UUID,
        sakId: Long
    ) {
        withFeilhaandtering(packet, context, EventNames.ATTESTER) {
            val attestert = vedtak.attesterVedtak(behandlingId)
            logger.info(
                "Attesterte vedtak ${attestert.vedtakId} for sak: $sakId og behandling: $behandlingId"
            )
        }
    }
}