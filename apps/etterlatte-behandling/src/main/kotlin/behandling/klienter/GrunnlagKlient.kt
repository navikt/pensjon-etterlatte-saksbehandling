package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory

interface GrunnlagKlient {
    suspend fun finnPersonOpplysning(
        sakId: Long,
        opplysningsType: Opplysningstype,
        accessToken: String
    ): Grunnlagsopplysning<Person>?
}

class GrunnlagKlientImpl(config: Config, httpClient: HttpClient) : GrunnlagKlient {
    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val resourceUrl = config.getString("grunnlag.resource.url")

    override suspend fun finnPersonOpplysning(
        sakId: Long,
        opplysningsType: Opplysningstype,
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

            return objectMapper.readValue(json.toString())
        } catch (e: Exception) {
            logger.error("Henting av opplysning ($opplysningsType) fra grunnlag for sak med id $sakId feilet.", e)
            throw e
        }
    }
}

class GrunnlagKlientTest : GrunnlagKlient {
    override suspend fun finnPersonOpplysning(
        sakId: Long,
        opplysningsType: Opplysningstype,
        accessToken: String
    ): Grunnlagsopplysning<Person>? {
        TODO("Not yet implemented")
    }
}