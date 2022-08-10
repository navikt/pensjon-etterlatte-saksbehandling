package no.nav.etterlatte.behandling

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.*
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory

interface EtterlatteGrunnlag {
    suspend fun finnOpplysning(
        sakId: Long,
        opplysningsType: Opplysningstyper,
        accessToken: String
    ): Grunnlagsopplysning<Person>?
}

class GrunnlagKlient(config: Config, httpClient: HttpClient) : EtterlatteGrunnlag {
    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val resourceUrl = config.getString("grunnlag.resource.url")

    override suspend fun finnOpplysning(
        sakId: Long,
        opplysningsType: Opplysningstyper,
        accessToken: String
    ): Grunnlagsopplysning<Person>? {
        try {
            logger.info("Henter opplysning ($opplysningsType) fra grunnlag for sak med id $sakId.")

            val json = downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/grunnlag/$sakId/$opplysningsType"
                    ),
                    accessToken = accessToken
                )
                .mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response

            logger.info("svar fra grunnlag: $json")
            return objectMapper.readValue(json.toString())
        } catch (e: Exception) {
            logger.error("Henting av opplysning ($opplysningsType) fra grunnlag for sak med id $sakId feilet.")
            throw e
        }
    }
}
