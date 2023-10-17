package no.nav.etterlatte.grunnlag.rivers

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLogging

// TODO: Fjerne når grunnlag er versjonert (EY-2567)
class GrunnlagsversjoneringRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingKlient: BehandlingKlient,
    private val grunnlagService: GrunnlagService,
) : ListenerMedLogging() {
    private val logger: Logger = LoggerFactory.getLogger(GrunnlagsversjoneringRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, EventNames.GRUNNLAGSVERSJONERING_EVENT_NAME) {}
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        grunnlagService.hentAlleSakIder()
            .flatMap { sakId -> runBlocking { behandlingKlient.hentBehandlinger(sakId).behandlinger } }
            .forEach {
                val skalLaaseVersjon = it.status in BehandlingStatus.iverksattEllerAttestert()
                grunnlagService.oppdaterVersjonForBehandling(it.sakId, it.behandlingId, skalLaaseVersjon)
            }
    }
}
