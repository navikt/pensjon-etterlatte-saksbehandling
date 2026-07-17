package no.nav.etterlatte.prosessering

import efterlatte.prosessering.Status
import efterlatte.prosessering.Task
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.ktor.route.kunSaksbehandler
import org.slf4j.LoggerFactory
import java.time.Instant

private val logger = LoggerFactory.getLogger("ProsesseringAdminRoutes")

/**
 * Ren 5-status-DTO for operatør-innsyn. Ingen `Ressurs`-konvolutt og ingen
 * 8-status-oversettelse — GUIet ([etterlatte-testdata]) tegnes rett mot vår egen modell.
 */
data class ProsesseringTaskDto(
    val id: Long,
    val type: String,
    val status: Status,
    val antallFeil: Int,
    val stoppaarsak: String?,
    val triggerTid: Instant,
    val opprettetTid: Instant,
    val plukketTid: Instant?,
    val payload: String?,
)

private fun Task.tilDto() =
    ProsesseringTaskDto(
        id = id,
        type = type,
        status = status,
        antallFeil = antallFeil,
        stoppaarsak = stoppaarsak?.name,
        triggerTid = triggerTid,
        opprettetTid = opprettetTid,
        plukketTid = plukketTid,
        payload = payload,
    )

/**
 * Operatør-endepunkter for å se og styre prosessering-tasker (PoC Fase 4c). Kun lesing
 * og manuell «rekjør» — motoren selv eier all annen tilstandsendring. Saksbehandler-auth,
 * gated bak [ProsesseringToggles.PROSESSERING_ADMIN] slik at innsynet kan skrus av uavhengig
 * av selve skyggekjøringen.
 */
fun Route.prosesseringAdminRoutes(
    dao: ProsesseringAdminDao,
    featureToggleService: FeatureToggleService,
) {
    route("/api/prosessering/task") {
        get {
            kunSaksbehandler {
                if (avslaattEllerRespondert(featureToggleService)) return@kunSaksbehandler

                val status = call.request.queryParameters["status"]?.let { Status.valueOf(it) }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                call.respond(dao.list(status = status, limit = limit).map { it.tilDto() })
            }
        }

        get("{id}") {
            kunSaksbehandler {
                if (avslaattEllerRespondert(featureToggleService)) return@kunSaksbehandler

                val id = taskId() ?: return@kunSaksbehandler
                val task = dao.finn(id)
                if (task == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(task.tilDto())
                }
            }
        }

        put("{id}/rekjor") {
            kunSaksbehandler { saksbehandler ->
                if (avslaattEllerRespondert(featureToggleService)) return@kunSaksbehandler

                val id = taskId() ?: return@kunSaksbehandler
                if (dao.rekjor(id)) {
                    logger.info("Saksbehandler ${saksbehandler.ident()} rekjørte prosessering-task $id")
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.Conflict, "Task $id kan ikke rekjøres (ikke STOPPET/AVBRUTT)")
                }
            }
        }
    }
}

private suspend fun io.ktor.server.routing.RoutingContext.avslaattEllerRespondert(featureToggleService: FeatureToggleService): Boolean {
    if (!featureToggleService.isEnabled(ProsesseringToggles.PROSESSERING_ADMIN, false)) {
        call.respond(HttpStatusCode.NotFound)
        return true
    }
    return false
}

private suspend fun io.ktor.server.routing.RoutingContext.taskId(): Long? {
    val id = call.parameters["id"]?.toLongOrNull()
    if (id == null) {
        call.respond(HttpStatusCode.BadRequest, "Ugyldig task-id")
    }
    return id
}
