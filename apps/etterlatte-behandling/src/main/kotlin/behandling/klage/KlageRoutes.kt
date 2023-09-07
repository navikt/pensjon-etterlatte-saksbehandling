package no.nav.etterlatte.behandling.klage

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.KLAGEID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.klageId
import no.nav.etterlatte.libs.common.sakId

internal fun Route.klageRoutes(klageService: KlageService) {
    route("/api/klage") {
        post("opprett/{$SAKID_CALL_PARAMETER}") {
            val sakId = sakId
            val klage = inTransaction {
                klageService.opprettKlage(sakId)
            }
            call.respond(klage)
        }

        get("{$KLAGEID_CALL_PARAMETER}") {
            val klage = inTransaction {
                klageService.hentKlage(klageId)
            }
            when (klage) {
                null -> call.respond(HttpStatusCode.NotFound)
                else -> call.respond(klage)
            }
        }

        get("sak/{$SAKID_CALL_PARAMETER}") {
            val klager = inTransaction {
                klageService.hentKlagerISak(sakId)
            }
            call.respond(klager)
        }
    }
}