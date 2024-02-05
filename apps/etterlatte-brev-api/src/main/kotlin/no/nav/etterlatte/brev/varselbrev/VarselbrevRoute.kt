package no.nav.etterlatte.brev.varselbrev

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.hentinformasjon.Tilgangssjekker
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
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
                service.hentVarselbrev(behandlingId)
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
    }
}
