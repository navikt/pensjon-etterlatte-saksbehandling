package no.nav.etterlatte.brev.notat

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
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
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.brev.NotatService
import no.nav.etterlatte.brev.NyNotatService
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.libs.ktor.route.withSakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class StrukturertNotat {
    @JsonTypeName("KLAGE_BLANKETT")
    data class KlageBlankett(
        val klage: Klage,
    ) : StrukturertNotat()
}

data class NotatRequest(
    val data: StrukturertNotat,
)

const val NOTAT_ID_CALL_PARAMETER = "notatId"

inline val PipelineContext<*, ApplicationCall>.notatId: NotatID
    get() =
        requireNotNull(call.parameters[NOTAT_ID_CALL_PARAMETER]?.toLong()) {
            "Gosys oppgaveId er ikke i path params"
        }

private val logger = LoggerFactory.getLogger("notatRoute")

fun Route.notatRoute(
    notatService: NotatService,
    nyNotatService: NyNotatService,
    tilgangsSjekk: Tilgangssjekker,
) {
    route("/notat") {
        route("/{$NOTAT_ID_CALL_PARAMETER}") {
            delete {
                nyNotatService.slett(notatId)
                call.respond(HttpStatusCode.OK)
            }

            get("/payload") {
                val payload = nyNotatService.hentPayload(notatId)
                call.respond(payload)
            }

            post("/payload") {
                val payload = call.receive<Slate>()

                nyNotatService.oppdaterPayload(notatId, payload, brukerTokenInfo)

                call.respond(HttpStatusCode.OK)
            }

            post("/tittel") {
                val tittel = call.request.queryParameters["tittel"]

                if (tittel.isNullOrBlank()) {
                    throw UgyldigForespoerselException(
                        code = "TITTEL_MANGLER",
                        detail = "Kan ikke oppdatere tittel når den er tom eller mangler!",
                    )
                } else {
                    nyNotatService.oppdaterTittel(notatId, tittel, brukerTokenInfo)
                    call.respond(HttpStatusCode.OK)
                }
            }

            get("/pdf") {
                val payload = nyNotatService.genererPdf(notatId)
                call.respond(payload)
            }

            post("/journalfoer") {
                nyNotatService.journalfoer(notatId, brukerTokenInfo)
                call.respond(HttpStatusCode.OK)
            }
        }

        route("/referanse/{referanse}") {
            get {
                val referanse = checkNotNull(call.parameters["referanse"])

                val notater = nyNotatService.hentForReferanse(referanse)
                call.respond(notater)
            }
        }

        route("/sak/{$SAKID_CALL_PARAMETER}") {
            get {
                withSakId(tilgangsSjekk, skrivetilgang = true) { sakId ->
                    val notater = nyNotatService.hentForSak(sakId)
                    call.respond(notater)
                }
            }

            post {
                withSakId(tilgangsSjekk, skrivetilgang = true) { sakId ->
                    val referanse = call.request.queryParameters["referanse"]
                    val mal = NotatMal.valueOf(call.request.queryParameters["mal"]!!)

                    val notat =
                        nyNotatService.opprett(
                            sakId = sakId,
                            referanse = referanse,
                            mal = mal,
                            bruker = brukerTokenInfo,
                        )

                    call.respond(notat)
                }
            }

            /*
             * Kun brukt av klage. Burde på sikt fjernes og flytte klage over på det generelle API-et over
             */
            post("/forhaandsvis") {
                withSakId(tilgangsSjekk, skrivetilgang = false) { sakId ->
                    logger.info("Forhåndsviser klageblankett i sak $sakId")
                    medBody<NotatRequest> {
                        call.respond(
                            notatService.genererPdf(
                                (it.data as StrukturertNotat.KlageBlankett),
                                brukerTokenInfo,
                            ),
                        )
                    }
                }
            }

            /*
             * Kun brukt av klage. Burde på sikt fjernes og flytte klage over på det generelle API-et over
             */
            post("/journalfoer") {
                withSakId(tilgangsSjekk, skrivetilgang = true) { sakId ->
                    logger.info("Journalfører klageblankett i sak $sakId")

                    medBody<NotatRequest> {
                        call.respond(
                            notatService.journalfoerNotatISak(
                                (it.data as StrukturertNotat.KlageBlankett),
                                brukerTokenInfo,
                            ),
                        )
                    }
                }
            }
        }
    }
}
