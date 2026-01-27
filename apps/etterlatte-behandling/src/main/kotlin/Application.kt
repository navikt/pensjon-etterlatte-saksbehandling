package no.nav.etterlatte

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.intercept
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import no.nav.etterlatte.arbeidOgInntekt.arbeidOgInntekt
import no.nav.etterlatte.behandling.aktivitetsplikt.aktivitetspliktRoutes
import no.nav.etterlatte.behandling.behandlingRoutes
import no.nav.etterlatte.behandling.behandlingVedtakRoute
import no.nav.etterlatte.behandling.behandlinginfo.behandlingInfoRoutes
import no.nav.etterlatte.behandling.behandlingsstatusRoutes
import no.nav.etterlatte.behandling.bosattutland.bosattUtlandRoutes
import no.nav.etterlatte.behandling.etteroppgjoer.etteroppgjoerRoutes
import no.nav.etterlatte.behandling.generellbehandling.generellbehandlingRoutes
import no.nav.etterlatte.behandling.klage.klageRoutes
import no.nav.etterlatte.behandling.omregning.migreringRoutes
import no.nav.etterlatte.behandling.omregning.omregningRoutes
import no.nav.etterlatte.behandling.revurdering.revurderingRoutes
import no.nav.etterlatte.behandling.selftest.selfTestRoute
import no.nav.etterlatte.behandling.sjekkliste.sjekklisteRoute
import no.nav.etterlatte.behandling.statistikk.statistikkRoutes
import no.nav.etterlatte.behandling.tilbakekreving.tilbakekrevingRoutes
import no.nav.etterlatte.behandling.tilgang.tilgangRoutes
import no.nav.etterlatte.behandling.vedtaksbehandling.behandlingMedBrevRoutes
import no.nav.etterlatte.brev.brevRoute
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.egenansatt.EgenAnsattService
import no.nav.etterlatte.egenansatt.egenAnsattRoute
import no.nav.etterlatte.grunnlag.aldersovergang.aldersovergangRoutes
import no.nav.etterlatte.grunnlag.behandlingGrunnlagRoute
import no.nav.etterlatte.grunnlag.personRoute
import no.nav.etterlatte.grunnlag.sakGrunnlagRoute
import no.nav.etterlatte.grunnlagsendring.doedshendelse.doedshendelseRoute
import no.nav.etterlatte.grunnlagsendring.grunnlagsendringshendelseRoute
import no.nav.etterlatte.inntektsjustering.aarligInntektsjusteringRoute
import no.nav.etterlatte.inntektsjustering.selvbetjening.inntektsjusteringSelvbetjeningRoute
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdService
import no.nav.etterlatte.institusjonsopphold.institusjonsoppholdRoute
import no.nav.etterlatte.kodeverk.kodeverk
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstart
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.initialisering.runEngine
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.oppgave.oppgaveRoutes
import no.nav.etterlatte.oppgaveGosys.gosysOppgaveRoute
import no.nav.etterlatte.sak.sakSystemRoutes
import no.nav.etterlatte.sak.sakWebRoutes
import no.nav.etterlatte.saksbehandler.saksbehandlerRoutes
import no.nav.etterlatte.tilgangsstyring.PluginConfiguration
import no.nav.etterlatte.tilgangsstyring.SpesialTilgangPlugin
import no.nav.etterlatte.vilkaarsvurdering.aldersovergang
import no.nav.etterlatte.vilkaarsvurdering.vilkaarsvurdering
import org.slf4j.Logger
import javax.sql.DataSource

val sikkerLogg: Logger = sikkerlogger()

fun main() {
    Server(ApplicationContext()).runServer()
}

private class Server(
    private val context: ApplicationContext,
) {
    init {
        sikkerLoggOppstart("etterlatte-behandling")
    }

    private val engine =
        initEmbeddedServer(
            httpPort = context.httpPort,
            applicationConfig = context.config,
            cronJobs = timerJobs(context),
            routes = { selfTestRoute(context.selfTestService) },
        ) {
            settOppApplikasjonen(context)
        }

    fun runServer() =
        with(context) {
            dataSource.migrate()
            engine.runEngine()
        }
}

private fun timerJobs(context: ApplicationContext): List<TimerJob> =
    listOf(
        context.metrikkerJob,
        context.doedsmeldingerJob,
        context.doedsmeldingerReminderJob,
        context.saksbehandlerJob,
        context.etteroppgjoerJob,
        context.etteroppgjoerSvarfristUtloeptJob,
        context.aktivitetspliktOppgaveUnntakUtloeperJob,
        context.sjekkAdressebeskyttelseJob,
        context.uttrekkLoependeYtelseEtter67Job,
        context.lesSkatteoppgjoerHendelserJob,
        context.oppdaterSkatteoppgjoerIkkeMottattJob,
    )

@Deprecated("Denne blir brukt i veldig mange testar. Bør rydde opp, men tar det etter denne endringa er inne")
internal fun Application.module(context: ApplicationContext) {
    restModule(
        sikkerLogg,
        withMetrics = true,
    ) {
        settOppApplikasjonen(context)
    }
}

internal fun Route.settOppApplikasjonen(context: ApplicationContext) {
    attachContekst(context.dataSource, context)
    settOppRoutes(context)
    settOppTilganger(context, SpesialTilgangPlugin)
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
                brukerTokenInfo = brukerTokenInfo,
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
    tilbakekrevingRoutes(service = applicationContext.tilbakekrevingService, featureToggleService = applicationContext.featureToggleService)
    etteroppgjoerRoutes(
        forbehandlingService = applicationContext.etteroppgjoerForbehandlingService,
        forbehandlingBrevService = applicationContext.etteroppgjoerForbehandlingBrevService,
        etteroppgjoerService = applicationContext.etteroppgjoerService,
        skatteoppgjoerHendelserService = applicationContext.skatteoppgjoerHendelserService,
        featureToggleService = applicationContext.featureToggleService,
        etteroppgjoerRevurderingService = applicationContext.etteroppgjoerRevurderingService,
    )
    behandlingRoutes(
        behandlingService = applicationContext.behandlingService,
        gyldighetsproevingService = applicationContext.gyldighetsproevingService,
        kommerBarnetTilGodeService = applicationContext.kommerBarnetTilGodeService,
        behandlingFactory = applicationContext.behandlingFactory,
    )

    brevRoute(service = applicationContext.brevService)

    aktivitetspliktRoutes(
        aktivitetspliktService = applicationContext.aktivitetspliktService,
        aktivitetspliktOppgaveService = applicationContext.aktivitetspliktOppgaveService,
    )
    sjekklisteRoute(sjekklisteService = applicationContext.sjekklisteService)
    statistikkRoutes(behandlingService = applicationContext.behandlingService)
    generellbehandlingRoutes(
        generellBehandlingService = applicationContext.generellBehandlingService,
        sakService = applicationContext.sakService,
    )
    behandlingMedBrevRoutes(behandlingMedBrevService = applicationContext.behandlingMedBrevService)
    revurderingRoutes(
        revurderingService = applicationContext.revurderingService,
        manuellRevurderingService = applicationContext.manuellRevurderingService,
        omgjoeringKlageRevurderingService = applicationContext.omgjoeringKlageRevurderingService,
        automatiskRevurderingService = applicationContext.automatiskRevurderingService,
        aarligInntektsjusteringJobbService = applicationContext.aarligInntektsjusteringJobbService,
        etteroppgjoerRevurderingService = applicationContext.etteroppgjoerRevurderingService,
    )
    omregningRoutes(omregningService = applicationContext.omregningService)
    aarligInntektsjusteringRoute(service = applicationContext.aarligInntektsjusteringJobbService)
    inntektsjusteringSelvbetjeningRoute(service = applicationContext.inntektsjusteringSelvbetjeningService)
    migreringRoutes(migreringService = applicationContext.migreringService)
    bosattUtlandRoutes(bosattUtlandService = applicationContext.bosattUtlandService)
    behandlingsstatusRoutes(behandlingsstatusService = applicationContext.behandlingsStatusService)
    behandlingVedtakRoute(
        behandlingsstatusService = applicationContext.behandlingsStatusService,
        behandlingService = applicationContext.behandlingService,
    )
    behandlingInfoRoutes(applicationContext.behandlingInfoService)
    gosysOppgaveRoute(applicationContext.gosysOppgaveService)
    oppgaveRoutes(applicationContext.oppgaveService, applicationContext.oppgaveKommentarService)
    grunnlagsendringshendelseRoute(
        grunnlagsendringshendelseService = applicationContext.grunnlagsendringshendelseService,
    )
    doedshendelseRoute(doedshendelseService = applicationContext.doedshendelseService)
    egenAnsattRoute(
        egenAnsattService =
            EgenAnsattService(
                applicationContext.sakService,
                applicationContext.grunnlagService,
                applicationContext.oppdaterTilgangService,
            ),
        requestLogger = applicationContext.behandlingRequestLogger,
    )
    institusjonsoppholdRoute(institusjonsoppholdService = InstitusjonsoppholdService(applicationContext.institusjonsoppholdDao))
    saksbehandlerRoutes(saksbehandlerService = applicationContext.saksbehandlerService)

    tilgangRoutes(applicationContext.tilgangService)
    kodeverk(applicationContext.kodeverkService)
    vilkaarsvurdering(applicationContext.vilkaarsvurderingService)
    aldersovergang(applicationContext.aldersovergangService)

    arbeidOgInntekt(applicationContext.arbeidOgInntektKlient)

    route("/api/grunnlag") {
        behandlingGrunnlagRoute(applicationContext.grunnlagService)
        personRoute(applicationContext.grunnlagService, applicationContext.tilgangService)
        sakGrunnlagRoute(applicationContext.grunnlagService)
        aldersovergangRoutes(applicationContext.nyAldersovergangService)
    }
}

private fun Route.settOppTilganger(
    context: ApplicationContext,
    adressebeskyttelsePlugin: RouteScopedPlugin<PluginConfiguration>,
) {
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
            harTilgangTilGenerellBehandling = { generellbehandlingId, saksbehandlerMedRoller ->
                tilgangService.harTilgangTilGenerellBehandling(generellbehandlingId, saksbehandlerMedRoller)
            }
            harTilgangTilTilbakekreving = { tilbakekrevingId, saksbehandlerMedRoller ->
                tilgangService.harTilgangTilTilbakekreving(tilbakekrevingId, saksbehandlerMedRoller)
            }
            harTilgangTilEtteroppgjoer = { etteroppgjoerId, saksbehandlerMedRoller ->
                tilgangService.harTilgangTilEtteroppgjoer(etteroppgjoerId, saksbehandlerMedRoller)
            }
        }
    }
}
