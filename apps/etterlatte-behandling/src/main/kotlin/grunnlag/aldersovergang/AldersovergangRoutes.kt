package no.nav.etterlatte.grunnlag.aldersovergang

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import java.time.YearMonth

fun Route.aldersovergangRoutes(aldersovergangService: IAldersovergangService) {
    route("/aldersovergang") {
        get("{yearMonth}") {
            val yearMonth = call.parameters["yearMonth"].toString().let { YearMonth.parse(it) }
            call.respond(aldersovergangService.hentSoekereFoedtIEnGittMaaned(yearMonth, brukerTokenInfo))
        }
        get("sak/{sakId}/{sakType}") {
            val sakType =
                call.parameters["sakType"]?.let { SakType.valueOf(it) }
                    ?: throw UgyldigForespoerselException("MANGLER_SAKTYPE", "Mangler sakType")

            when (val maaned = aldersovergangService.aldersovergangMaaned(sakId, sakType, brukerTokenInfo)) {
                null -> call.respond(HttpStatusCode.NoContent)
                else -> call.respond(maaned)
            }
        }
    }

    route("/doedsdato") {
        get("{yearMonth}") {
            val behandlingsmaaned = call.parameters["yearMonth"].toString().let { YearMonth.parse(it) }
            call.respond(
                aldersovergangService.hentSakerHvorDoedsfallForekomIGittMaaned(
                    behandlingsmaaned,
                    brukerTokenInfo,
                ),
            )
        }
    }
}
