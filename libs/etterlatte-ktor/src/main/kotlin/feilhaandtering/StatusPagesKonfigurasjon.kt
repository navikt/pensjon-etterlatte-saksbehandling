package no.nav.etterlatte.libs.ktor.feilhaandtering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilLoggerException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.isProd
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.erDeserialiseringsException
import no.nav.etterlatte.libs.ktor.feilhaandtering.EscapeUtils.escape
import no.nav.etterlatte.libs.ktor.skjulAllePotensielleFnr
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val routeLogger = LoggerFactory.getLogger("Route")

class StatusPagesKonfigurasjon(
    private val sikkerLogg: Logger,
) {
    private val statusCodes4xx = HttpStatusCode.allStatusCodes.filter { it.value in 400..499 }.toTypedArray()
    private val statusCodes5xx = HttpStatusCode.allStatusCodes.filter { it.value in 500..599 }.toTypedArray()

    val config: StatusPagesConfig.() -> Unit = {
        exception<Throwable> { call, cause ->
            when (cause) {
                is ForespoerselException -> {
                    call.application.log.loggForespoerselException(cause, call)
                    call.respond(cause)
                }

                is InternfeilException -> {
                    call.application.log.loggInternfeilException(cause, call)
                    call.respond(cause)
                }

                is NotFoundException -> {
                    val wrapped =
                        IkkeFunnetException(
                            code = "NOT_FOUND",
                            detail = cause.message ?: "Fant ikke ressursen",
                            cause = cause,
                        )
                    call.application.log.loggForespoerselException(wrapped, call)
                    call.respond(wrapped)
                }

                else -> {
                    val mappetFeil =
                        UkjentInternfeilException(
                            "En feil har skjedd: ${cause::class.java.canonicalName} Tidspunkt: ${Tidspunkt.now()}. Korrelasjonsid: ${getCorrelationId()}",
                            cause,
                        )
                    call.application.log.loggInternfeilException(mappetFeil, call)
                    call.respond(
                        UkjentInternfeilException(
                            "En ukjent feil har skjedd. Tidspunkt: ${Tidspunkt.now()}. Korrelasjonsid: ${getCorrelationId()}",
                            mappetFeil,
                        ),
                    )
                }
            }
        }

        status(*statusCodes4xx) { call, code ->
            routeLogger.debug("Fikk kode {}", code)
            when (code) {
                HttpStatusCode.NotFound -> {
                    call.application.log.warn(
                        "Forespurt URI (${skjulAllePotensielleFnr(call.request.uri)}) " +
                            "returnerte kode 404, ruten er enten ikke satt opp eller den returnerer 404 direkte.",
                    )

                    call.respond(
                        IkkeFunnetException(
                            code = "ROUTE_NOT_CONFIGURED",
                            detail = "Du ba om en rute / url som ikke er konfigurert opp. Dobbeltsjekk forespørselen.",
                        ),
                    )
                }

                HttpStatusCode.BadRequest ->
                    call.respond(
                        UgyldigForespoerselException(
                            code = "BAD_REQUEST",
                            detail = "Forespørselen er ugyldig",
                        ),
                    )

                HttpStatusCode.Unauthorized ->
                    call.respond(
                        ForespoerselException(
                            status = 401,
                            code = "UNAUTHORIZED",
                            detail = "Forespørselen er ikke autentisert",
                        ),
                    )

                HttpStatusCode.Forbidden ->
                    call.respond(
                        IkkeTillattException(
                            code = "FORBIDDEN",
                            detail = "Forespørselen er ikke tillatt",
                        ),
                    )

                else ->
                    call.respond(
                        ForespoerselException(
                            status = code.value,
                            code = "UNKNOWN_ERROR",
                            detail = "En ukjent feil oppstod",
                        ),
                    )
            }
        }

        status(*statusCodes5xx) { call, code ->
            routeLogger.debug("Fikk kode {}", code)
            if (code == HttpStatusCode.ServiceUnavailable) {
                routeLogger.warn("Et endepunkt returnerte Service Unavailable. Det er sannsynligvis fra R&R")
                return@status call.respond(
                    ForespoerselException(
                        status = code.value,
                        code = code.description,
                        detail = "Tjenesten er ikke tilgjengelig",
                    ),
                )
            }

            val feil =
                InternfeilLoggerException(
                    status = code.value,
                    detail = call.request.uri,
                    code = "5XX_FEIL",
                )

            if (code !== HttpStatusCode.InternalServerError) {
                call.application.log.error("Et endepunkt returnerte $code. Maskerer som 500: ", feil)
            } else {
                call.application.log.error("Et endepunkt returnerte $code: ", feil)
            }

            // Maskerer alle interne feil som 500 internal server error
            call.respond(InternfeilException("Ukjent feil"))
        }
    }

    private suspend fun Logger.loggInternfeilException(
        internfeil: InternfeilException,
        call: ApplicationCall,
    ) {
        if (internfeil.erDeserialiseringsException() && isProd()) {
            sikkerLogg.error(
                "En feil har oppstått ved deserialisering. Requestobjektet var {}",
                escape(hentRequestobjekt(call)),
                internfeil,
            )
            this.error(
                "En feil har oppstått ved deserialisering. Se sikkerlogg for mer detaljer.",
            )
        } else if (internfeil is UkjentInternfeilException) {
            this.error(
                internfeil.cause.message ?: internfeil.detail,
                internfeil.cause,
            )
        } else {
            this.error(internfeil.detail, internfeil)
        }
    }

    private suspend fun Logger.loggForespoerselException(
        internfeil: ForespoerselException,
        call: ApplicationCall,
    ) {
        if (internfeil.erDeserialiseringsException() && isProd()) {
            sikkerLogg.info(
                "En feil har oppstått ved deserialisering. Requestobjektet var {}",
                escape(hentRequestobjekt(call)),
                internfeil,
            )
            this.info(
                "En feil har oppstått ved deserialisering i et endepunkt. Se sikkerlogg for mer detaljer. " +
                    "Feilen fikk status ${internfeil.status} til frontend.",
            )
        }

        this.info(
            "En forespørselsfeil oppstod i et endepunkt, detaljer: ${internfeil.detail}",
            internfeil.cause ?: internfeil,
        )
    }

    private suspend fun ApplicationCall.respond(feil: ForespoerselException) {
        this.respond(
            HttpStatusCode.fromValue(feil.status),
            feil.somExceptionResponse(),
        )
    }

    private suspend fun ApplicationCall.respond(feil: InternfeilException) {
        this.respond(
            HttpStatusCode.InternalServerError,
            feil.somJsonRespons(),
        )
    }

    private suspend fun hentRequestobjekt(call: ApplicationCall): String {
        val requestobjekt =
            try {
                call.receive<String>()
            } catch (_: Exception) {
                "Kunne ikke hente requestobjektet"
            }
        sikkerLogg.debug("Henta requestobjekt på totalt ${requestobjekt.length} tegn")
        return requestobjekt
    }
}

private class UkjentInternfeilException(
    override val detail: String,
    override val cause: Throwable,
) : InternfeilException(detail, cause)
