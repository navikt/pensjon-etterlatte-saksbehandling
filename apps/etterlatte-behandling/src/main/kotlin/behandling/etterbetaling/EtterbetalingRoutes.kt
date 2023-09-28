package no.nav.etterlatte.behandling.etterbetaling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandling.Etterbetaling
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.medBody
import java.time.LocalDate

internal fun Route.etterbetalingRoutes(service: EtterbetalingService) {
    route("/api/behandling/etterbetaling/{$BEHANDLINGSID_CALL_PARAMETER}") {
        post {
            medBody<EtterbetalingDTO> { request ->
                service.lagreEtterbetaling(
                    Etterbetaling(
                        behandlingId = behandlingsId,
                        fraDato = requireNotNull(request.fraDato),
                        tilDato = requireNotNull(request.tilDato),
                    ),
                )
            }
            call.respond(HttpStatusCode.Created)
        }

        get {
            when (val etterbetaling = service.hentEtterbetaling(behandlingsId)) {
                null -> call.respond(HttpStatusCode.NoContent)
                else ->
                    call.respond(
                        HttpStatusCode.OK,
                        EtterbetalingDTO(fraDato = etterbetaling.fraDato, tilDato = etterbetaling.tilDato),
                    )
            }
        }
    }
}

data class EtterbetalingDTO(
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
)
