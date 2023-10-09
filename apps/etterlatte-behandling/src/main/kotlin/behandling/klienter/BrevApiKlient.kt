package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.libs.common.behandling.Mottaker
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo

interface BrevApiKlient {
    suspend fun opprettKlageInnstillingsbrevISak(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto

    suspend fun ferdigstillBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    )

    /**
     * @return journalpostId til brevets journalpost
     */
    suspend fun journalfoerBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): JournalpostIdDto

    /**
     * @return bestillingsId for distribusjonen
     */
    suspend fun distribuerBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): DistribusjonskvitteringDto

    suspend fun hentBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto
}

class BrevApiKlientObo(config: Config, client: HttpClient) : BrevApiKlient {
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, client)

    private val clientId = config.getString("brev-api.client.id")
    private val resourceUrl = config.getString("brev-api.resource.url")

    override suspend fun opprettKlageInnstillingsbrevISak(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto {
        try {
            return downstreamResourceClient.post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/brev/sak/$sakId",
                    ),
                brukerTokenInfo = brukerTokenInfo,
                postBody = Unit,
            ).mapBoth(
                success = { resource ->
                    resource.response?.let { objectMapper.readValue(it.toString()) }
                        ?: throw RuntimeException("Fikk ikke en riktig respons fra oppretting av brev")
                },
                failure = { errorResponse -> throw errorResponse },
            )
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun ferdigstillBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        try {
            downstreamResourceClient.post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/brev/$brevId/ferdigstill?sakId=$sakId",
                    ),
                brukerTokenInfo = brukerTokenInfo,
                postBody = Unit,
            ).mapBoth(
                success = { resource -> resource },
                failure = { errorResponse -> throw errorResponse },
            )
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun journalfoerBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): JournalpostIdDto {
        try {
            return downstreamResourceClient.post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/brev/$brevId/journalfoer?sakId=$sakId",
                    ),
                brukerTokenInfo = brukerTokenInfo,
                postBody = Unit,
            ).mapBoth(
                success = { resource ->
                    objectMapper.readValue(resource.response!!.toJson())
                },
                failure = { errorResponse -> throw errorResponse },
            )
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun distribuerBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): DistribusjonskvitteringDto {
        try {
            return downstreamResourceClient.post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/brev/$brevId/distribuer?sakId=$sakId",
                    ),
                brukerTokenInfo = brukerTokenInfo,
                postBody = Unit,
            ).mapBoth(
                success = { resource ->
                    objectMapper.readValue(resource.response!!.toJson())
                },
                failure = { errorResponse -> throw errorResponse },
            )
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun hentBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto {
        try {
            return downstreamResourceClient.get(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/brev/$brevId?sakId=$sakId",
                    ),
                brukerTokenInfo = brukerTokenInfo,
            ).mapBoth(
                success = { resource ->
                    objectMapper.readValue(resource.response.toString())
                },
                failure = { error -> throw error },
            )
        } catch (e: Exception) {
            throw e
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpprettetBrevDto(val id: Long, val mottaker: Mottaker)

data class JournalpostIdDto(
    val journalpostId: String,
)

data class DistribusjonskvitteringDto(
    val distribusjonskvittering: String,
)
