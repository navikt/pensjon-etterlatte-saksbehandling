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
import no.nav.etterlatte.behandling.EnhetService
import no.nav.etterlatte.behandling.behandlingRoutes
import no.nav.etterlatte.behandling.behandlingsstatusRoutes
import no.nav.etterlatte.behandling.omregning.omregningRoutes
import no.nav.etterlatte.behandling.revurdering.revurderingRoutes
import no.nav.etterlatte.behandling.tilgang.tilgangRoutes
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.config.BeanFactory
import no.nav.etterlatte.config.EnvBasedBeanFactory
import no.nav.etterlatte.egenansatt.EgenAnsattService
import no.nav.etterlatte.egenansatt.egenAnsattRoute
import no.nav.etterlatte.grunnlagsendring.grunnlagsendringshendelseRoute
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.oppgave.oppgaveRoutes
import no.nav.etterlatte.sak.sakRoutes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    Server(EnvBasedBeanFactory(System.getenv())).run()
}

class Server(private val beanFactory: BeanFactory) {

    init {
        sikkerLogg.info("SikkerLogg: etterlatte-behandling oppstart")
    }

    private val engine = embeddedServer(
        factory = CIO,
        environment = applicationEngineEnvironment {
            config = HoconApplicationConfig(beanFactory.config)
            module { module(beanFactory) }
            connector { port = 8080 }
        }
    )

    fun run() = with(beanFactory) {
        dataSource().migrate()

        val behandlingHendelser = behandlingHendelser()
        grunnlagsendringshendelseJob()
        behandlingHendelser.start()

        setReady().also { engine.start(true) }
        behandlingHendelser.nyHendelse.close()
    }
}

fun Application.module(beanFactory: BeanFactory) {
    with(beanFactory) {
        val generellBehandlingService = generellBehandlingService()
        val grunnlagsendringshendelseService = grunnlagsendringshendelseService()

        val tilgangService = tilgangService()
        val sakService = sakService()
        val enhetService = enhetService()
        val saksbehandlerGroupIds = getSaksbehandlerGroupIdsByKey()

        restModule(
            sikkerLogg
        ) {
            attachContekst(dataSource(), enhetService, saksbehandlerGroupIds)
            sakRoutes(
                tilgangService = tilgangService,
                sakService = sakService,
                generellBehandlingService = generellBehandlingService,
                grunnlagsendringshendelseService = grunnlagsendringshendelseService
            )
            behandlingRoutes(
                generellBehandlingService = generellBehandlingService,
                foerstegangsbehandlingService = foerstegangsbehandlingService(),
                manueltOpphoerService = manueltOpphoerService()
            )
            revurderingRoutes(
                revurderingService = revurderingService(),
                generellBehandlingService = generellBehandlingService
            )
            omregningRoutes(omregningService = omregningService())
            behandlingsstatusRoutes(behandlingsstatusService = behandlingsStatusService())
            oppgaveRoutes(service = beanFactory.oppgaveService())
            grunnlagsendringshendelseRoute(grunnlagsendringshendelseService = grunnlagsendringshendelseService)
            egenAnsattRoute(egenAnsattService = EgenAnsattService(sakService))
            tilgangRoutes(tilgangService)
            install(adressebeskyttelsePlugin) {
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
    enhetService: EnhetService,
    saksbehandlerGroupIds: Map<String, String>
) {
    intercept(ApplicationCallPipeline.Call) {
        val requestContekst =
            Context(
                AppUser = decideUser(
                    call.principal() ?: throw Exception("Ingen bruker funnet i jwt token"),
                    saksbehandlerGroupIds,
                    enhetService
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