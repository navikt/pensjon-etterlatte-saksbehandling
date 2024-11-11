package no.nav.etterlatte.pdl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PDLIdentGruppeTyper
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.ktor.behandlingsnummer
import no.nav.etterlatte.utils.toPdlVariables
import org.slf4j.LoggerFactory

class PdlKlient(
    private val httpClient: HttpClient,
    private val apiUrl: String,
) {
    private val logger = LoggerFactory.getLogger(PdlKlient::class.java)

    suspend fun hentPerson(hentPersonRequest: HentPersonRequest): PdlPersonResponse {
        val request =
            PdlGraphqlRequest(
                query = getQuery("/pdl/hentPerson.graphql"),
                variables = toPdlVariables(hentPersonRequest.foedselsnummer, hentPersonRequest.rolle),
            )

        return retry<PdlPersonResponse>(times = 3) {
            httpClient
                .post(apiUrl) {
                    behandlingsnummer(hentPersonRequest.saktyper)
                    header(HEADER_TEMA, HEADER_TEMA_VALUE)
                    accept(Json)
                    contentType(Json)
                    setBody(request)
                }.body()
        }.let {
            when (it) {
                is RetryResult.Success ->
                    it.content.also { loggDelvisReturnerteData(it, request) }

                is RetryResult.Failure -> throw it.samlaExceptions()
            }
        }
    }

    suspend fun hentAdressebeskyttelse(hentAdressebeskyttelseRequest: HentAdressebeskyttelseRequest): PdlAdressebeskyttelseResponse {
        val request =
            PdlAdressebeskyttelseRequest(
                query = getQuery("/pdl/hentAdressebeskyttelse.graphql"),
                variables = PdlAdressebeskyttelseVariables(hentAdressebeskyttelseRequest.ident.value),
            )

        return retry<PdlAdressebeskyttelseResponse>(times = 3) {
            httpClient
                .post(apiUrl) {
                    behandlingsnummer(hentAdressebeskyttelseRequest.saktype)
                    header(HEADER_TEMA, HEADER_TEMA_VALUE)
                    accept(Json)
                    contentType(Json)
                    setBody(request)
                }.body()
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.samlaExceptions()
            }
        }
    }

    suspend fun hentPersonHistorikkForeldreansvar(fnr: Folkeregisteridentifikator): PdlHentForeldreansvarHistorikkResponse {
        val request =
            PdlHentForeldreansvarHistorikkRequest(
                query = getQuery("/pdl/hentPersonHistorikkForeldreansvar.graphql"),
                variables =
                    PdlHentForelderansvarHistorikkVariables(
                        ident = fnr.value,
                    ),
            )

        return retry<PdlHentForeldreansvarHistorikkResponse>(times = 3) {
            httpClient
                .post(apiUrl) {
                    behandlingsnummer(SakType.BARNEPENSJON)
                    header(HEADER_TEMA, HEADER_TEMA_VALUE)
                    accept(Json)
                    contentType(Json)
                    setBody(request)
                }.body()
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.samlaExceptions()
            }
        }
    }

    suspend fun hentPersonBolk(
        fnr: List<Folkeregisteridentifikator>,
        saktyper: List<SakType>,
    ): PdlPersonResponseBolk {
        val request =
            PdlGraphqlBolkRequest(
                query = getQuery("/pdl/hentPersonBolk.graphql"),
                variables =
                    PdlBolkVariables(
                        identer = fnr.map { it.value },
                        bostedsadresse = true,
                        bostedsadresseHistorikk = true,
                        deltBostedsadresse = true,
                        kontaktadresse = true,
                        kontaktadresseHistorikk = true,
                        oppholdsadresse = true,
                        oppholdsadresseHistorikk = true,
                        utland = true,
                        sivilstand = false,
                        familieRelasjon = true,
                        vergemaal = true,
                    ),
            )

        logger.info("Bolkhenter personer med fnr=$fnr fra PDL")
        return retry<PdlPersonResponseBolk> {
            httpClient
                .post(apiUrl) {
                    behandlingsnummer(saktyper)
                    header(HEADER_TEMA, HEADER_TEMA_VALUE)
                    accept(Json)
                    contentType(Json)
                    setBody(request)
                }.body()
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.samlaExceptions()
            }
        }
    }

    suspend fun hentPdlIdentifikator(
        request: HentPdlIdentRequest,
        grupper: List<PDLIdentGruppeTyper> = listOf(PDLIdentGruppeTyper.FOLKEREGISTERIDENT, PDLIdentGruppeTyper.NPID),
    ): PdlIdentResponse {
        val graphqlRequest =
            PdlIdentRequest(
                query = getQuery("/pdl/hentIdenter.graphql"),
                variables =
                    PdlIdentVariables(
                        ident = request.ident.value,
                        grupper = grupper.map { it.navn },
                        historikk = true,
                    ),
            )
        logger.info("Henter PdlIdentifikator for ident = ${request.ident} fra PDL")

        return retry<PdlIdentResponse> {
            httpClient
                .post(apiUrl) {
                    behandlingsnummer(SakType.entries)
                    header(HEADER_TEMA, HEADER_TEMA_VALUE)
                    accept(Json)
                    contentType(Json)
                    setBody(graphqlRequest)
                }.body()
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.samlaExceptions()
            }
        }
    }

    suspend fun hentGeografiskTilknytning(request: HentGeografiskTilknytningRequest): PdlGeografiskTilknytningResponse {
        val graphqlRequest =
            PdlGeografiskTilknytningRequest(
                query = getQuery("/pdl/hentGeografiskTilknytning.graphql"),
                variables =
                    PdlGeografiskTilknytningIdentVariables(
                        ident = request.foedselsnummer.value,
                    ),
            )

        logger.info("Henter geografisk tilknytning for fnr = ${request.foedselsnummer} fra PDL")
        return retry<PdlGeografiskTilknytningResponse> {
            httpClient
                .post(apiUrl) {
                    behandlingsnummer(SakType.BARNEPENSJON)
                    header(HEADER_TEMA, HEADER_TEMA_VALUE)
                    accept(Json)
                    contentType(Json)
                    setBody(graphqlRequest)
                }.body()
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.samlaExceptions()
            }
        }
    }

    suspend fun hentAktoerId(request: HentPdlIdentRequest): PdlIdentResponse {
        val graphqlRequest =
            PdlIdentRequest(
                query = getQuery("/pdl/hentIdenter.graphql"),
                variables =
                    PdlIdentVariables(
                        ident = request.ident.value,
                        grupper = listOf(PDLIdentGruppeTyper.AKTORID.navn),
                        historikk = true,
                    ),
            )
        logger.info("Henter AktørID for ident = ${request.ident} fra PDL")

        return retry<PdlIdentResponse> {
            httpClient
                .post(apiUrl) {
                    accept(Json)
                    contentType(Json)
                    setBody(graphqlRequest)
                }.body()
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.samlaExceptions()
            }
        }
    }

    private fun getQuery(name: String): String =
        javaClass
            .getResource(name)!!
            .readText()
            .replace(Regex("[\n\t]"), "")

    companion object {
        const val HEADER_TEMA = "Tema"
        const val HEADER_TEMA_VALUE = "PEN"
    }
}
