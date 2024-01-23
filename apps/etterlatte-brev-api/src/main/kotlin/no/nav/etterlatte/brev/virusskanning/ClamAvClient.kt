package no.nav.etterlatte.brev.virusskanning

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class ClamAvClient(
    private val httpClient: HttpClient,
    private val endpointUrl: String,
) {
    suspend fun virusScanVedlegg(request: VirusScanRequest): List<ScanResult> {
        val httpResponse =
            httpClient.submitFormWithBinaryData(
                url = "$endpointUrl/scan",
                formData =
                    formData {
                        append(
                            request.filnavn(),
                            request.fil,
                            Headers.build {
                                append(HttpHeaders.ContentType, ContentType.Application.Pdf.contentType)
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "filename=${removeNewLines(request.tittel())}",
                                )
                            },
                        )
                    },
            )
        return httpResponse.body<List<ScanResult>>()
    }
}

data class ScanResult(
    val Filename: String,
    val Result: Status,
)

enum class Status {
    FOUND,
    OK,
    ERROR,
}

private fun removeNewLines(description: String): String {
    return description.replace("\n", "")
}
