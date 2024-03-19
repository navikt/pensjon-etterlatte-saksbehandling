package no.nav.etterlatte.pdl

import com.github.michaelbull.result.get
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMessageBuilder
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.Behandlingsnummer
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class PdlOboKlient(private val httpClient: HttpClient, private val config: Config) {
    private val logger = LoggerFactory.getLogger(PdlKlient::class.java)

    private val apiUrl = config.getString("pdl.url")
    private val pdlScope = config.getString("pdl.scope")

    private val azureAdClient = AzureAdClient(config, httpClient)

    suspend fun hentPersonNavn(
        ident: String,
        bruker: BrukerTokenInfo,
    ): PdlPersonNavnResponse {
        val request =
            PdlGraphqlRequest(
                query = getQuery("/pdl/hentPersonNavn.graphql"),
                variables = PdlVariables(ident),
            )

        return retry<PdlPersonNavnResponse>(times = 3) {
            httpClient.post(apiUrl) {
                bearerAuth(getOboToken(bruker))
                behandlingsnummer(Behandlingsnummer.BARNEPENSJON, Behandlingsnummer.OMSTILLINGSSTOENAD)
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(request)
            }.body()
        }.let {
            when (it) {
                is RetryResult.Success ->
                    it.content.also { result ->
                        result.errors?.joinToString()?.let { feil ->
                            logger.error("Fikk data fra PDL, men også følgende feil: $feil")
                        }
                    }

                is RetryResult.Failure -> throw it.samlaExceptions()
            }
        }
    }

    suspend fun hentPerson(
        fnr: Folkeregisteridentifikator,
        rolle: PersonRolle,
        saktyper: List<SakType>,
        bruker: BrukerTokenInfo,
    ): PdlPersonResponse {
        val request =
            PdlGraphqlRequest(
                query = getQuery("/pdl/hentPerson.graphql"),
                variables = toPdlVariables(fnr, rolle),
            )

        val behandlingsnummere = hentBehandlingsnummerFromSaktyper(saktyper)

        return retry<PdlPersonResponse>(times = 3) {
            httpClient.post(apiUrl) {
                bearerAuth(getOboToken(bruker))
                behandlingsnummer(behandlingsnummere)
                header(HEADER_BEHANDLINGSNUMMER, behandlingsnummere.joinToString { it.behandlingsnummer })
                header(PdlKlient.HEADER_TEMA, PdlKlient.HEADER_TEMA_VALUE)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }.let {
            when (it) {
                is RetryResult.Success ->
                    it.content.also { result ->
                        result.errors?.joinToString(",")?.let { feil ->
                            logger.error("Fikk data fra PDL, men også følgende feil: $feil")
                        }
                    }

                is RetryResult.Failure -> throw it.samlaExceptions()
            }
        }
    }

    private fun getQuery(name: String): String {
        return javaClass.getResource(name)!!
            .readText()
            .replace(Regex("[\n\t]"), "")
    }

    private suspend fun getOboToken(bruker: BrukerTokenInfo): String {
        val token =
            azureAdClient.hentTokenFraAD(
                bruker,
                listOf(pdlScope),
            )

        return requireNotNull(token.get()?.accessToken) {
            "Kunne ikke hente ut obo-token for bruker ${bruker.ident()}"
        }
    }

    private fun HttpMessageBuilder.behandlingsnummer(vararg behandlingsnummer: Behandlingsnummer): Unit =
        header(HEADER_BEHANDLINGSNUMMER, behandlingsnummer.joinToString { it.behandlingsnummer })

    private fun hentBehandlingsnummerFromSaktyper(saktyper: List<SakType>): List<Behandlingsnummer> {
        return saktyper.map {
            when (it) {
                SakType.BARNEPENSJON -> Behandlingsnummer.BARNEPENSJON
                SakType.OMSTILLINGSSTOENAD -> Behandlingsnummer.OMSTILLINGSSTOENAD
            }
        }.distinct()
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
}
