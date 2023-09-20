package no.nav.etterlatte.libs.ktorobo

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

data class Resource(
    val clientId: String,
    val url: String,
    val response: Any? = null,
) {
    fun addResponse(response: Any?): Resource = this.copy(response = response)
}

data class ThrowableErrorMessage(
    override val message: String,
    val throwable: Throwable,
    val downstreamStatusCode: HttpStatusCode? = null,
) : Exception(message, throwable)

class HttpStatusRuntimeException(val downstreamStatusCode: HttpStatusCode, override val message: String) :
    RuntimeException(message)

internal fun Throwable.toErr(url: String): Result<JsonNode, ThrowableErrorMessage> {
    val downstreamStatusCode =
        when (this) {
            is HttpStatusRuntimeException -> this.downstreamStatusCode
            else -> null
        }
    return Err(
        ThrowableErrorMessage(
            message = "Error response from '$url'",
            throwable = this,
            downstreamStatusCode = downstreamStatusCode,
        ),
    )
}

internal fun HttpResponse.checkForError() =
    if (!this.status.isSuccess()) {
        throw HttpStatusRuntimeException(
            this.status,
            "received response with status ${this.status.value} from downstream api",
        )
    } else {
        this
    }
