package no.nav.etterlatte.libs.common.feilhaandtering

data class ExceptionResponse(
    val status: Int,
    val detail: String,
    val code: String?,
    val meta: Map<String, Any>?,
)
