package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_KJORING_VARIANT
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.migreringKjoringVariant
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.etterlatte.vedtaksvurdering.RapidUtsender
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class MigreringHendelserRiver(
    rapidsConnection: RapidsConnection,
    private val vedtakService: VedtakService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.BEREGNET_FERDIG) {
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(MIGRERING_KJORING_VARIANT) }
            validate { it.requireValue(KILDE_KEY, Vedtaksloesning.PESYS.name) }
        }
    }

    override fun kontekst() = Kontekst.MIGRERING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet.behandlingId
        logger.info("Oppretter, fatter og attesterer vedtak for migrer behandling $behandlingId")

        val kjoringVariant = packet.migreringKjoringVariant
        val respons = vedtakService.opprettVedtakFattOgAttester(packet.sakId, behandlingId, kjoringVariant)
        if (kjoringVariant == MigreringKjoringVariant.MED_PAUSE) {
            packet.setEventNameForHendelseType(Migreringshendelser.PAUSE)
            context.publish(packet.toJson())
        }
        logger.info("Opprettet vedtak ${respons.vedtak.id} for migrert behandling: $behandlingId")
        RapidUtsender.sendUt(respons, packet, context)
    }
}
