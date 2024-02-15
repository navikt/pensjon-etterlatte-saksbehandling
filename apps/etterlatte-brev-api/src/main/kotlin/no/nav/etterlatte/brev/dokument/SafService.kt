package no.nav.etterlatte.brev.dokument

import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.brev.dokarkiv.BrukerIdType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class SafService(
    private val safKlient: SafKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun hentDokumentPDF(
        journalpostId: String,
        dokumentInfoId: String,
        accessToken: String,
    ): ByteArray {
        logger.info("Henter dokument (journalpostId=$journalpostId, dokumentInfoId=$dokumentInfoId)")

        return safKlient.hentDokumentPDF(journalpostId, dokumentInfoId, accessToken)
    }

    suspend fun hentDokumenter(
        ident: String,
        visTemaPen: Boolean,
        idType: BrukerIdType,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Journalpost> {
        logger.info("Henter alle journalposter for ident=${ident.maskerFnr()} (visTemaPen=$visTemaPen)")

        val response = safKlient.hentDokumenter(ident, visTemaPen, idType, brukerTokenInfo)

        return if (response.errors.isNullOrEmpty()) {
            response.data?.dokumentoversiktBruker?.journalposter ?: emptyList()
        } else {
            throw konverterTilForespoerselException(response.errors)
        }
    }

    suspend fun hentJournalpost(
        journalpostId: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): Journalpost? {
        logger.info("Henter journalpost (id=$journalpostId)")

        val response = safKlient.hentJournalpost(journalpostId, brukerTokenInfo)

        return if (response.errors.isNullOrEmpty()) {
            response.data?.journalpost
        } else {
            throw konverterTilForespoerselException(response.errors)
        }
    }

    private fun konverterTilForespoerselException(errors: List<Error>): ForespoerselException {
        errors.forEach {
            logger.error("${errors.size} feil oppsto ved kall mot saf: ${it.toJson()}")
        }

        val error = errors.firstOrNull()

        return when (error?.extensions?.code) {
            Error.Code.FORBIDDEN -> IkkeTilgangTilJournalpost()
            Error.Code.NOT_FOUND ->
                IkkeFunnetException(
                    code = "IKKE_FUNNET",
                    detail = "Fant ikke journalpost(er)",
                )

            Error.Code.BAD_REQUEST ->
                UgyldigForespoerselException(
                    code = "UGYLDIG_FORESPOERSEL_SAF",
                    detail = "Ugyldig forespørsel mot Saf. Hvis problemet vedvarer, opprett sak i Porten",
                )

            Error.Code.SERVER_ERROR -> SafServerError()
            else -> UkjentFeilSaf()
        }
    }
}

class IkkeTilgangTilJournalpost : UgyldigForespoerselException(
    code = "IKKE_TILGANG_JOURNALPOST",
    detail = "Ikke tilgang til å se journalposten",
)

class SafServerError : ForespoerselException(
    status = HttpStatusCode.InternalServerError.value,
    code = "SAF_SERVER_ERROR",
    detail = "Teknisk feil i Saf. Prøv igjen om litt",
)

class UkjentFeilSaf : ForespoerselException(
    status = HttpStatusCode.InternalServerError.value,
    code = "UKJENT_FEIL_SAF",
    detail = "Ukjent feil oppsto",
)
