package no.nav.etterlatte.libs.ktor

import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class PdfGenereringException(
    val kanProevesIgjen: Boolean,
    message: String,
) : RuntimeException(message)

fun HttpRequestBuilder.krevPdfSuksess() {
    // Kontrollerer responsen selv, også når klienten ellers har expectSuccess=true.
    expectSuccess = false
}

suspend fun HttpResponse.pdfBytes(): ByteArray {
    if (!status.isSuccess()) {
        throw PdfGenereringException(
            kanProevesIgjen = status.value == 408 || status.value == 429 || status.value >= 500,
            message = "PDF-generering feilet med HTTP ${status.value}",
        )
    }
    if (contentType()?.withoutParameters() != ContentType.Application.Pdf) {
        throw PdfGenereringException(false, "PDF-generering returnerte ikke application/pdf")
    }
    val bytes = body<ByteArray>()
    if (!bytes.take(5).toByteArray().contentEquals("%PDF-".toByteArray(Charsets.US_ASCII))) {
        throw PdfGenereringException(false, "PDF-generering returnerte ugyldig PDF-innhold")
    }
    return bytes
}
