package no.nav.etterlatte.inntektsjustering

import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.rapidsandrivers.BREV_KODE
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.RapidEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.brevId
import no.nav.etterlatte.rapidsandrivers.kjoering
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class OppdaterInntektsjusteringException(
    override val detail: String,
    override val cause: Throwable?,
) : InternfeilException(detail, cause)

internal class OppdaterInntektsjusteringBrevDistribuert(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevHendelseType.DISTRIBUERT) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BREV_KODE) }
            validate { it.interestedIn(KJOERING) }
            validate { it.demandValue(BREV_KODE, Brevkoder.OMS_INNTEKTSJUSTERING.name) }
        }
    }

    override fun kontekst() = Kontekst.INNTEKTSJUSTERING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Oppdaterer brev distribuert for inntektsjustering ${packet.sakId}, ${packet.brevId}")
        try {
            behandlingService.lagreKjoering(packet.sakId, KjoeringStatus.FERDIGSTILT, packet.kjoering)
        } catch (e: Exception) {
            logger.error("Kunne ikke oppdatere distribuert brev for sak ${packet.sakId} brevid: ${packet.brevId}", e)
            throw OppdaterInntektsjusteringException("Kan ikke oppdatere brev distribuert for ${packet.sakId}", e)
        }
    }
}
