package no.nav.etterlatte

import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.migrering.BREV_OPPRETTA_MIGRERING
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_KJORING_VARIANT
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.PAUSE
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.VEDTAK
import no.nav.etterlatte.rapidsandrivers.migrering.migreringKjoringVariant
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
    private val vedtakService: VedtakService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, VEDTAK) {
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(MIGRERING_KJORING_VARIANT) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet.behandlingId
        logger.info("Oppretter, fatter og attesterer vedtak for migrer behandling $behandlingId")

        val kjoringVariant = packet.migreringKjoringVariant
        withFeilhaandtering(packet, context, VEDTAK) {
            val respons = vedtakService.opprettVedtakFattOgAttester(packet.sakId, behandlingId, kjoringVariant)
            if (kjoringVariant == MigreringKjoringVariant.MED_PAUSE) {
                packet.eventName = PAUSE
                context.publish(packet.toJson())
            }
            logger.info("Opprettet vedtak ${respons.vedtak.vedtakId} for migrert behandling: $behandlingId")
            with(respons.rapidInfo1) {
                packet.eventName = vedtakhendelse.toString()
                packet[TEKNISK_TID_KEY] = tekniskTid
                packet["vedtak"] = vedtak
                packet[BREV_OPPRETTA_MIGRERING] = false
                extraParams.forEach { (k, v) -> packet[k] = v }
                context.publish(behandlingId.toString(), packet.toJson())
            }
            respons.rapidInfo2?.let {
                with(it) {
                    packet.eventName = vedtakhendelse.toString()
                    packet[TEKNISK_TID_KEY] = tekniskTid
                    packet["vedtak"] = vedtak
                    packet[BREV_OPPRETTA_MIGRERING] = false
                    extraParams.forEach { (k, v) -> packet[k] = v }
                    context.publish(behandlingId.toString(), packet.toJson())
                }
            }
        }
    }
}
