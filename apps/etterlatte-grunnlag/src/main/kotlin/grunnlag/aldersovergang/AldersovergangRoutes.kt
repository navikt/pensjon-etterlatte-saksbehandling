package no.nav.etterlatte.grunnlag.aldersovergang

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import java.time.YearMonth

fun Route.aldersovergangRoutes(aldersovergangService: AldersovergangService) {
    route("/aldersovergang") {
        get("{yearMonth}") {
            val yearMonth = call.parameters["yearMonth"].toString().let { YearMonth.parse(it) }
            call.respond(aldersovergangService.hentSoekereFoedtIEnGittMaaned(yearMonth))
        }
        get("sak/{sakId}/{sakType}") {
            val sakId =
                call.parameters["sakId"]?.toLong() ?: throw UgyldigForespoerselException(
                    "MANGLER_SAKID",
                    "Mangler sakId",
                )
            val sakType =
                call.parameters["sakType"]?.let { SakType.valueOf(it) }
                    ?: throw UgyldigForespoerselException("MANGLER_SAKTYPE", "Mangler sakType")

            when (val maaned = aldersovergangService.aldersovergangMaaned(sakId, sakType)) {
                null -> call.respond(HttpStatusCode.NoContent)
                else -> call.respond(maaned)
            }
        }
    }

    route("/doedsdato") {
        get("{yearMonth}") {
            val behandlingsmaaned = call.parameters["yearMonth"].toString().let { YearMonth.parse(it) }
            call.respond(aldersovergangService.hentSakerHvorDoedsfallForekomIGittMaaned(behandlingsmaaned))
        }
    }
}
