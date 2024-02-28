package no.nav.etterlatte.migrering

import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PESYS_ID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.pesysId
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.util.UUID

internal class LagreKoblingRiver(rapidsConnection: RapidsConnection, private val pesysRepository: PesysRepository) :
    ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.LAGRE_KOPLING) {
            validate { it.requireKey(PESYS_ID_KEY) }
            validate { it.interestedIn(BEHANDLING_ID_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Lagrer kopling fra pesyssak ${packet.pesysId} til behandling ${packet.behandlingId}")
        val behandlingId: UUID? =
            try {
                packet.behandlingId
            } catch (e: Exception) {
                null
            }
        pesysRepository.lagreKoplingTilBehandling(behandlingId, packet.pesysId, packet.sakId)
        if (behandlingId == null) {
            pesysRepository.oppdaterStatus(packet.pesysId, Migreringsstatus.OPPGAVE_OPPRETTA_FOR_MANUELT)
        }
        packet.setEventNameForHendelseType(Migreringshendelser.LAGRE_GRUNNLAG)
        context.publish(packet.toJson())
        logger.info(
            "Publiserte oppdatert migreringshendelse for $behandlingId " +
                "med lagra kopling til pesyssak ${packet.pesysId}",
        )
    }
}
