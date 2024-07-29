package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

interface GrunnlagKlient {
    suspend fun finnPersonOpplysning(
        behandlingId: UUID,
        opplysningsType: Opplysningstype,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlagsopplysning<Person>?

    suspend fun hentPersongalleri(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlagsopplysning<Persongalleri>?

    suspend fun hentGrunnlagForSak(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag

    suspend fun hentGrunnlagForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag
}

class GrunnlagKlientException(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)

class GrunnlagKlientObo(
    config: Config,
    httpClient: HttpClient,
) : GrunnlagKlient {
    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val resourceApiUrl = config.getString("grunnlag.resource.url").plus("/api")

    override suspend fun finnPersonOpplysning(
        behandlingId: UUID,
        opplysningsType: Opplysningstype,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlagsopplysning<Person>? {
        try {
            logger.info("Henter opplysning ($opplysningsType) fra grunnlag for behandling med id=$behandlingId")

            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceApiUrl/grunnlag/behandling/$behandlingId/$opplysningsType",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> resource.response?.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse ->
                        if (errorResponse is ResponseException && errorResponse.response.status == HttpStatusCode.NotFound) {
                            return null
                        } else {
                            throw errorResponse
                        }
                    },
                )
        } catch (e: Exception) {
            throw GrunnlagKlientException(
                "Henting av opplysning ($opplysningsType) fra grunnlag for behandling med id=$behandlingId feilet",
                e,
            )
        }
    }

    override suspend fun hentPersongalleri(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlagsopplysning<Persongalleri>? {
        try {
            logger.info("Henter persongalleri fra grunnlag for behandling med id=$behandlingId")

            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceApiUrl/grunnlag/behandling/$behandlingId/${Opplysningstype.PERSONGALLERI_V1}",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> resource.response?.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw GrunnlagKlientException(
                "Henting av persongalleri fra grunnlag for behandling med id=$behandlingId feilet",
                e,
            )
        }
    }

    override suspend fun hentGrunnlagForSak(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag {
        try {
            logger.info("Henter grunnlag for sak med id=$sakId")

            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceApiUrl/grunnlag/sak/$sakId",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> resource.response!!.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw GrunnlagKlientException(
                "Henting av grunnlag for sak med id=$sakId feilet",
                e,
            )
        }
    }

    override suspend fun hentGrunnlagForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag {
        try {
            logger.info("Henter grunnlag for behandling med id=$behandlingId")

            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceApiUrl/grunnlag/behandling/$behandlingId",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> resource.response!!.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw GrunnlagKlientException(
                "Henting av grunnlag for behandling med id=$behandlingId feilet",
                e,
            )
        }
    }
}
