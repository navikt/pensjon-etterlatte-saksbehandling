package no.nav.etterlatte.trygdetid.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.omregning.OmregningDataPacket
import no.nav.etterlatte.omregning.OmregningHendelseType
import no.nav.etterlatte.omregning.omregningData
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import org.slf4j.LoggerFactory

internal class KopierTrygdetidRiver(
    rapidsConnection: RapidsConnection,
    private val trygdetidService: TrygdetidService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, OmregningHendelseType.VILKAARSVURDERT) {
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireKey(OmregningDataPacket.BEHANDLING_ID) }
            validate { it.requireKey(OmregningDataPacket.FORRIGE_BEHANDLING_ID) }
        }
    }

    override fun kontekst() = Kontekst.REGULERING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Mottatt omregninghendelse, kopierer trygdetid")
        val omregningData = packet.omregningData
        val behandlingId = omregningData.hentBehandlingId()
        val behandlingViOmregnerFra = omregningData.hentForrigeBehandlingid()
        trygdetidService.kopierTrygdetidFraForrigeBehandling(behandlingId, behandlingViOmregnerFra)
        packet.setEventNameForHendelseType(OmregningHendelseType.TRYGDETID_KOPIERT)
        context.publish(packet.toJson())
        logger.info("Trygdetid kopiert. Publiserte oppdatert omregningshendelse")
    }
}
