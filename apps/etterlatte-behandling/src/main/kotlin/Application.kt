package no.nav.etterlatte

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.principal
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.Route
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.nav.etterlatte.behandling.behandlingRoutes
import no.nav.etterlatte.behandling.behandlingVedtakRoute
import no.nav.etterlatte.behandling.behandlinginfo.behandlingInfoRoutes
import no.nav.etterlatte.behandling.behandlingsstatusRoutes
import no.nav.etterlatte.behandling.bosattutland.bosattUtlandRoutes
import no.nav.etterlatte.behandling.generellbehandling.generellbehandlingRoutes
import no.nav.etterlatte.behandling.klage.klageRoutes
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.behandling.omregning.migreringRoutes
import no.nav.etterlatte.behandling.omregning.omregningRoutes
import no.nav.etterlatte.behandling.revurdering.revurderingRoutes
import no.nav.etterlatte.behandling.sjekklisteRoute
import no.nav.etterlatte.behandling.statistikk.statistikkRoutes
import no.nav.etterlatte.behandling.tilbakekreving.tilbakekrevingRoutes
import no.nav.etterlatte.behandling.tilgang.tilgangRoutes
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.egenansatt.EgenAnsattService
import no.nav.etterlatte.egenansatt.egenAnsattRoute
import no.nav.etterlatte.grunnlagsendring.grunnlagsendringshendelseRoute
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdService
import no.nav.etterlatte.institusjonsopphold.institusjonsoppholdRoute
import no.nav.etterlatte.jobs.addShutdownHook
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.oppgave.oppgaveRoutes
import no.nav.etterlatte.sak.sakSystemRoutes
import no.nav.etterlatte.sak.sakWebRoutes
import no.nav.etterlatte.tilgangsstyring.adressebeskyttelsePlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

val sikkerLogg: Logger = sikkerlogger()

fun main() {
    Server(ApplicationContext()).run()
}

private class Server(private val context: ApplicationContext) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-behandling")
    }

    private val engine =
        embeddedServer(
            factory = CIO,
            environment =
                applicationEngineEnvironment {
                    config = HoconApplicationConfig(context.config)
                    module { module(context) }
                    connector { port = context.httpPort }
                },
        )

    fun run() =
        with(context) {
            dataSource.migrate()
            lagSaksbendlereMedNavn(context)
            setReady().also { engine.start(true) }
        }
}

@OptIn(DelicateCoroutinesApi::class)
private fun lagSaksbendlereMedNavn(context: ApplicationContext) {
    if (context.leaderElectionKlient.isLeader()) {
        GlobalScope.launch(newSingleThreadContext("saksbehandlernavnjob")) {
            val logger = LoggerFactory.getLogger("saksbehandlernavnjob")
            logger.info("Starter job for å legge inn saksbehandlere med navn")
            val sbidenter = context.saksbehandlerInfoDao.hentalleSaksbehandlere()
            logger.info("Antall identer ${sbidenter.size}")
            val mappedMedNavn =
                sbidenter.map {
                    it to runBlocking { context.navAnsattKlient.hentSaksbehanderNavn(it) }
                }
            mappedMedNavn.forEach {
                SaksbehandlerInfo(it.first, it.first)
                if (it.second == null) {
                    context.saksbehandlerInfoDao.upsertSaksbehandler(SaksbehandlerInfo(it.first, it.first))
                } else {
                    context.saksbehandlerInfoDao.upsertSaksbehandler(it.second!!)
                }
            }
            logger.info("Ferdig")
        }
    }
}

internal fun Application.module(context: ApplicationContext) {
    with(context) {
        restModule(
            sikkerLogg,
            withMetrics = true,
        ) {
            attachContekst(dataSource, context)
            sakSystemRoutes(
                tilgangService = tilgangService,
                sakService = sakService,
                behandlingService = behandlingService,
                requestLogger = behandlingRequestLogger,
            )
            sakWebRoutes(
                tilgangService = tilgangService,
                sakService = sakService,
                behandlingService = behandlingService,
                grunnlagsendringshendelseService = grunnlagsendringshendelseService,
                oppgaveService = oppgaveService,
                requestLogger = behandlingRequestLogger,
            )
            klageRoutes(klageService = klageService, featureToggleService = featureToggleService)
            tilbakekrevingRoutes(service = tilbakekrevingService)
            behandlingRoutes(
                behandlingService = behandlingService,
                gyldighetsproevingService = gyldighetsproevingService,
                kommerBarnetTilGodeService = kommerBarnetTilGodeService,
                aktivitetspliktService = aktivtetspliktService,
                behandlingFactory = behandlingFactory,
            )
            sjekklisteRoute(sjekklisteService = sjekklisteService)
            statistikkRoutes(behandlingService = behandlingService)
            generellbehandlingRoutes(
                generellBehandlingService = generellBehandlingService,
                sakService = sakService,
            )
            revurderingRoutes(revurderingService = revurderingService, featureToggleService = featureToggleService)
            omregningRoutes(omregningService = omregningService)
            migreringRoutes(migreringService = migreringService)
            bosattUtlandRoutes(bosattUtlandService = bosattUtlandService)
            behandlingsstatusRoutes(behandlingsstatusService = behandlingsStatusService)
            behandlingVedtakRoute(
                behandlingsstatusService = behandlingsStatusService,
                oppgaveService = oppgaveService,
                behandlingService = behandlingService,
            )
            behandlingInfoRoutes(behandlingInfoService)
            oppgaveRoutes(
                service = oppgaveService,
                gosysOppgaveService = gosysOppgaveService,
            )
            grunnlagsendringshendelseRoute(grunnlagsendringshendelseService = grunnlagsendringshendelseService)
            egenAnsattRoute(
                egenAnsattService = EgenAnsattService(sakService, oppgaveService, sikkerLogg, enhetService),
                requestLogger = behandlingRequestLogger,
            )
            institusjonsoppholdRoute(institusjonsoppholdService = InstitusjonsoppholdService(institusjonsoppholdDao))
            tilgangRoutes(tilgangService)

            context.metrikkerJob.schedule().also { addShutdownHook(it) }

            install(adressebeskyttelsePlugin) {
                saksbehandlerGroupIdsByKey = context.saksbehandlerGroupIdsByKey

                harTilgangBehandling = { behandlingId, saksbehandlerMedRoller ->
                    tilgangService.harTilgangTilBehandling(behandlingId, saksbehandlerMedRoller)
                }
                harTilgangTilSak = { sakId, saksbehandlerMedRoller ->
                    tilgangService.harTilgangTilSak(sakId, saksbehandlerMedRoller)
                }
                harTilgangTilOppgave = { oppgaveId, saksbehandlerMedRoller ->
                    tilgangService.harTilgangTilOppgave(
                        oppgaveId,
                        saksbehandlerMedRoller,
                    )
                }
                harTilgangTilKlage = { klageId, saksbehandlerMedRoller ->
                    tilgangService.harTilgangTilKlage(
                        klageId,
                        saksbehandlerMedRoller,
                    )
                }
            }
        }
    }
}

private fun Route.attachContekst(
    ds: DataSource,
    context: ApplicationContext,
) {
    intercept(ApplicationCallPipeline.Call) {
        val requestContekst =
            Context(
                AppUser =
                    decideUser(
                        call.principal() ?: throw Exception("Ingen bruker funnet i jwt token"),
                        context.saksbehandlerGroupIdsByKey,
                        context.navAnsattKlient,
                        brukerTokenInfo,
                    ),
                databasecontxt = DatabaseContext(ds),
            )

        withContext(
            Dispatchers.Default +
                Kontekst.asContextElement(
                    value = requestContekst,
                ),
        ) {
            proceed()
        }
        Kontekst.remove()
    }
}
