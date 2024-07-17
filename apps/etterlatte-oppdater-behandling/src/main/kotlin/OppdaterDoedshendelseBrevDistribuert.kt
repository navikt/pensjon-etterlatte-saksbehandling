package no.nav.etterlatte

import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.libs.common.behandling.DoedshendelseBrevDistribuert
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.rapidsandrivers.BREV_ID_KEY
import no.nav.etterlatte.rapidsandrivers.BREV_KODE
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.brevId
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class OppdaterDoedshendelseException(
    override val detail: String,
    override val cause: Throwable?,
) : InternfeilException(detail, cause)

internal class OppdaterDoedshendelseBrevDistribuert(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevHendelseType.DISTRIBUERT) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BREV_ID_KEY) }
            validate { it.requireKey(BREV_KODE) }
        }
    }

    override fun kontekst() = Kontekst.DOEDSHENDELSE

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val brevkode = packet[BREV_KODE].asText()
        if (brevkode == Brevkoder.BP_INFORMASJON_DOEDSFALL.name || brevkode == Brevkoder.OMS_INFORMASJON_DOEDSFALL.name) {
            logger.info("Oppdaterer brev distribuert for dødshendelse ${packet.sakId}, ${packet.brevId}")
            try {
                behandlingService.oppdaterDoedshendelseBrevDistribuert(DoedshendelseBrevDistribuert(packet.sakId, packet.brevId))
            } catch (e: Exception) {
                logger.error("Kunne ikke oppdatere distribuert brev for sak ${packet.sakId} brevid: ${packet.brevId}")
                throw OppdaterDoedshendelseException("Kan ikke oppdatere doedshendelse ${packet.sakId}", e)
            }
        }
    }
}
