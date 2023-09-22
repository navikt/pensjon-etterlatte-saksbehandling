package no.nav.etterlatte.samordning.vedtak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.ktor.hentTokenClaims
import java.time.LocalDate

fun Route.samordningVedtakRoute(samordningVedtakService: SamordningVedtakService) {
    route("ping") {
        get {
            call.respond("Tjeneste ok")
        }
    }

    route("api/vedtak") {
        get("{vedtakId}") {
            val vedtakId = requireNotNull(call.parameters["vedtakId"]).toLong()

            val samordningVedtakDto =
                try {
                    samordningVedtakService.hentVedtak(
                        vedtakId = vedtakId,
                        organisasjonsnummer = call.orgNummer,
                    )
                } catch (e: VedtakFeilSakstypeException) {
                    call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til sakstype")
                } catch (e: IllegalArgumentException) {
                    call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(samordningVedtakDto)
        }

        get {
            val virkFom =
                call.parameters["virkFom"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "virkFom ikke angitt")

            val fnr =
                call.request.headers["fnr"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "fnr ikke angitt")

            val samordningVedtakDtos =
                try {
                    samordningVedtakService.hentVedtaksliste(
                        virkFom = virkFom,
                        fnr = fnr,
                        organisasjonsnummer = call.orgNummer,
                    )
                } catch (e: IllegalArgumentException) {
                    call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(samordningVedtakDtos)
        }
    }
}

inline val ApplicationCall.orgNummer: String
    get() {
        val claims =
            this.hentTokenClaims("maskinporten")
                ?.get("consumer") as Map<*, *>?
                ?: throw IllegalArgumentException("Kan ikke hente ut organisasjonsnummer")
        return (claims["ID"] as String).split(":")[1]
    }
