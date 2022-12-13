package no.nav.etterlatte

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerRequest
import no.nav.etterlatte.libs.common.person.InvalidFoedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import org.slf4j.LoggerFactory
import java.lang.RuntimeException
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

fun Route.behandlingRoute(service: BehandlingService) {
    val logger = LoggerFactory.getLogger(this::class.java)

    route("saker") {
        // hent alle sakerª
        get {
            try {
                val accessToken = getAccessToken(call)
                val list = service.hentSaker(accessToken)
                call.respond(list)
            } catch (e: Exception) {
                throw e
            }
        }

        route("{sakId}") {
            // hent spesifikk sak med tilhørende behandlinger
            get {
                val sakId = call.parameters["sakId"]?.toInt()
                if (sakId == null) {
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("SakId mangler")
                } else {
                    call.respond(service.hentBehandlingerForSak(sakId, getAccessToken(call)))
                }
            }

            // Slett alle behandlinger på en sak
            delete("behandlinger") {
                val sakId = call.parameters["sakId"]?.toInt()
                if (sakId == null) {
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("SakId mangler")
                } else {
                    if (service.slettBehandlinger(sakId, getAccessToken(call))) {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            delete("revurderinger") {
                val sakId = call.parameters["sakId"]?.toInt()
                if (sakId == null) {
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("SakId mangler")
                } else {
                    if (service.slettRevurderinger(sakId, getAccessToken(call))) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }

            post("manueltopphoer") {
                try {
                    call.receive<ManueltOpphoerRequest>().also { req ->
                        service.opprettManueltOpphoer(req, getAccessToken(call)).also { opprettet ->
                            call.respond(opprettet)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Kunne ikke opprette manuelt opphoer", e)
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }
    }

    route("behandling/{behandlingId}") {
        get {
            call.withUUID("behandlingId") {
                call.respond(service.hentBehandling(it.toString(), getAccessToken(call)))
            }
        }

        get("hendelser") {
            call.withUUID("behandlingId") {
                call.respond(service.hentHendelserForBehandling(it.toString(), getAccessToken(call)))
            }
        }

        post("avbryt") {
            call.withUUID("behandlingId") {
                call.respond(service.avbrytBehanding(it.toString(), getAccessToken(call)))
            }
        }

        post("virkningstidspunkt") {
            call.withUUID("behandlingId") {
                val body = call.receive<VirkningstidspunktRequest>()

                if (!body.isValid()) {
                    return@withUUID call.respond(HttpStatusCode.BadRequest)
                }

                call.respond(
                    service.fastsettVirkningstidspunkt(
                        it.toString(),
                        body.dato,
                        getAccessToken(call)
                    )
                )
            }
        }
    }

    route("personer") {
        get("{fnr}") {
            val fnr = call.parameters["fnr"]
            if (fnr == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("Fødselsnummer mangler")
            } else {
                try {
                    call.respond(service.hentPersonOgSaker(fnr, getAccessToken(call)))
                } catch (e: InvalidFoedselsnummer) {
                    logger.error("Ugyldig fødselsnummer", e)
                    call.respond(HttpStatusCode.BadRequest, "Ugyldig fødselsnummer")
                }
            }
        }
    }
}

data class VirkningstidspunktRequest(@JsonProperty("dato") private val _dato: String) {
    val dato: YearMonth = try {
        LocalDate.ofInstant(Instant.parse(_dato), norskTidssone).let {
            YearMonth.of(it.year, it.month)
        }
    } catch (e: Exception) {
        throw RuntimeException("Kunne ikke lese dato for virkningstidspunkt: $_dato", e)
    }

    fun isValid() = when (dato.year) {
        in (0..9999) -> true
        else -> false
    }
}

suspend fun ApplicationCall.withUUID(parameter: String, onSuccess: (suspend (id: UUID) -> Unit)) {
    val id = this.parameters[parameter]
    if (id == null) {
        this.respond(HttpStatusCode.BadRequest, "Fant ikke følgende parameter: $parameter")
    }

    try {
        onSuccess(UUID.fromString(id))
    } catch (e: IllegalArgumentException) {
        this.respond(HttpStatusCode.BadRequest, "Ikke ett gyldigt UUID-format")
    }
}