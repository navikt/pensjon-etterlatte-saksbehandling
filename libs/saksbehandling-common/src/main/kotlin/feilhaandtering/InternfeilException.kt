package no.nav.etterlatte.libs.common.feilhaandtering

open class InternfeilLoggerException(
    open val status: Int,
    open val code: String,
    open val detail: String,
    override val cause: Throwable? = null,
) : Exception(detail, cause)

open class InternfeilException(
    open val detail: String,
    override val cause: Throwable? = null,
) : Exception(detail, cause) {
    fun somJsonRespons(): ExceptionResponse =
        ExceptionResponse(
            status = 500,
            detail = detail,
            code = "INTERNAL_SERVER_ERROR",
            meta = null,
        )
}
