package no.nav.etterlatte.behandling

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory

interface EtterlattePdl {
    suspend fun hentPerson(fnr: String, accessToken: String): Person
}

// data class Person(private val fnr: String, private val fornavn: String, private val etternavn: String)

class PdltjenesterKlient(config: Config) : EtterlattePdl {

    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient)

    private val clientId = config.getString("pdl.client.id")
    private val resourceUrl = config.getString("pdl.resource.url")

    override suspend fun hentPerson(fnr: String, accessToken: String): Person {

        try {
            logger.info("Henter persondata fra pdl")
            val hentPersonRequest = HentPersonRequest(Foedselsnummer.of(fnr))
            val json = downstreamResourceClient
                .post(
                    Resource(
                        clientId,
                        "$resourceUrl/person"
                    ), accessToken, hentPersonRequest
                ).mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response

            return objectMapper.readValue(json.toString(), Person::class.java)
        } catch (e: Exception) {
            logger.error("Henting av person fra pdl feilet", e)
            throw e
        }
    }

}