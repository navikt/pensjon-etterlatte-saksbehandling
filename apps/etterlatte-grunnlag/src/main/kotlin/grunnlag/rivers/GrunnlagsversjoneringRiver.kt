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
        try {
            val alleSakIderFraGrunnlag = grunnlagService.hentAlleSakIder()

            logger.info("Fant ${alleSakIderFraGrunnlag.size} unike sak ider i grunnlag")

            alleSakIderFraGrunnlag
                .flatMap { sakId ->
                    runBlocking {
                        behandlingKlient.hentBehandlinger(sakId).behandlinger
                            .also { logger.info("Fant ${it.size} behandlinger for sak=$sakId") }
                    }
                }
                .forEach {
                    logger.info("Forsøker å koble behandling mot hendelsenummer")
                    val skalLaaseVersjon = it.status !in BehandlingStatus.underBehandling()
                    grunnlagService.oppdaterVersjonForBehandling(it.sakId, it.behandlingId, skalLaaseVersjon)
                }
        } catch (e: Exception) {
            logger.error("Feil ved oppdatering av versjon for behandling: ", e)
        }
    }
}
