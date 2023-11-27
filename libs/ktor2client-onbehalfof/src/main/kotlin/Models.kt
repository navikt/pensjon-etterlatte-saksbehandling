package no.nav.etterlatte.libs.ktorobo

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
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
    override val cause: Throwable? = null,
    val response: HttpResponse? = null,
) : InternfeilException(message, cause)

internal fun Throwable.toErr(url: String): Result<JsonNode, ThrowableErrorMessage> {
    return Err(
        ThrowableErrorMessage(
            message = "An unexpected error occured when calling $url",
            cause = this,
        ),
    )
}

internal suspend fun HttpResponse.toErr(): Result<JsonNode, ThrowableErrorMessage> {
    return Err(
        ThrowableErrorMessage(
            message = "Received response with status ${status.value} from downstream api with error: ${this.body<String>()}",
            response = this,
        ),
    )
}
