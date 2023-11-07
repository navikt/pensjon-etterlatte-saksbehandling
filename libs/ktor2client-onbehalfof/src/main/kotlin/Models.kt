package no.nav.etterlatte.libs.ktorobo

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException

data class Resource(
    val clientId: String,
    val url: String,
    val additionalHeaders: Map<String, String>? = null,
    val response: Any? = null,
) {
    fun addResponse(response: Any?): Resource = this.copy(response = response)
}

data class ThrowableErrorMessage(
    override val message: String,
    val throwable: Throwable,
) : Exception(message, throwable)

class HttpStatusRuntimeException(override val message: String) :
    InternfeilException(message)

internal fun Throwable.toErr(url: String): Result<JsonNode, ThrowableErrorMessage> {
    return Err(
        ThrowableErrorMessage(
            message = "Error response from '$url'",
            throwable = this,
        ),
    )
}

internal suspend fun HttpResponse.checkForError() =
    if (this.status.isSuccess()) {
        this
    } else {
        throw HttpStatusRuntimeException(
            "Received response with status ${this.status.value} from downstream api with error: ${this.body<String>()}",
        )
    }
