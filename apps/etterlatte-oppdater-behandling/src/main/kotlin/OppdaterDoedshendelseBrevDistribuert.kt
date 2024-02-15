package no.nav.etterlatte

import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.libs.common.behandling.DoedshendelseBrevDistribuert
import no.nav.etterlatte.rapidsandrivers.BREV_ID_KEY
import no.nav.etterlatte.rapidsandrivers.BREV_KODE
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.brevId
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

const val BREVKODE_BP_INFORMASJON_DOEDSFALL = "BP_INFORMASJON_DOEDSFALL"

internal class OppdaterDoedshendelseBrevDistribuert(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevHendelseType.DISTRIBUERT) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BREV_ID_KEY) }
            validate { it.requireKey(BREV_KODE) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val brevkode = packet[BREV_KODE].asText()
        if (brevkode == BREVKODE_BP_INFORMASJON_DOEDSFALL) {
            logger.info("Oppdaterer brev distribuert for dødshendelse ${packet.sakId}, ${packet.brevId}")
            behandlingService.oppdaterDoedshendelseBrevDistribuert(DoedshendelseBrevDistribuert(packet.sakId, packet.brevId))
        }
    }
}
