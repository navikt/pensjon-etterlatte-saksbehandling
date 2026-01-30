package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.brev.BrevParametre
import no.nav.etterlatte.brev.BrevPayload
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevStatusResponse
import no.nav.etterlatte.brev.model.FerdigstillJournalFoerOgDistribuerOpprettetBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.brev.BestillingsIdDto
import no.nav.etterlatte.libs.common.brev.JournalpostIdDto
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Systembruker
import java.time.Duration
import java.util.UUID

interface BrevApiKlient {
    suspend fun opprettSpesifiktBrev(
        sakId: SakId,
        brevParametre: BrevParametre,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev

    suspend fun slettBrev(
        brevId: Long,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun oppdaterSpesifiktBrev(
        sakId: SakId,
        brevId: BrevID,
        brevParametre: BrevParametre,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev

    suspend fun ferdigstillJournalFoerOgDistribuerBrev(
        req: FerdigstillJournalFoerOgDistribuerOpprettetBrev,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevStatusResponse

    suspend fun opprettKlageOversendelsesbrevISak(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev

    suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev

    suspend fun ferdigstillVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun ferdigstillOversendelseBrev(
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    )

    /**
     * @return journalpostId til brevets journalpost
     */
    suspend fun journalfoerBrev(
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): JournalpostIdDto

    /**
     * @return bestillingsId for distribusjonen
     */
    suspend fun distribuerBrev(
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): BestillingsIdDto

    suspend fun hentBrev(
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev

    suspend fun slettVedtaksbrev(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun journalfoerNotatKa(
        klage: Klage,
        brukerInfoToken: BrukerTokenInfo,
    ): OpprettJournalpostDto

    suspend fun slettOversendelsesbrev(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun hentVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev?

    suspend fun hentOversendelsesbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev?

    suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pdf

    suspend fun tilbakestillVedtaksbrev(
        brevID: BrevID,
        behandlingId: UUID,
        sakId: SakId,
        brevtype: Brevtype,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevPayload

    /**
     * Denne metoden ligger sperret bak kun bruk av systembrukere. Dette for å sikre at det kun brukes når det er
     * "korrekt", og ikke i andre tilfeller der man ikke burde fjerne ferdigstilling av brevet
     */
    suspend fun fjernFerdigstillingTilbakekreving(
        tilbakekrevingId: UUID,
        sakId: SakId,
        systembruker: Systembruker,
    )

    suspend fun hentPdf(
        sakId: SakId,
        brevID: BrevID,
        bruker: BrukerTokenInfo,
    ): Pdf?
}

class BrevApiKlientObo(
    config: Config,
    client: HttpClient,
) : BrevApiKlient {
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, client)

    private val clientId = config.getString("brev-api.client.id")
    private val resourceUrl = config.getString("brev-api.resource.url")

    override suspend fun slettBrev(
        brevId: Long,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        downstreamResourceClient
            .delete(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/brev/$brevId?${SAKID_CALL_PARAMETER}=${sakId.sakId}",
                    ),
                brukerTokenInfo = brukerTokenInfo,
            ).mapBoth(
                success = { },
                failure = { errorResponse -> throw errorResponse },
            )
    }

    override suspend fun ferdigstillJournalFoerOgDistribuerBrev(
        req: FerdigstillJournalFoerOgDistribuerOpprettetBrev,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevStatusResponse =
        post(
            url = "$resourceUrl/api/brev/sak/${req.sakId}/ferdigstill-journalfoer-og-distribuer",
            onSuccess = { resource ->
                resource.response?.let { objectMapper.readValue(it.toJson()) }
                    ?: throw RuntimeException("Fikk ikke en riktig respons fra oppretting av brev")
            },
            brukerTokenInfo = brukerTokenInfo,
            postBody = req,
            timeoutConfig = {
                socketTimeoutMillis = Duration.ofSeconds(20).toMillis()
                requestTimeoutMillis = Duration.ofSeconds(20).toMillis()
            },
        )

    override suspend fun opprettSpesifiktBrev(
        sakId: SakId,
        brevParametre: BrevParametre,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev =
        post(
            url = "$resourceUrl/api/brev/sak/$sakId/spesifikk",
            onSuccess = { resource ->
                resource.response?.let { objectMapper.readValue(it.toJson()) }
                    ?: throw InternfeilException(
                        "Fikk ikke en riktig respons fra oppretting av brev av " +
                            "type ${brevParametre.brevkode} i sak $sakId",
                    )
            },
            brukerTokenInfo = brukerTokenInfo,
            postBody = brevParametre.toJson(),
        )

    override suspend fun oppdaterSpesifiktBrev(
        sakId: SakId,
        brevId: Long,
        brevParametre: BrevParametre,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev =
        post(
            url = "$resourceUrl/api/brev/sak/$sakId/spesifikk/$brevId",
            onSuccess = { resource ->
                resource.response?.let { objectMapper.readValue(it.toJson()) }
                    ?: throw InternfeilException(
                        "Fikk ikke en riktig respons fra oppdatering av brev med " +
                            "id=$brevId av type ${brevParametre.brevkode} i sak $sakId",
                    )
            },
            brukerTokenInfo = brukerTokenInfo,
            postBody = brevParametre.toJson(),
        )

    override suspend fun opprettKlageOversendelsesbrevISak(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev =
        post(
            url = "$resourceUrl/api/brev/behandling/$klageId/oversendelse",
            onSuccess = { resource ->
                resource.response?.let { objectMapper.readValue(it.toJson()) }
                    ?: throw RuntimeException("Fikk ikke en riktig respons fra oppretting av brev")
            },
            brukerTokenInfo = brukerTokenInfo,
        )

    override suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev =
        post(
            url = "$resourceUrl/api/brev/behandling/$behandlingId/vedtak?sakId=${sakId.sakId}",
            onSuccess = { resource -> deserialize(resource.response!!.toJson()) },
            brukerTokenInfo = brukerTokenInfo,
        )

    override suspend fun ferdigstillVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        post(
            url = "$resourceUrl/api/brev/behandling/$behandlingId/vedtak/ferdigstill",
            onSuccess = { },
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    override suspend fun ferdigstillOversendelseBrev(
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        post(
            url = "$resourceUrl/api/brev/$brevId/oversendelse/ferdigstill?sakId=${sakId.sakId}",
            postBody = "",
            onSuccess = {},
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    override suspend fun journalfoerBrev(
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): JournalpostIdDto =
        post(
            url = "$resourceUrl/api/brev/$brevId/journalfoer?sakId=${sakId.sakId}",
            onSuccess = { resource -> deserialize(resource.response!!.toJson()) },
            brukerTokenInfo = brukerTokenInfo,
        )

    override suspend fun distribuerBrev(
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): BestillingsIdDto =
        post(
            url = "$resourceUrl/api/brev/$brevId/distribuer?sakId=${sakId.sakId}",
            onSuccess = { resource -> deserialize(resource.response!!.toJson()) },
            brukerTokenInfo = brukerTokenInfo,
        )

    override suspend fun hentBrev(
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev =
        get(
            url = "$resourceUrl/api/brev/$brevId?sakId=${sakId.sakId}",
            onSuccess = { resource -> deserialize(resource.response!!.toJson()) },
            brukerTokenInfo = brukerTokenInfo,
        )

    override suspend fun hentVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev? =
        get(
            url = "$resourceUrl/api/brev/behandling/$behandlingId/vedtak",
            onSuccess = { resource -> resource.response?.let { deserialize(it.toJson()) } },
            brukerTokenInfo = brukerTokenInfo,
        )

    override suspend fun hentOversendelsesbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev? =
        get(
            url = "$resourceUrl/api/brev/behandling/$behandlingId/oversendelse",
            onSuccess = { resource -> resource.response?.let { deserialize(it.toJson()) } },
            brukerTokenInfo = brukerTokenInfo,
        )

    override suspend fun slettVedtaksbrev(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        downstreamResourceClient
            .delete(
                resource = Resource(clientId = clientId, url = "$resourceUrl/api/brev/behandling/$klageId/vedtak"),
                brukerTokenInfo = brukerTokenInfo,
                postBody = "",
            ).mapError { error -> throw error }
    }

    override suspend fun journalfoerNotatKa(
        klage: Klage,
        brukerInfoToken: BrukerTokenInfo,
    ): OpprettJournalpostDto =
        post(
            url = "$resourceUrl/api/notat/sak/${klage.sak.id}/journalfoer",
            postBody = mapOf("data" to KlageNotatRequest(klage)),
            onSuccess = { response -> deserialize(response.response!!.toJson()) },
            brukerTokenInfo = brukerInfoToken,
        )

    override suspend fun slettOversendelsesbrev(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        downstreamResourceClient
            .delete(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/brev/behandling/$klageId/oversendelse",
                    ),
                brukerTokenInfo = brukerTokenInfo,
                postBody = "",
            ).mapError { error -> throw error }
    }

    override suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pdf =
        get(
            url = "$resourceUrl/api/brev/behandling/$behandlingId/vedtak/pdf?brevId=$brevID",
            onSuccess = { resource ->
                resource.response?.let { deserialize(it.toJson()) }
                    ?: throw InternfeilException("Feil ved generering av pdf vedtaksbrev")
            },
            brukerTokenInfo = brukerTokenInfo,
            timeoutConfig = {
                socketTimeoutMillis = Duration.ofSeconds(30).toMillis()
                requestTimeoutMillis = Duration.ofSeconds(30).toMillis()
            },
        )

    override suspend fun tilbakestillVedtaksbrev(
        brevID: BrevID,
        behandlingId: UUID,
        sakId: SakId,
        brevtype: Brevtype,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevPayload =
        put(
            url = "$resourceUrl/api/brev/behandling/$behandlingId/payload/tilbakestill",
            onSuccess = { resource ->
                resource.response?.let { deserialize(it.toJson()) }
                    ?: throw InternfeilException("Feil ved tilbakestilling av pdf vedtaksbrev")
            },
            putBody =
                ResetPayloadRequest(
                    brevId = brevID,
                    sakId = sakId,
                    brevtype = brevtype,
                ),
            brukerTokenInfo = brukerTokenInfo,
        )

    override suspend fun fjernFerdigstillingTilbakekreving(
        tilbakekrevingId: UUID,
        sakId: SakId,
        systembruker: Systembruker,
    ) {
        post(
            url = "$resourceUrl/api/brev/behandling/$tilbakekrevingId/tilbakekreving/fjern-ferdigstilt",
            postBody = { },
            onSuccess = { },
            brukerTokenInfo = systembruker,
        )
    }

    override suspend fun hentPdf(
        sakId: SakId,
        brevID: BrevID,
        bruker: BrukerTokenInfo,
    ): Pdf {
        // Vi kan ikke gjenbruke standard get her, siden vi trenger å potensielt lese ut
        // bytes fra responsen og det er suspend -- som brekker når vi sender inn en suspend
        // som parameter til mapBoth på result
        val resourceResult =
            downstreamResourceClient.get(
                resource = Resource(clientId = clientId, url = "$resourceUrl/api/brev/$brevID/pdf?sakId=$sakId"),
                brukerTokenInfo = bruker,
            )
        return resourceResult.get()?.let { resource ->
            when (val body = resource.response) {
                is ByteArray -> Pdf(body)
                is HttpResponse -> Pdf(body.bodyAsBytes())
                else -> null
            }
        } ?: throw InternfeilException(
            "Kunne ikke lese ut Pdf-respons som et bytearray " +
                "i henting av brev med id=$brevID i sak=$sakId",
        )
    }

    private suspend fun <T> post(
        url: String,
        postBody: Any = Unit,
        onSuccess: (Resource) -> T,
        brukerTokenInfo: BrukerTokenInfo,
        timeoutConfig: (HttpTimeoutConfig.() -> Unit)? = null,
    ): T =
        downstreamResourceClient
            .post(
                resource = Resource(clientId = clientId, url = url),
                brukerTokenInfo = brukerTokenInfo,
                postBody = postBody,
                timeoutConfig = timeoutConfig,
            ).mapBoth(
                success = onSuccess,
                failure = { errorResponse -> throw errorResponse },
            )

    private suspend fun <T> get(
        url: String,
        onSuccess: (Resource) -> T,
        brukerTokenInfo: BrukerTokenInfo,
        timeoutConfig: (HttpTimeoutConfig.() -> Unit)? = null,
        additionalHeaders: Map<String, String>? = null,
    ): T =
        downstreamResourceClient
            .get(
                resource = Resource(clientId = clientId, url = url, additionalHeaders = additionalHeaders),
                brukerTokenInfo = brukerTokenInfo,
                timeoutConfig = timeoutConfig,
            ).mapBoth(
                success = onSuccess,
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )

    private suspend fun <T> put(
        url: String,
        putBody: Any = Unit,
        onSuccess: (Resource) -> T,
        brukerTokenInfo: BrukerTokenInfo,
    ): T =
        downstreamResourceClient
            .put(
                resource = Resource(clientId = clientId, url = url),
                brukerTokenInfo = brukerTokenInfo,
                putBody = putBody,
            ).mapBoth(
                success = onSuccess,
                failure = { errorResponse -> throw errorResponse },
            )
}

data class KlageNotatRequest(
    val klage: Klage,
) {
    val type = "KLAGE_BLANKETT"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpprettJournalpostDto(
    val journalpostId: String,
)

private data class ResetPayloadRequest(
    val brevId: Long,
    val sakId: SakId,
    val brevtype: Brevtype,
)
