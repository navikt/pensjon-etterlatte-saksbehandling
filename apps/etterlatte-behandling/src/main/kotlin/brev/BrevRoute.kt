package no.nav.etterlatte.brev

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

/**
 * Behandling i denne konteksten er en behandling med et strukturert brev, altså tilbakekreving, klage,
 * forbehandling etteroppgjør, eller behandling
 */
fun Route.brevRoute(service: BrevService) {
    val logger = LoggerFactory.getLogger("BrevRoute")

    route("api/behandling/brev/{$BEHANDLINGID_CALL_PARAMETER}") {
        get {
            logger.info("Henter strukturert brev for behandling (behandlingId=$behandlingId)")

            val brev =
                measureTimedValue {
                    inTransaction {
                        runBlocking {
                            service.hentStrukturertBrev(behandlingId, brukerTokenInfo)
                        }
                    }
                }.let { (brev, varighet) ->
                    logger.info("Henting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    brev
                }
            when (brev) {
                null -> call.respond(HttpStatusCode.NoContent)
                else -> call.respond(brev)
            }
        }

        post {
            kunSkrivetilgang {
                val brev =
                    inTransaction {
                        logger.info("Oppretter strukturert brev for behandling (sakId=$sakId, behandlingId=$behandlingId)")

                        measureTimedValue {
                            runBlocking {
                                service.opprettStrukturertBrev(behandlingId, sakId, brukerTokenInfo)
                            }
                        }.let { (brev, varighet) ->
                            logger.info("Oppretting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                            brev
                        }
                    }
                call.respond(HttpStatusCode.Created, brev)
            }
        }

        get("pdf") {
            kunSkrivetilgang {
                val pdf =
                    inTransaction {
                        val brevId =
                            krevIkkeNull(call.request.queryParameters["brevId"]?.toLong()) {
                                "Kan ikke generere PDF uten brevId"
                            }
                        logger.info("Genererer PDF for strukturert brev (id=$brevId)")

                        measureTimedValue {
                            runBlocking {
                                service.genererPdf(brevId, behandlingId, sakId, brukerTokenInfo).bytes
                            }
                        }.let { (pdf, varighet) ->
                            logger.info("Generering av pdf tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                            pdf
                        }
                    }
                call.respondBytes(pdf, contentType = ContentType.Application.Pdf)
            }
        }

        post("ferdigstill") {
            kunSkrivetilgang {
                inTransaction {
                    logger.info("Ferdigstiller strukturert brev for behandling (id=$behandlingId)")
                    measureTimedValue {
                        runBlocking {
                            service.ferdigstillStrukturertBrev(behandlingId, brukerTokenInfo)
                        }
                    }.also { (_, varighet) ->
                        logger.info("Ferdigstilling av strukturert brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    }
                }
                call.respond(HttpStatusCode.OK)
            }
        }

        put("tilbakestill") {
            kunSkrivetilgang {
                val brevPayload =
                    inTransaction {
                        val brevId =
                            krevIkkeNull(call.request.queryParameters["brevId"]?.toLong()) {
                                "Kan ikke tilbakestille PDF uten brevId"
                            }
                        val brevType =
                            krevIkkeNull(call.request.queryParameters["brevType"]) {
                                "Kan ikke tilbakestille PDF uten brevType"
                            }.let { Brevtype.valueOf(it) }

                        logger.info("Tilbakestiller payload for strukturert brev (id=$brevId)")

                        measureTimedValue {
                            runBlocking {
                                service.tilbakestillStrukturertBrev(brevId, behandlingId, sakId, brevType, brukerTokenInfo)
                            }
                        }.let { (brevPayload, varighet) ->
                            logger.info(
                                "Oppretting av nytt innhold til brev (id=$brevId) tok ${
                                    varighet.toString(
                                        DurationUnit.SECONDS,
                                        2,
                                    )
                                }",
                            )
                            brevPayload
                        }
                    }
                call.respond(brevPayload)
            }
        }
    }
}
