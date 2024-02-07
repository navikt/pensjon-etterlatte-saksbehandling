package no.nav.etterlatte.migrering

import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

internal class LyttPaaDistribuerBrevRiver(
    rapidsConnection: RapidsConnection,
    private val pesysRepository: PesysRepository,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevHendelseType.DISTRIBUERT) {
            validate { it.requireKey("bestillingsId") }
            validate { it.requireKey("vedtak.behandlingId") }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = UUID.fromString(packet["vedtak.behandlingId"].asText())
        val migreringsBehandling = pesysRepository.hentPesysId(behandlingId)

        if (migreringsBehandling == null) {
            logger.info("Sak med behandlingId={} er ikke en migrert sak. Gjør ingenting.", behandlingId)
            return
        }

        pesysRepository.oppdaterStatus(migreringsBehandling.pesysId, Migreringsstatus.BREVUTSENDING_OK)
    }
}
