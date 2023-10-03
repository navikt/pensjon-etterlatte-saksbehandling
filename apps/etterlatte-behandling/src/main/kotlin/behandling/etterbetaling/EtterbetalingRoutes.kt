package no.nav.etterlatte.behandling.etterbetaling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandling.Etterbetaling
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.medBody
import java.time.LocalDate
import java.time.YearMonth

internal fun Route.etterbetalingRoutes(service: EtterbetalingService) {
    route("/api/behandling/{$BEHANDLINGSID_CALL_PARAMETER}/etterbetaling") {
        put {
            medBody<EtterbetalingDTO> { request ->
                service.lagreEtterbetaling(
                    Etterbetaling(
                        behandlingId = behandlingsId,
                        fra = requireNotNull(request.fraDato) { "Mangler fradato etterbetaling" }.let {
                            YearMonth.from(
                                it
                            )
                        },
                        til = requireNotNull(request.tilDato) { "Mangler tildato etterbetaling" }.let {
                            YearMonth.from(
                                it
                            )
                        },
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
                        EtterbetalingDTO(
                            fraDato = etterbetaling.fra.atDay(1),
                            tilDato = etterbetaling.til.atEndOfMonth(),
                        ),
                    )
            }
        }
    }
}

data class EtterbetalingDTO(
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
)
