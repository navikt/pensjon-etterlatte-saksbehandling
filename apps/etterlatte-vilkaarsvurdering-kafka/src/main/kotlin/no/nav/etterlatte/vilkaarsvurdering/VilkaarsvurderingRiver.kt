package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_VI_OMREGNER_FRA_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.OmregningHendelseType
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.toUUID
import org.slf4j.LoggerFactory

internal class VilkaarsvurderingRiver(
    rapidsConnection: RapidsConnection,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(VilkaarsvurderingRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, OmregningHendelseType.BEHANDLING_OPPRETTA) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(BEHANDLING_VI_OMREGNER_FRA_KEY) }
        }
    }

    override fun kontekst() = Kontekst.REGULERING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet.behandlingId
        val behandlingViOmregnerFra = packet[BEHANDLING_VI_OMREGNER_FRA_KEY].asText().toUUID()

        vilkaarsvurderingService.kopierForrigeVilkaarsvurdering(behandlingId, behandlingViOmregnerFra)
        packet.setEventNameForHendelseType(OmregningHendelseType.VILKAARSVURDERT)
        context.publish(packet.toJson())
        logger.info(
            "Vilkaarsvurdert ferdig for behandling $behandlingId og melding beregningsmelding ble sendt.",
        )
    }
}
