package no.nav.etterlatte.libs.ktor.feilhaandtering

import io.ktor.http.HttpStatusCode

open class ForespoerselException(
    open val status: Int,
    open val code: String,
    open val detail: String,
    open val meta: Map<String, Any>? = null,
    override val cause: Throwable? = null,
) : Exception(detail, cause) {
    fun noMeta(): ForespoerselException {
        return ForespoerselException(status = status, code = code, detail = detail, cause = cause)
    }

    fun somExceptionResponse(): ExceptionResponse {
        return ExceptionResponse(
            status = status,
            detail = detail,
            code = code,
            meta = meta,
        )
    }
}

open class UgyldigForespoerselException(
    override val code: String,
    override val detail: String,
    override val meta: Map<String, Any>? = null,
    override val cause: Throwable? = null,
) : ForespoerselException(HttpStatusCode.BadRequest.value, code = code, detail = detail, meta = meta, cause = cause)

open class IkkeFunnetException(
    override val code: String,
    override val detail: String,
    override val meta: Map<String, Any>? = null,
    override val cause: Throwable? = null,
) : ForespoerselException(status = HttpStatusCode.NotFound.value, code = code, detail = detail, meta = meta, cause = cause)

open class IkkeTillattException(
    override val code: String,
    override val detail: String,
    override val meta: Map<String, Any>? = null,
    override val cause: Throwable? = null,
) : ForespoerselException(status = HttpStatusCode.Forbidden.value, code = code, detail = detail, meta = meta, cause = cause)
