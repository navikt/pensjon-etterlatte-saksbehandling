package no.nav.etterlatte.prosessering

import efterlatte.prosessering.ProcessingEngine
import efterlatte.prosessering.Reaper
import efterlatte.prosessering.StandardTaskProdusent
import efterlatte.prosessering.Status
import efterlatte.prosessering.TaskProdusent
import efterlatte.prosessering.ktor.Prosessering
import efterlatte.prosessering.ktor.taskProdusent
import efterlatte.prosessering.postgres.PostgresTaskRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.ktor.route.kunSaksbehandler
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import javax.sql.DataSource

fun Application.installProsessering(dataSource: DataSource) {
    install(Prosessering) {
        repository = PostgresTaskRepository(dataSource)
        steg = listOf(SoeknadMottakSkyggeTaskStep())
        node = "etterlatte-behandling"
        reaperPaa = true
    }
}

data class SoeknadSkyggeRequest(
    val soeknadId: String,
    val sakType: SakType,
    val fnrSoeker: String,
)

fun Route.soeknadSkyggeRoute(soeknadSkyggeDao: SoeknadSkyggeDao) {
    route("/api/prosessering/skygge/soeknad") {
        post {
            kunSystembruker {
                medBody<SoeknadSkyggeRequest> { request ->
                    if (soeknadSkyggeDao.harAlleredeHaandtertSoeknad(request.soeknadId)) {
                        call.respond(HttpStatusCode.OK, "allerede håndtert")
                        return@medBody
                    }
                    val taskProdusent: TaskProdusent = call.application.taskProdusent
                    taskProdusent.opprettIEgenTransaksjon(
                        SoeknadMottakSkygge,
                        SoeknadMottakSkyggePayload(
                            soeknadId = request.soeknadId,
                            sakType = request.sakType,
                            fnrSoeker = request.fnrSoeker,
                        ),
                    )
                    call.respond(HttpStatusCode.Created)
                }
            }
        }
    }
}

fun Route.prosesseringLesRoutes(
    prosesseringAdminDao: ProsesseringAdminDao,
    saksbehandlerGroupIdsByKey: Map<AzureGroup, String>,
) {
    route("/api/prosessering/task") {
        get {
            kunSaksbehandler { saksbehandler ->
                if (!SaksbehandlerMedRoller(saksbehandler, saksbehandlerGroupIdsByKey).harRolleProsessering()) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@kunSaksbehandler
                }
                val status =
                    call.request.queryParameters["status"]?.let {
                        runCatching { Status.valueOf(it) }.getOrNull()
                    }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                call.respond(prosesseringAdminDao.list(status = status, limit = limit))
            }
        }

        get("/{id}") {
            kunSaksbehandler { saksbehandler ->
                if (!SaksbehandlerMedRoller(saksbehandler, saksbehandlerGroupIdsByKey).harRolleProsessering()) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@kunSaksbehandler
                }
                val id =
                    krevIkkeNull(call.parameters["id"]?.toLongOrNull()) {
                        "id mangler eller er ugyldig"
                    }
                val task = prosesseringAdminDao.finn(id)
                if (task == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(task)
                }
            }
        }
    }
}
