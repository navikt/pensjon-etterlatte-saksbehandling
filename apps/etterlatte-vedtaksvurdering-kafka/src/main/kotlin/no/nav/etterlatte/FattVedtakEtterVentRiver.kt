package no.nav.etterlatte.no.nav.etterlatte

import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.rapidsandrivers.AUTOMATISK_GJENOPPRETTING
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_KJORING_VARIANT
import no.nav.etterlatte.rapidsandrivers.migrering.OPPGAVEKILDE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Ventehendelser
import no.nav.etterlatte.rapidsandrivers.migrering.migreringKjoringVariant
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.etterlatte.vedtaksvurdering.RapidUtsender
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.Kontekst

class FattVedtakEtterVentRiver(
    rapidsConnection: RapidsConnection,
    private val vedtakService: VedtakService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Ventehendelser.TATT_AV_VENT_UNDER_20_SJEKKA_OG_BREVUTFALL_LAGT_INN) {
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireValue(OPPGAVEKILDE_KEY, OppgaveKilde.GJENOPPRETTING.name) }
            validate { it.requireKey(MIGRERING_KJORING_VARIANT) }
        }
    }

    override fun kontekst() = Kontekst.VENT

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet.behandlingId
        logger.info("Oppretter, fatter og attesterer vedtak for gjenopptatt behandling $behandlingId")
        val respons =
            vedtakService.opprettVedtakFattOgAttester(
                packet.sakId,
                behandlingId,
                packet.migreringKjoringVariant,
            )
        logger.info("Opprettet vedtak ${respons.vedtak.id} for migrert behandling: $behandlingId")

        packet[AUTOMATISK_GJENOPPRETTING] = true
        RapidUtsender.sendUt(respons, packet, context)
    }
}
