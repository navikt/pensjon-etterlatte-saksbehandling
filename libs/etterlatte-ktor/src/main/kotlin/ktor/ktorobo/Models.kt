package no.nav.etterlatte.libs.ktor.ktor.ktorobo

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
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

internal fun Throwable.toErr(url: String): Result<JsonNode, Throwable> =
    if (this is ResponseException) {
        Err(this)
    } else {
        Err(
            ThrowableErrorMessage(
                message = "An unexpected error occured when calling $url",
                cause = this,
            ),
        )
    }

internal suspend fun HttpResponse.toResponseException(): Result<JsonNode, ResponseException> =
    Err(
        ResponseException(
            this,
            "Received response with status ${status.value} from downstream api with error: ${this.body<String>()}",
        ),
    )
