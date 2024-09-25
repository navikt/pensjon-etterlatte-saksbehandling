package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.OmregningHendelseType
import no.nav.etterlatte.rapidsandrivers.OmregningshendelsePacket
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.omregninshendelse
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class VilkaarsvurderingRiver(
    rapidsConnection: RapidsConnection,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(VilkaarsvurderingRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, OmregningHendelseType.BEHANDLING_OPPRETTA) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireKey(OmregningshendelsePacket.BEHANDLING_ID) }
            validate { it.requireKey(OmregningshendelsePacket.FORRIGE_BEHANDLING_ID) }
        }
    }

    override fun kontekst() = Kontekst.OMREGNING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val omregningshendelse = packet.omregninshendelse
        val behandlingId = omregningshendelse.hentBehandlingId()
        val behandlingViOmregnerFra = omregningshendelse.hentForrigeBehandlingid()

        vilkaarsvurderingService.kopierForrigeVilkaarsvurdering(behandlingId, behandlingViOmregnerFra)
        packet.setEventNameForHendelseType(OmregningHendelseType.VILKAARSVURDERT)
        context.publish(packet.toJson())
        logger.info(
            "Vilkaarsvurdert ferdig for behandling $behandlingId og melding beregningsmelding ble sendt.",
        )
    }
}
