package no.nav.etterlatte.brev.virusskanning

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import java.util.Base64

class ClamAvClient(
    private val httpClient: HttpClient,
    private val endpointUrl: String,
) {
    suspend fun virusScanVedlegg(vedleggList: List<Vedlegg>): List<ScanResult> {
        val httpResponse =
            httpClient.submitFormWithBinaryData(
                url = "$endpointUrl/scan",
                formData =
                    formData {
                        vedleggList.forEachIndexed { index, vedlegg ->
                            append(
                                "file$index",
                                Base64.getMimeDecoder().decode(vedlegg.content.content),
                                Headers.build {
                                    append(HttpHeaders.ContentType, vedlegg.content.contentType)
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "filename=${removeNewLines(vedlegg.description)}",
                                    )
                                },
                            )
                        }
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

fun removeNewLines(description: String): String {
    return description.replace("\n", "")
}
