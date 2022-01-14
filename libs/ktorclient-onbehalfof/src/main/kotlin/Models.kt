package no.nav.etterlatte.libs.ktorobo

data class Resource(
    val clientId: String,
    val url: String,
    val response: Any? = null
) {
    fun addResponse(response: Any?): Resource = this.copy(response = response)
}

data class ThrowableErrorMessage(
    val message: String,
    val throwable: Throwable
) {
    fun toErrorResponse() = ErrorResponse(message, throwable.toString())
}

data class ErrorResponse(
    val message: String,
    val cause: String
)