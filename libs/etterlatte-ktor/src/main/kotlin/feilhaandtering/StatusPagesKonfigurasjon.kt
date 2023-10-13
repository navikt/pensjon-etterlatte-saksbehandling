package no.nav.etterlatte.libs.ktor.feilhaandtering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.response.respond
import isProd
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
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
                    cause.respond(call)
                }

                is InternfeilException -> {
                    call.application.log.loggInternfeilException(cause)
                    cause.respond(call)
                }

                else -> {
                    val mappetFeil = InternfeilException("En feil har skjedd.", cause)
                    call.application.log.loggInternfeilException(mappetFeil)
                    mappetFeil.respond(call)
                }
            }
        }
        status(*statusCodes4xx) { call, code ->
            when (code) {
                HttpStatusCode.NotFound -> GenerellIkkeFunnetException().respond(call)

                HttpStatusCode.BadRequest ->
                    UgyldigForespoerselException(
                        "BAD_REQUEST",
                        "Forespørselen er ugyldig",
                    ).respond(call)

                HttpStatusCode.Forbidden ->
                    IkkeTillattException(
                        "FORBIDDEN",
                        "Forespørselen er ikke tillatt",
                    ).respond(call)

                else ->
                    ForespoerselException(
                        status = code.value,
                        code = "UNKNOWN_ERROR",
                        detail = "En ukjent feil oppstod",
                    )
                        .respond(call)
            }
        }

        status(*statusCodes5xx) { call, code ->
            // Maskerer alle interne feil som 500 internal server error
            val feil = InternfeilException("Ukjent feil")
            if (code !== HttpStatusCode.InternalServerError) {
                call.application.log.error(
                    "Et endepunkt returnerte en ikke-500 5xx-respons ($code). Maskerer som 500.",
                    feil,
                )
            } else {
                call.application.log.error("Et endepunkt returnerte en 500-respons", feil)
            }
            feil.respond(call)
        }
    }

    private fun Logger.loggInternfeilException(cause: InternfeilException) {
        if (cause.erDeserialiseringsException() && isProd()) {
            sikkerLogg.error("En feil har oppstått ved deserialisering", cause)
            this.error(
                "En feil har oppstått ved deserialisering. Se sikkerlogg for mer detaljer.",
            )
        } else {
            this.error("En intern feil oppstod i et endepunkt. Svarer frontend med 500-feil", cause)
        }
    }

    private fun Logger.loggForespoerselException(cause: ForespoerselException) {
        if (cause.erDeserialiseringsException() && isProd()) {
            sikkerLogg.info("En feil har oppstått ved deserialisering", cause)
            this.info(
                "En feil har oppstått ved deserialisering i et endepunkt. Se sikkerlogg for mer detaljer. " +
                    "Feilen fikk status ${cause.status} til frontend.",
            )
        }
        // Logger ikke meta, siden det kan inneholde identiferende informasjon
        this.info("En forespørselsfeil oppstod i et endepunkt", cause.noMeta())
    }

    private suspend fun ForespoerselException.respond(call: ApplicationCall) {
        call.respond(
            HttpStatusCode.fromValue(this.status),
            this.somExceptionResponse(),
        )
    }

    private suspend fun InternfeilException.respond(call: ApplicationCall) {
        call.respond(
            HttpStatusCode.InternalServerError,
            this.somJsonRespons(),
        )
    }
}
