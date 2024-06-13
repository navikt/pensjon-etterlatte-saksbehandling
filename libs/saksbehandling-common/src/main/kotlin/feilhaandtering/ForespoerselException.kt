package no.nav.etterlatte.libs.common.feilhaandtering

open class ForespoerselException(
    open val status: Int,
    open val code: String,
    open val detail: String,
    open val meta: Map<String, Any>? = null,
    override val cause: Throwable? = null,
) : Exception(detail, cause) {
    fun somExceptionResponse(): ExceptionResponse =
        ExceptionResponse(
            status = status,
            detail = detail,
            code = code,
            meta = meta,
        )
}

open class UgyldigForespoerselException(
    override val code: String,
    override val detail: String,
    override val meta: Map<String, Any>? = null,
    override val cause: Throwable? = null,
) : ForespoerselException(status = 400, code = code, detail = detail, meta = meta, cause = cause)

open class IkkeFunnetException(
    override val code: String,
    override val detail: String,
    override val meta: Map<String, Any>? = null,
    override val cause: Throwable? = null,
) : ForespoerselException(status = 404, code = code, detail = detail, meta = meta, cause = cause)

/**
 * Brukes felles alle steder der vi ikke vil lekke noe informasjon om vi faktisk har funnet noe eller ikke ref.
 * informasjonssikring
 */
class GenerellIkkeFunnetException : IkkeFunnetException(code = "NOT_FOUND", detail = "Kunne ikke finne ønsket ressurs")

open class IkkeTillattException(
    override val code: String,
    override val detail: String,
    override val meta: Map<String, Any>? = null,
    override val cause: Throwable? = null,
) : ForespoerselException(status = 403, code = code, detail = detail, meta = meta, cause = cause)
