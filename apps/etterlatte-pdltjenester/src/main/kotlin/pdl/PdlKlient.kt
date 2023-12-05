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
import no.nav.etterlatte.libs.common.person.Behandlingsnummer
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdenterForAktoerIdBolkRequest
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PDLIdentGruppeTyper
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.retry
import org.slf4j.LoggerFactory

class PdlKlient(private val httpClient: HttpClient, private val apiUrl: String) {
    private val logger = LoggerFactory.getLogger(PdlKlient::class.java)

    suspend fun hentPerson(hentPersonRequest: HentPersonRequest): PdlPersonResponse {
        val request =
            PdlGraphqlRequest(
                query = getQuery("/pdl/hentPerson.graphql"),
                variables = toPdlVariables(hentPersonRequest.foedselsnummer, hentPersonRequest.rolle),
            )

        val behandlingsnummer = findBehandlingsnummerFromSaktype(hentPersonRequest.saktype)

        return retry<PdlPersonResponse>(times = 3) {
            httpClient.post(apiUrl) {
                header(HEADER_BEHANDLINGSNUMMER, behandlingsnummer.behandlingsnummer)
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

    suspend fun hentAdressebeskyttelse(hentAdressebeskyttelseRequest: HentAdressebeskyttelseRequest): PdlAdressebeskyttelseResponse {
        val request =
            PdlAdressebeskyttelseRequest(
                query = getQuery("/pdl/hentAdressebeskyttelse.graphql"),
                variables = PdlAdressebeskyttelseVariables(hentAdressebeskyttelseRequest.ident.value),
            )

        return retry<PdlAdressebeskyttelseResponse>(times = 3) {
            httpClient.post(apiUrl) {
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

        val behandlingsnummer = findBehandlingsnummerFromSaktype(SakType.BARNEPENSJON)
        return retry<PdlHentForeldreansvarHistorikkResponse>(times = 3) {
            httpClient.post(apiUrl) {
                header(HEADER_BEHANDLINGSNUMMER, behandlingsnummer.behandlingsnummer)
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
        saktype: SakType,
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

        val behandlingsnummer = findBehandlingsnummerFromSaktype(saktype)

        logger.info("Bolkhenter personer med fnr=$fnr fra PDL")
        return retry<PdlPersonResponseBolk> {
            httpClient.post(apiUrl) {
                header(HEADER_BEHANDLINGSNUMMER, behandlingsnummer.behandlingsnummer)
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

    suspend fun hentPdlIdentifikator(request: HentPdlIdentRequest): PdlIdentResponse {
        val graphqlRequest =
            PdlFolkeregisterIdentRequest(
                query = getQuery("/pdl/hentFolkeregisterIdent.graphql"),
                variables =
                    PdlFolkeregisterIdentVariables(
                        ident = request.ident.value,
                        grupper = listOf(PDLIdentGruppeTyper.FOLKEREGISTERIDENT.navn, PDLIdentGruppeTyper.NPID.navn),
                        historikk = true,
                    ),
            )
        logger.info("Henter PdlIdentifikator for ident = ${request.ident} fra PDL")
        return retry<PdlIdentResponse> {
            httpClient.post(apiUrl) {
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

    suspend fun hentFolkeregisterIdenterForAktoerIdBolk(
        request: HentFolkeregisterIdenterForAktoerIdBolkRequest,
    ): List<HentIdenterBolkResult> {
        return request.aktoerIds.chunked(PDL_BULK_SIZE).map { identerChunk ->
            val graphqlBolkRequest =
                PdlFoedselsnumreFraAktoerIdRequest(
                    query = getQuery("/pdl/hentFolkeregisterIdenterBolk.graphql"),
                    variables =
                        IdenterBolkVariables(
                            identer = identerChunk,
                            grupper = setOf(IdentGruppe.FOLKEREGISTERIDENT),
                        ),
                )

            logger.info("Henter folkeregisterident for ${request.aktoerIds.size} aktørIds fra PDL")

            val response =
                retry<PdlFoedselsnumreFraAktoerIdResponse> {
                    httpClient.post(apiUrl) {
                        header(HEADER_TEMA, HEADER_TEMA_VALUE)
                        accept(Json)
                        contentType(Json)
                        setBody(graphqlBolkRequest)
                    }.body()
                }.let {
                    when (it) {
                        is RetryResult.Success -> it.content
                        is RetryResult.Failure -> throw it.samlaExceptions()
                    }
                }
            response.data
        }.flatMap { it.hentIdenterBolk }
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
        val behandlingsnummer = findBehandlingsnummerFromSaktype(request.saktype)
        return retry<PdlGeografiskTilknytningResponse> {
            httpClient.post(apiUrl) {
                header(HEADER_BEHANDLINGSNUMMER, behandlingsnummer.behandlingsnummer)
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

    private fun getQuery(name: String): String {
        return javaClass.getResource(name)!!
            .readText()
            .replace(Regex("[\n\t]"), "")
    }

    private fun toPdlVariables(
        fnr: Folkeregisteridentifikator,
        rolle: PersonRolle,
    ) = when (rolle) {
        PersonRolle.INNSENDER ->
            PdlVariables(
                ident = fnr.value,
                bostedsadresse = true,
                bostedsadresseHistorikk = false,
                deltBostedsadresse = false,
                kontaktadresse = false,
                kontaktadresseHistorikk = false,
                oppholdsadresse = false,
                oppholdsadresseHistorikk = false,
                utland = false,
                sivilstand = false,
                familieRelasjon = false,
                vergemaal = false,
            )
        PersonRolle.BARN ->
            PdlVariables(
                ident = fnr.value,
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
            )
        PersonRolle.GJENLEVENDE ->
            PdlVariables(
                ident = fnr.value,
                bostedsadresse = true,
                bostedsadresseHistorikk = true,
                deltBostedsadresse = false,
                kontaktadresse = false,
                kontaktadresseHistorikk = false,
                oppholdsadresse = true,
                oppholdsadresseHistorikk = false,
                utland = true,
                sivilstand = true,
                familieRelasjon = true,
                vergemaal = true,
            )
        PersonRolle.AVDOED ->
            PdlVariables(
                ident = fnr.value,
                bostedsadresse = true,
                bostedsadresseHistorikk = true,
                deltBostedsadresse = false,
                kontaktadresse = true,
                kontaktadresseHistorikk = true,
                oppholdsadresse = true,
                oppholdsadresseHistorikk = true,
                utland = true,
                sivilstand = true,
                familieRelasjon = true,
                vergemaal = false,
            )
        PersonRolle.TILKNYTTET_BARN ->
            PdlVariables(
                ident = fnr.value,
                bostedsadresse = true,
                bostedsadresseHistorikk = true,
                deltBostedsadresse = true,
                kontaktadresse = true,
                kontaktadresseHistorikk = true,
                oppholdsadresse = true,
                oppholdsadresseHistorikk = true,
                utland = true,
                sivilstand = false,
                familieRelasjon = false,
                vergemaal = false,
            )
    }

    companion object {
        const val HEADER_BEHANDLINGSNUMMER = "behandlingsnummer"
        const val HEADER_TEMA = "Tema"
        const val HEADER_TEMA_VALUE = "PEN"
        const val PDL_BULK_SIZE = 100
    }
}

fun findBehandlingsnummerFromSaktype(saktype: SakType): Behandlingsnummer {
    return when (saktype) {
        SakType.BARNEPENSJON -> Behandlingsnummer.BARNEPENSJON
        SakType.OMSTILLINGSSTOENAD -> Behandlingsnummer.OMSTILLINGSSTOENAD
    }
}
