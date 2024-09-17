package no.nav.etterlatte.gyldigsoeknad.journalfoering

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.common.Enhet
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import org.slf4j.LoggerFactory

class DokarkivKlient(
    private val client: HttpClient,
    private val url: String,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    internal suspend fun opprettJournalpost(
        request: OpprettJournalpostRequest,
        forsoekFerdigstill: Boolean = true,
    ): OpprettJournalpostResponse {
        val response =
            client.post("$url?forsoekFerdigstill=$forsoekFerdigstill") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

        return if (response.status.isSuccess()) {
            response
                .body<OpprettJournalpostResponse>()
                .also {
                    logger.info(
                        "Journalpost opprettet (journalpostId=${it.journalpostId}, ferdigstilt=${it.journalpostferdigstilt})",
                    )
                }
        } else if (response.status == HttpStatusCode.Conflict) {
            response
                .body<OpprettJournalpostResponse>()
                .also { logger.warn("Konflikt ved lagring av journalpost ${it.journalpostId}") }
        } else {
            logger.error("Feil oppsto på opprett journalpost: ${response.bodyAsText()}")

            throw ForespoerselException(
                status = response.status.value,
                code = "UKJENT_FEIL_VED_JOURNALFOERING",
                detail = "Ukjent feil oppsto ved journalføring av brev",
            )
        }
    }
}

@Suppress("unused")
data class OpprettJournalpostRequest(
    val tittel: String,
    val tema: String,
    val journalfoerendeEnhet: Enhet?,
    val avsenderMottaker: AvsenderMottaker?,
    val bruker: Bruker?,
    val eksternReferanseId: String,
    val sak: JournalpostSak?,
    var dokumenter: List<JournalpostDokument>,
) {
    val journalpostType: String = "INNGAAENDE"
    val kanal: String = "NAV_NO"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpprettJournalpostResponse(
    val journalpostId: String,
    val journalpostferdigstilt: Boolean,
    val dokumenter: List<DokumentInfo> = emptyList(),
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DokumentInfo(
        val dokumentInfoId: String,
    )
}

sealed class DokumentVariant {
    abstract val filtype: String
    abstract val fysiskDokument: String
    abstract val variantformat: String

    data class ArkivPDF(
        override val fysiskDokument: String,
    ) : DokumentVariant() {
        override val filtype: String = "PDFA"
        override val variantformat: String = "ARKIV"
    }

    data class OriginalJson(
        override val fysiskDokument: String,
    ) : DokumentVariant() {
        override val filtype: String = "JSON"
        override val variantformat: String = "ORIGINAL"
    }
}

@Suppress("unused")
data class JournalpostSak(
    val fagsakId: String,
) {
    val sakstype: String = "FAGSAK"
    val fagsaksystem: String = "EY"
}

@Suppress("unused")
data class AvsenderMottaker(
    val id: String,
) {
    val navn: String = ""
    val idType: String = "FNR"
}

@Suppress("unused")
data class Bruker(
    val id: String,
) {
    val idType = "FNR"
}

@Suppress("unused")
data class JournalpostDokument(
    val tittel: String,
    val dokumentvarianter: List<DokumentVariant>,
) {
    val dokumentKategori = "SOK"
    val brevkode = "XX.YY-ZZ"
}
