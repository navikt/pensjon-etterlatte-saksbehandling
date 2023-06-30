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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import no.nav.etterlatte.behandling.behandlingRoutes
import no.nav.etterlatte.behandling.behandlingsstatusRoutes
import no.nav.etterlatte.behandling.omregning.migreringRoutes
import no.nav.etterlatte.behandling.omregning.omregningRoutes
import no.nav.etterlatte.behandling.revurdering.revurderingRoutes
import no.nav.etterlatte.behandling.tilgang.tilgangRoutes
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.egenansatt.EgenAnsattService
import no.nav.etterlatte.egenansatt.egenAnsattRoute
import no.nav.etterlatte.grunnlagsendring.grunnlagsendringshendelseRoute
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdService
import no.nav.etterlatte.institusjonsopphold.institusjonsoppholdRoute
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

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    Server(ApplicationContext()).run()
}

class Server(private val context: ApplicationContext) {

    init {
        sikkerLogg.info("SikkerLogg: etterlatte-behandling oppstart")
    }

    private val engine = embeddedServer(
        factory = CIO,
        environment = applicationEngineEnvironment {
            config = HoconApplicationConfig(context.config)
            module { module(context) }
            connector { port = context.httpPort }
        }
    )

    fun run() = with(context) {
        dataSource.migrate()

        val grunnlagsendringshendelseJobSkedulert = grunnlagsendringshendelseJob.schedule()
        setReady().also { engine.start(true) }

        Runtime.getRuntime().addShutdownHook(
            Thread {
                grunnlagsendringshendelseJob.setClosedTrue()
                grunnlagsendringshendelseJobSkedulert.cancel()
            }
        )
    }
}

fun Application.module(context: ApplicationContext) {
    with(context) {
        restModule(
            sikkerLogg
        ) {
            attachContekst(dataSource, context)
            sakSystemRoutes(
                tilgangService = tilgangService,
                sakService = sakService,
                generellBehandlingService = generellBehandlingService
            )
            sakWebRoutes(
                tilgangService = tilgangService,
                sakService = sakService,
                generellBehandlingService = generellBehandlingService,
                grunnlagsendringshendelseService = grunnlagsendringshendelseService
            )
            behandlingRoutes(
                generellBehandlingService = generellBehandlingService,
                foerstegangsbehandlingService = foerstegangsbehandlingService,
                manueltOpphoerService = manueltOpphoerService
            )
            revurderingRoutes(
                revurderingService = revurderingService,
                generellBehandlingService = generellBehandlingService
            )
            omregningRoutes(omregningService = omregningService)
            migreringRoutes(migreringService = migreringService)
            behandlingsstatusRoutes(behandlingsstatusService = behandlingsStatusService)
            oppgaveRoutes(service = oppgaveService)
            grunnlagsendringshendelseRoute(grunnlagsendringshendelseService = grunnlagsendringshendelseService)
            egenAnsattRoute(egenAnsattService = EgenAnsattService(sakService, sikkerLogg))
            institusjonsoppholdRoute(institusjonsoppholdService = InstitusjonsoppholdService(institusjonsoppholdDao))
            tilgangRoutes(tilgangService)

            install(adressebeskyttelsePlugin) {
                saksbehandlerGroupIdsByKey = context.saksbehandlerGroupIdsByKey
                harTilgangBehandling = { behandlingId, saksbehandlerMedRoller ->
                    tilgangService.harTilgangTilBehandling(behandlingId, saksbehandlerMedRoller)
                }
                harTilgangTilSak = { sakId, saksbehandlerMedRoller ->
                    tilgangService.harTilgangTilSak(sakId, saksbehandlerMedRoller)
                }
            }
        }
    }
}

private fun Route.attachContekst(
    ds: DataSource,
    context: ApplicationContext
) {
    intercept(ApplicationCallPipeline.Call) {
        val requestContekst =
            Context(
                AppUser = decideUser(
                    call.principal() ?: throw Exception("Ingen bruker funnet i jwt token"),
                    context.saksbehandlerGroupIdsByKey,
                    context.enhetService,
                    brukerTokenInfo
                ),
                databasecontxt = DatabaseContext(ds)
            )

        withContext(
            Dispatchers.Default + Kontekst.asContextElement(
                value = requestContekst
            )
        ) {
            proceed()
        }
        Kontekst.remove()
    }
}