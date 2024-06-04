package no.nav.etterlatte.brev.notat

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.brev.model.Slate
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.UUID

/**
 * Klient mot ey-pdfgen
 * Brukes i tilfeller hvor vi skal generere PDF-er som ikke er et brev.
 * Dette fordi Brevbakeren er veldig låst til "brev-format", derav navnet.
 *
 * @see: <a href="https://github.com/navikt/pensjon-etterlatte-felles/tree/main/apps/ey-pdfgen">ey-pdfgen</a>
 *
 * @see: <a href="https://github.com/navikt/pensjonsbrev">Pensjonsbrev - Brevbaker</a>
 **/
class PdfGeneratorKlient(private val klient: HttpClient, private val apiUrl: String) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun genererPdf(request: PdfGenRequest): ByteArray {
        logger.info("Genererer PDF med ey-pdfgen")

        return klient.post("$apiUrl/notat/tom_mal") {
            header("X-Correlation-ID", MDC.get("X-Correlation-ID") ?: UUID.randomUUID().toString())
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}

data class PdfGenRequest(
    val tittel: String,
    val payload: Slate,
)
