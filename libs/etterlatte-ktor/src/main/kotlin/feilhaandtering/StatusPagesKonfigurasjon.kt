package no.nav.etterlatte.libs.ktor.feilhaandtering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import isProd
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.ktor.erDeserialiseringsException
import org.slf4j.Logger

class StatusPagesKonfigurasjon(private val sikkerLogg: Logger) {
    private val statusCodes4xx = HttpStatusCode.allStatusCodes.filter { it.value in 400..499 }.toTypedArray()
    private val statusCodes5xx = HttpStatusCode.allStatusCodes.filter { it.value in 500..599 }.toTypedArray()

    val config: StatusPagesConfig.() -> Unit = {
        exception<Throwable> { call, cause ->
            when (cause) {
                is ForespoerselException -> {
                    call.application.log.loggForespoerselException(cause)
                    call.respond(cause)
                }

                is InternfeilException -> {
                    call.application.log.loggInternfeilException(cause)
                    call.respond(cause)
                }

                is NotFoundException -> {
                    val wrapped =
                        IkkeFunnetException(
                            code = "NOT_FOUND",
                            detail = cause.message ?: "Fant ikke ressursen",
                            cause = cause,
                        )
                    call.application.log.loggForespoerselException(wrapped)
                    call.respond(wrapped)
                }

                else -> {
                    val mappetFeil = InternfeilException("En feil har skjedd.", cause)
                    call.application.log.loggInternfeilException(mappetFeil)
                    call.respond(mappetFeil)
                }
            }
        }

        status(*statusCodes4xx) { call, code ->
            when (code) {
                HttpStatusCode.NotFound -> call.respond(GenerellIkkeFunnetException())

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
            val feil =
                ForespoerselException(
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

    private fun Logger.loggInternfeilException(internfeil: InternfeilException) {
        if (internfeil.erDeserialiseringsException() && isProd()) {
            sikkerLogg.error("En feil har oppstått ved deserialisering", internfeil)
            this.error(
                "En feil har oppstått ved deserialisering. Se sikkerlogg for mer detaljer.",
            )
        } else {
            this.error(
                "En intern feil oppstod i et endepunkt. Svarer frontend med 500-feil",
                internfeil.cause ?: internfeil,
            )
        }
    }

    private fun Logger.loggForespoerselException(internfeil: ForespoerselException) {
        if (internfeil.erDeserialiseringsException() && isProd()) {
            sikkerLogg.info("En feil har oppstått ved deserialisering", internfeil)
            this.info(
                "En feil har oppstått ved deserialisering i et endepunkt. Se sikkerlogg for mer detaljer. " +
                    "Feilen fikk status ${internfeil.status} til frontend.",
            )
        }

        // Logger ikke meta, siden det kan inneholde identiferende informasjon
        this.info("En forespørselsfeil oppstod i et endepunkt, detaljer: ${internfeil.detail}", internfeil.cause ?: internfeil.noMeta())
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
}
