package no.nav.etterlatte.brev.virusskanning

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import org.slf4j.LoggerFactory

class ClamAvClient(
    val httpClient: HttpClient,
    val endpointUrl: String,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun skann(request: ClamAVRequest): List<ScanResult> {
        val httpResponse =
            httpClient.submitFormWithBinaryData(
                url = "$endpointUrl/scan",
                formData =
                    formData {
                        append(
                            request.filnavn,
                            request.fil,
                            Headers.build {
                                append(HttpHeaders.ContentType, ContentType.Application.Pdf.contentType)
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "filename=${removeNewLines(request.filnavn)}",
                                )
                            },
                        )
                    },
            )
        logger.info("Status fra ClamAV: ${httpResponse.status}")
        return httpResponse.body<List<ScanResult>>()
    }
}

data class ClamAVRequest(
    val filnavn: String,
    val fil: ByteArray,
)

data class ScanResult(
    val Filename: String,
    val Result: Status,
)

enum class Status {
    FOUND,
    OK,
    ERROR,
}

private fun removeNewLines(description: String): String = description.replace("\n", "")
