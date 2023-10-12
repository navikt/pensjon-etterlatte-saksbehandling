package no.nav.etterlatte.libs.ktor.feilhaandtering

import io.ktor.http.HttpStatusCode

open class InternfeilException(
    open val detail: String,
    override val cause: Throwable? = null,
) : Exception(detail, cause) {
    fun somJsonRespons(): ExceptionResponse {
        return ExceptionResponse(
            status = HttpStatusCode.InternalServerError.value,
            detail = detail,
            code = "INTERNAL_SERVER_ERROR",
            meta = null,
        )
    }
}
