package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.libs.common.behandling.Mottaker
import no.nav.etterlatte.libs.common.brev.BestillingsIdDto
import no.nav.etterlatte.libs.common.brev.JournalpostIdDto
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

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
    ): BestillingsIdDto

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

    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun opprettKlageInnstillingsbrevISak(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto {
        val opprettetBrev: OpprettetBrevDto =
            downstreamResourceClient.post(
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
        try {
            settTittelForBrev(sakId, opprettetBrev.id, "Innstillingsbrev til NAV Klageinstans", brukerTokenInfo)
        } catch (e: Exception) {
            logger.warn(
                "Fikk ikke satt tittel på opprettet innstillingsbrev med id=${opprettetBrev.id} " +
                    "for klage i sak med id=$sakId på grunn av feil. Saksbehandler kan endre i frontend som " +
                    "workaround.",
                e,
            )
        }
        return opprettetBrev
    }

    override suspend fun ferdigstillBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
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
    }

    override suspend fun journalfoerBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): JournalpostIdDto {
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
    }

    override suspend fun distribuerBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): BestillingsIdDto {
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
    }

    override suspend fun hentBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto {
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
    }

    private suspend fun settTittelForBrev(
        sakId: Long,
        brevId: Long,
        tittel: String,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        downstreamResourceClient.post(
            resource =
                Resource(
                    clientId = clientId,
                    url = "$resourceUrl/api/brev/$brevId/tittel?sakId=$sakId",
                ),
            brukerTokenInfo = brukerTokenInfo,
            postBody = OppdaterTittelRequest(tittel),
        ).mapBoth(success = {}, failure = { error -> throw error })
    }
}

data class OppdaterTittelRequest(
    val tittel: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpprettetBrevDto(val id: Long, val mottaker: Mottaker)
