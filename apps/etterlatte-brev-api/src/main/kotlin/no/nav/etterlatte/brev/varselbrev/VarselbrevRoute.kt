package no.nav.etterlatte.brev.varselbrev

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.BREV_ID_CALL_PARAMETER
import no.nav.etterlatte.brev.brevId
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.route.withSakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

internal fun Route.varselbrevRoute(
    service: VarselbrevService,
    tilgangssjekker: Tilgangssjekker,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    route("brev/behandling/{$BEHANDLINGID_CALL_PARAMETER}/varsel") {
        get {
            withBehandlingId(tilgangssjekker) {
                measureTimedValue {
                    service.hentVarselbrev(behandlingId)
                }.let { (brev, varighet) ->
                    logger.info("Henting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    if (brev.isNotEmpty()) {
                        call.respond(brev.maxBy { it.opprettet })
                    } else {
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }

        post {
            withBehandlingId(tilgangssjekker, skrivetilgang = true) { behandlingId ->
                val sakId = sakId

                logger.info("Oppretter varselbrev for behandling (sakId=$sakId, behandlingId=$behandlingId)")

                measureTimedValue {
                    service.opprettVarselbrev(sakId, behandlingId, brukerTokenInfo)
                }.let { (brev, varighet) ->
                    logger.info("Oppretting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(HttpStatusCode.Created, brev)
                }
            }
        }

        get("/pdf") {
            withBehandlingId(tilgangssjekker) {
                logger.info("Genererer PDF for varselbrev (id=$brevId)")

                measureTimedValue {
                    service.genererPdfFerdigstilling(brevId, brukerTokenInfo).bytes
                }.let { (pdf, varighet) ->
                    logger.info("Generering av pdf tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(pdf)
                }
            }
        }
    }

    route("brev/{$BREV_ID_CALL_PARAMETER}/varsel") {
        post("/ferdigstill") {
            withSakId(tilgangssjekker, skrivetilgang = true) {
                logger.info("Genererer og ferdigstiller PDF for varselbrev (id=$brevId)")

                measureTimedValue {
                    service.ferdigstillOgGenererPDF(brevId, brukerTokenInfo)
                }.let { (_, varighet) ->
                    logger.info("Ferdigstilling samt generering av pdf tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
