package no.nav.etterlatte.brev.dokument

import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.brev.dokarkiv.BrukerIdType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class SafService(
    private val safKlient: SafKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun hentDokumentPDF(
        journalpostId: String,
        dokumentInfoId: String,
        bruker: BrukerTokenInfo,
    ): ByteArray {
        logger.info("Henter dokument (journalpostId=$journalpostId, dokumentInfoId=$dokumentInfoId)")

        return safKlient.hentDokumentPDF(journalpostId, dokumentInfoId, bruker)
    }

    suspend fun hentDokumenter(
        request: HentDokumenterRequest,
        bruker: BrukerTokenInfo,
    ): Journalposter {
        logger.info("Henter journalposter for ident=${request.foedselsnummer}")

        val graphqlVariables =
            DokumentOversiktBrukerVariables(
                brukerId = BrukerId(request.foedselsnummer.value, BrukerIdType.FNR),
                tema = request.tema,
                journalposttyper = request.journalposttyper,
                journalstatuser = request.journalstatuser,
                foerste = request.foerste,
                etter = request.etter,
            )

        val response = safKlient.hentDokumenter(graphqlVariables, bruker)

        return if (response.errors.isNullOrEmpty()) {
            response.data?.dokumentoversiktBruker ?: throw IkkeFunnetException(
                code = "INGEN_JOURNALPOSTER_FUNNET",
                detail = "Fant ingen journalposter på brukeren",
            )
        } else {
            throw konverterTilForespoerselException(response.errors)
        }
    }

    suspend fun hentJournalpost(
        journalpostId: String,
        bruker: BrukerTokenInfo,
    ): Journalpost {
        logger.info("Henter journalpost (id=$journalpostId)")

        val response = safKlient.hentJournalpost(journalpostId, bruker)

        return if (response.errors.isNullOrEmpty()) {
            response.data?.journalpost
                ?: throw JournalpostIkkeFunnet(journalpostId)
        } else {
            throw konverterTilForespoerselException(response.errors)
        }
    }

    suspend fun hentUtsendingsinfo(
        journalpostId: String,
        bruker: BrukerTokenInfo,
    ): JournalpostUtsendingsinfo? {
        logger.info("Henter utsendingsinfo fra journalpost (id=$journalpostId)")

        val response = safKlient.hentUtsendingsInfo(journalpostId, bruker)

        return if (response.errors.isNullOrEmpty()) {
            response.data?.journalpost
        } else {
            throw konverterTilForespoerselException(response.errors)
        }
    }

    private fun konverterTilForespoerselException(errors: List<Error>): ForespoerselException {
        errors.forEach {
            if (errors.all { err -> err.extensions?.code == Error.Code.FORBIDDEN }) {
                logger.warn("${errors.size} feil oppsto ved kall mot saf, alle var tilgangssjekk: ${it.toJson()}")
            } else {
                logger.error("${errors.size} feil oppsto ved kall mot saf: ${it.toJson()}")
            }
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

class JournalpostIkkeFunnet(
    journalpostId: String,
) : IkkeFunnetException(
        code = "JOURNALPOST_IKKE_FUNNET",
        detail = "Journalpost med journalpostId=$journalpostId ikke funnet i Joark",
    )

class IkkeTilgangTilJournalpost :
    ForespoerselException(
        status = HttpStatusCode.Forbidden.value,
        code = "IKKE_TILGANG_JOURNALPOST",
        detail = "Ikke tilgang til å se journalposten",
    )

class SafServerError :
    ForespoerselException(
        status = HttpStatusCode.InternalServerError.value,
        code = "SAF_SERVER_ERROR",
        detail = "Teknisk feil i Saf. Prøv igjen om litt",
    )

class UkjentFeilSaf :
    ForespoerselException(
        status = HttpStatusCode.InternalServerError.value,
        code = "UKJENT_FEIL_SAF",
        detail = "Ukjent feil oppsto",
    )
