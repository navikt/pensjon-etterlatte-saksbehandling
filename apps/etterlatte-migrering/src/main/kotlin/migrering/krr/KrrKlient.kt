package no.nav.etterlatte.migrering.person.krr

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.migrering.pen.migreringssystembruker
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Krr {
    suspend fun hentDigitalKontaktinformasjon(fnr: Folkeregisteridentifikator): DigitalKontaktinformasjon?
}

class KrrKlient(client: HttpClient, config: Config) : Krr {
    private val logger: Logger = LoggerFactory.getLogger(KrrKlient::class.java)
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, client)
    private val url = config.getString("krr.url")

    private val clientId = config.getString("krr.client_id")

    override suspend fun hentDigitalKontaktinformasjon(fnr: Folkeregisteridentifikator): DigitalKontaktinformasjon? {
        logger.info("Henter kontaktopplysninger fra KRR.")

        return try {
            downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$url/person",
                            additionalHeaders =
                                mapOf(
                                    HttpHeaders.NavPersonIdent to fnr.value,
                                    HttpHeaders.Accept to ContentType.Application.Json.toString(),
                                    HttpHeaders.ContentType to ContentType.Application.Json.toString(),
                                ),
                        ),
                    brukerTokenInfo = migreringssystembruker,
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { response -> throw KrrException(response) },
                )
        } catch (cause: Throwable) {
            logger.warn("Klarte ikke å hente kontaktinformasjon fra KRR.", KrrException(cause))
            return null
        }
    }
}

class KrrException(cause: Throwable) :
    Exception("Klarte ikke å hente digital kontaktinfo fra Krr", cause)

val HttpHeaders.NavPersonIdent: String
    get() = "Nav-Personident"
