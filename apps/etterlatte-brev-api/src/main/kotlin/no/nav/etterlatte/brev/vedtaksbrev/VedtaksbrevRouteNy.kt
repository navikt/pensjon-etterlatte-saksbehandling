package no.nav.etterlatte.brev.vedtaksbrev

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.BrevRequest
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory

const val BREV_TYPE_CALL_PARAMETER = "brevType"
const val BREV_ID_CALL_PARAMETER = "brevId"

fun Route.strukturertBrevRoute(
    service: StrukturertBrevService,
    tilgangssjekker: Tilgangssjekker,
) {
    val logger = LoggerFactory.getLogger("no.nav.etterlatte.brev.strukturertBrevRoute")

    // TODO slett denne routen
    route("brev/vedtak/{$BEHANDLINGID_CALL_PARAMETER}") {
        route("vedtak") {
            post {
                withBehandlingId(tilgangssjekker, skrivetilgang = true) { behandlingId ->
                    val request = call.receive<BrevRequest>()
                    logger.info("Oppretter vedtaksbrev for tilbakekreving behandling (behandlingId=$behandlingId)")
                    val brev = service.opprettStrukturertBrev(behandlingId, brukerTokenInfo, request)
                    call.respond(HttpStatusCode.Created, brev)
                }
            }

            post("pdf") {
                withBehandlingId(tilgangssjekker) {
                    val brevId =
                        krevIkkeNull(call.request.queryParameters[BREV_ID_CALL_PARAMETER]?.toLong()) {
                            "Kan ikke generere PDF uten brevId"
                        }
                    val request = call.receive<BrevRequest>()

                    logger.info("Genererer PDF for tilbakekreving vedtaksbrev (id=$brevId)")
                    val pdf = service.genererEllerHentPdf(brevId, brukerTokenInfo, request)
                    call.respond(pdf)
                }
            }

            post("ferdigstill") {
                withBehandlingId(tilgangssjekker, skrivetilgang = true) { behandlingId ->
                    logger.info("Ferdigstiller vedtaksbrev for behandling (id=$behandlingId)")
                    val brevType =
                        krevIkkeNull(call.request.queryParameters[BREV_TYPE_CALL_PARAMETER]?.let { enumValueOf<Brevtype>(it) }) {
                            "Mangler brevtype-parameter"
                        }
                    service.ferdigstillStrukturertBrev(behandlingId, brevType, brukerTokenInfo)
                    call.respond(HttpStatusCode.OK)
                }
            }

            put("tilbakestill") {
                withBehandlingId(tilgangssjekker, skrivetilgang = true) {
                    val brevId =
                        krevIkkeNull(call.request.queryParameters[BREV_ID_CALL_PARAMETER]?.toLong()) {
                            "Kan ikke tilbakestille PDF uten brevId"
                        }
                    val brevRequest = call.receive<BrevRequest>()

                    logger.info("Tilbakestiller payload for vedtaksbrev (id=$brevId)")

                    val paylod = service.tilbakestillStrukturertBrev(brevId, brukerTokenInfo, brevRequest)
                    call.respond(paylod)
                }
            }
        }
    }

    route("brev/strukturert/{$BEHANDLINGID_CALL_PARAMETER}") {
        post {
            withBehandlingId(tilgangssjekker, skrivetilgang = true) { behandlingId ->
                val request = call.receive<BrevRequest>()
                logger.info("Oppretter vedtaksbrev for tilbakekreving behandling (behandlingId=$behandlingId)")
                val brev = service.opprettStrukturertBrev(behandlingId, brukerTokenInfo, request)
                call.respond(HttpStatusCode.Created, brev)
            }
        }

        post("pdf") {
            withBehandlingId(tilgangssjekker) {
                val brevId =
                    krevIkkeNull(call.request.queryParameters[BREV_ID_CALL_PARAMETER]?.toLong()) {
                        "Kan ikke generere PDF uten brevId"
                    }
                val request = call.receive<BrevRequest>()

                logger.info("Genererer PDF for tilbakekreving vedtaksbrev (id=$brevId)")
                val pdf = service.genererEllerHentPdf(brevId, brukerTokenInfo, request)
                call.respond(pdf)
            }
        }

        post("ferdigstill") {
            withBehandlingId(tilgangssjekker, skrivetilgang = true) { behandlingId ->
                logger.info("Ferdigstiller strukturert brev for behandling (id=$behandlingId)")
                val brevType =
                    krevIkkeNull(call.request.queryParameters[BREV_TYPE_CALL_PARAMETER]?.let { enumValueOf<Brevtype>(it) }) {
                        "Mangler brevtype-parameter"
                    }
                service.ferdigstillStrukturertBrev(behandlingId, brevType, brukerTokenInfo)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("ferdigstill-journalfoer-distribuer") {
            withBehandlingId(tilgangssjekker, skrivetilgang = true) { behandlingId ->
                logger.info("Ferdigstiller, Journalfoerer og distribuerer strukturert brev for behandling (id=$behandlingId)")
                val brevType =
                    krevIkkeNull(call.request.queryParameters[BREV_TYPE_CALL_PARAMETER]?.let { enumValueOf<Brevtype>(it) }) {
                        "Mangler brevtype-parameter"
                    }
                service.ferdigstillJournalfoerOgDistribuerStrukturertBrev(behandlingId, brevType, brukerTokenInfo)
                call.respond(HttpStatusCode.OK)
            }
        }

        put("tilbakestill") {
            withBehandlingId(tilgangssjekker, skrivetilgang = true) {
                val brevId =
                    krevIkkeNull(call.request.queryParameters[BREV_ID_CALL_PARAMETER]?.toLong()) {
                        "Kan ikke tilbakestille PDF uten brevId"
                    }
                val brevRequest = call.receive<BrevRequest>()

                logger.info("Tilbakestiller payload for vedtaksbrev (id=$brevId)")

                val paylod = service.tilbakestillStrukturertBrev(brevId, brukerTokenInfo, brevRequest)
                call.respond(paylod)
            }
        }
    }
}
