package no.nav.etterlatte.brev.oversendelsebrev

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.BREV_ID_CALL_PARAMETER
import no.nav.etterlatte.brev.brevId
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.route.withSakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

private val logger = LoggerFactory.getLogger("oversendelseBrevRoute")

fun Route.oversendelseBrevRoute(
    service: OversendelseBrevService,
    tilgangssjekker: Tilgangssjekker,
) {
    route("/brev/behandling/{$BEHANDLINGID_CALL_PARAMETER}/oversendelse") {
        get {
            withBehandlingId(tilgangssjekker) { behandlingId ->
                measureTimedValue {
                    service.hentOversendelseBrev(behandlingId)
                }.let { (brev, varighet) ->
                    logger.info("Henting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    if (brev != null) {
                        call.respond(brev)
                    } else {
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }

        post {
            withBehandlingId(tilgangssjekker, skrivetilgang = true) { behandlingId ->
                measureTimedValue {
                    service.opprettOversendelseBrev(behandlingId, brukerTokenInfo)
                }.let { (brev, varighet) ->
                    logger.info("Oppretting av oversendelsebrev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(brev)
                }
            }
        }

        delete {
            withBehandlingId(tilgangssjekker, skrivetilgang = true) { behandlingId ->
                service.slettOversendelseBrev(behandlingId, brukerTokenInfo)
                call.respond(HttpStatusCode.NoContent)
            }
        }

        get("pdf") {
            val brevId =
                call.parameters["brevId"]?.toLong() ?: throw UgyldigForespoerselException(
                    "MANGLER_BREV_ID",
                    "Mangler brevId i url parameters",
                )

            withBehandlingId(tilgangssjekker, skrivetilgang = false) { behandlingId ->
                measureTimedValue {
                    service.pdf(brevId, behandlingId, brukerTokenInfo).bytes
                }.let { (brev, varighet) ->
                    logger.info(
                        "Henting av pdf for oversendelsesbrev tok ${
                            varighet.toString(
                                DurationUnit.SECONDS,
                                2,
                            )
                        }",
                    )
                    call.respond(brev)
                }
            }
        }
    }

    route("brev/{$BREV_ID_CALL_PARAMETER}/oversendelse") {
        post("ferdigstill") {
            withSakId(tilgangssjekker, skrivetilgang = true) { sakId ->
                val brev = service.ferdigstillOversendelseBrev(brevId, sakId, brukerTokenInfo)
                call.respond(brev)
            }
        }
    }
}
