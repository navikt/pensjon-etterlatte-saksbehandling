package no.nav.etterlatte

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ServerReady
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.principal
import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import no.nav.etterlatte.behandling.behandlingRoutes
import no.nav.etterlatte.behandling.behandlingVedtakRoute
import no.nav.etterlatte.behandling.behandlinginfo.behandlingInfoRoutes
import no.nav.etterlatte.behandling.behandlingsstatusRoutes
import no.nav.etterlatte.behandling.bosattutland.bosattUtlandRoutes
import no.nav.etterlatte.behandling.generellbehandling.generellbehandlingRoutes
import no.nav.etterlatte.behandling.klage.klageRoutes
import no.nav.etterlatte.behandling.omregning.migreringRoutes
import no.nav.etterlatte.behandling.omregning.omregningRoutes
import no.nav.etterlatte.behandling.revurdering.revurderingRoutes
import no.nav.etterlatte.behandling.sjekklisteRoute
import no.nav.etterlatte.behandling.statistikk.statistikkRoutes
import no.nav.etterlatte.behandling.tilbakekreving.tilbakekrevingRoutes
import no.nav.etterlatte.behandling.tilgang.tilgangRoutes
import no.nav.etterlatte.behandling.vedtaksbehandling.vedtaksbehandlingRoutes
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.egenansatt.EgenAnsattService
import no.nav.etterlatte.egenansatt.egenAnsattRoute
import no.nav.etterlatte.grunnlagsendring.doedshendelse.doedshendelseRoute
import no.nav.etterlatte.grunnlagsendring.grunnlagsendringshendelseRoute
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdService
import no.nav.etterlatte.institusjonsopphold.institusjonsoppholdRoute
import no.nav.etterlatte.jobs.addShutdownHook
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.ktor.shutdownPolicyEmbeddedServer
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.oppgave.oppgaveRoutes
import no.nav.etterlatte.oppgaveGosys.gosysOppgaveRoute
import no.nav.etterlatte.sak.sakSystemRoutes
import no.nav.etterlatte.sak.sakWebRoutes
import no.nav.etterlatte.saksbehandler.saksbehandlerRoutes
import no.nav.etterlatte.tilgangsstyring.adressebeskyttelsePlugin
import org.slf4j.Logger
import java.util.Timer
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
            configure = shutdownPolicyEmbeddedServer(),
            factory = CIO,
            environment =
                applicationEngineEnvironment {
                    config = HoconApplicationConfig(context.config)
                    module { module(context) }
                    module { moduleOnServerReady(context) }
                    connector { port = context.httpPort }
                },
        )

    fun run() =
        with(context) {
            dataSource.migrate()
            setReady().also { engine.start(true) }
        }
}

internal fun Application.moduleOnServerReady(context: ApplicationContext) {
    environment.monitor.subscribe(ServerReady) {
        shutdownHooks(context).forEach { it.value.apply { it.key } }
    }
}

private fun shutdownHooks(context: ApplicationContext): Map<Timer, (Timer) -> Unit> =
    mapOf(
        context.metrikkerJob.schedule() to { addShutdownHook(it) },
        context.doedsmeldingerJob.schedule() to { addShutdownHook(it) },
        context.saksbehandlerJob.schedule() to { addShutdownHook(it) },
        context.fristGaarUtJobb.schedule() to { addShutdownHook(it) },
    )

internal fun Application.module(context: ApplicationContext) {
    restModule(
        sikkerLogg,
        withMetrics = true,
    ) {
        attachContekst(context.dataSource, context)
        settOppRoutes(context)

        with(context) {
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

private fun Route.settOppRoutes(applicationContext: ApplicationContext) {
    sakSystemRoutes(
        tilgangService = applicationContext.tilgangService,
        sakService = applicationContext.sakService,
        behandlingService = applicationContext.behandlingService,
        requestLogger = applicationContext.behandlingRequestLogger,
    )
    sakWebRoutes(
        tilgangService = applicationContext.tilgangService,
        sakService = applicationContext.sakService,
        behandlingService = applicationContext.behandlingService,
        grunnlagsendringshendelseService = applicationContext.grunnlagsendringshendelseService,
        oppgaveService = applicationContext.oppgaveService,
        requestLogger = applicationContext.behandlingRequestLogger,
        hendelseDao = applicationContext.hendelseDao,
    )
    klageRoutes(
        klageService = applicationContext.klageService,
        featureToggleService = applicationContext.featureToggleService,
    )
    tilbakekrevingRoutes(service = applicationContext.tilbakekrevingService)
    behandlingRoutes(
        behandlingService = applicationContext.behandlingService,
        gyldighetsproevingService = applicationContext.gyldighetsproevingService,
        kommerBarnetTilGodeService = applicationContext.kommerBarnetTilGodeService,
        aktivitetspliktService = applicationContext.aktivtetspliktService,
        behandlingFactory = applicationContext.behandlingFactory,
    )
    sjekklisteRoute(sjekklisteService = applicationContext.sjekklisteService)
    statistikkRoutes(behandlingService = applicationContext.behandlingService)
    generellbehandlingRoutes(
        generellBehandlingService = applicationContext.generellBehandlingService,
        sakService = applicationContext.sakService,
    )
    vedtaksbehandlingRoutes(vedtaksbehandlingService = applicationContext.vedtaksbehandlingService)
    revurderingRoutes(revurderingService = applicationContext.revurderingService)
    omregningRoutes(omregningService = applicationContext.omregningService)
    migreringRoutes(migreringService = applicationContext.migreringService)
    bosattUtlandRoutes(bosattUtlandService = applicationContext.bosattUtlandService)
    behandlingsstatusRoutes(behandlingsstatusService = applicationContext.behandlingsStatusService)
    behandlingVedtakRoute(
        behandlingsstatusService = applicationContext.behandlingsStatusService,
        oppgaveService = applicationContext.oppgaveService,
        behandlingService = applicationContext.behandlingService,
    )
    behandlingInfoRoutes(applicationContext.behandlingInfoService)
    gosysOppgaveRoute(applicationContext.gosysOppgaveService)
    oppgaveRoutes(applicationContext.oppgaveService)
    grunnlagsendringshendelseRoute(grunnlagsendringshendelseService = applicationContext.grunnlagsendringshendelseService)
    doedshendelseRoute(doedshendelseService = applicationContext.doedshendelseService)
    egenAnsattRoute(
        egenAnsattService =
            EgenAnsattService(
                applicationContext.sakService,
                applicationContext.oppgaveService,
                sikkerLogg,
                applicationContext.enhetService,
            ),
        requestLogger = applicationContext.behandlingRequestLogger,
    )
    institusjonsoppholdRoute(institusjonsoppholdService = InstitusjonsoppholdService(applicationContext.institusjonsoppholdDao))
    saksbehandlerRoutes(saksbehandlerService = applicationContext.saksbehandlerService)

    tilgangRoutes(applicationContext.tilgangService)
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
                        context.saksbehandlerService,
                        brukerTokenInfo,
                    ),
                databasecontxt = DatabaseContext(ds),
                sakTilgangDao = context.sakTilgangDao,
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
