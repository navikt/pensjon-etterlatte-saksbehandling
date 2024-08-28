package no.nav.etterlatte.brev.pdfgen

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.brev.notat.NotatMal
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PDFMal
import no.nav.etterlatte.libs.common.logging.CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.toJsonNode
import org.slf4j.LoggerFactory

/**
 * Klient mot ey-pdfgen
 * Brukes i tilfeller hvor vi skal generere PDF-er som ikke er et brev.
 * Dette fordi Brevbakeren er veldig låst til "brev-format", derav navnet.
 *
 * @see: <a href="https://github.com/navikt/pensjon-etterlatte-felles/tree/main/apps/ey-pdfgen">ey-pdfgen</a>
 *
 * @see: <a href="https://github.com/navikt/pensjonsbrev">Pensjonsbrev - Brevbaker</a>
 **/
class PdfGeneratorKlient(
    private val klient: HttpClient,
    private val apiUrl: String,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun genererPdf(
        tittel: String,
        payload: PDFMal,
        mal: NotatMal,
    ): ByteArray {
        logger.info("Genererer PDF med ey-pdfgen (mal=$mal)")

        return klient
            .post("$apiUrl/${mal.sti}/${mal.navn}") {
                header(CORRELATION_ID, getCorrelationId())
                contentType(ContentType.Application.Json)
                setBody(PdfGenRequest(tittel, payload.toJsonNode()))
            }.body()
    }
}

private data class PdfGenRequest(
    val tittel: String,
    val payload: JsonNode,
)
