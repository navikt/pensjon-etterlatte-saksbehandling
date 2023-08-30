package migrering

import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.migrering.PesysRepository
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PESYS_ID
import no.nav.etterlatte.rapidsandrivers.migrering.pesysId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering

internal class LagreKopling(rapidsConnection: RapidsConnection, private val pesysRepository: PesysRepository) :
    ListenerMedLoggingOgFeilhaandtering(Migreringshendelser.LAGRE_KOPLING) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(hendelsestype)
            correlationId()
            validate { it.requireKey(PESYS_ID) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
        }.register(this)
    }

    override fun haandterPakke(packet: JsonMessage, context: MessageContext) {
        pesysRepository.lagreKoplingTilBehandling(packet.behandlingId, packet.pesysId)
        packet.eventName = Migreringshendelser.LAGRE_GRUNNLAG
        context.publish(packet.toJson())
        logger.info(
            "Publiserte oppdatert migreringshendelse for ${packet.behandlingId} " +
                "med lagra kopling til pesyssak ${packet.pesysId}"
        )
    }
}