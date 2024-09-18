package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.grunnlag.PersonopplysningerResponse
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.ping
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

interface GrunnlagKlient : Pingable {
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
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag

    suspend fun hentGrunnlagForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag

    suspend fun hentGrunnlag(sakId: SakId): Grunnlag?

    suspend fun hentAlleSakIder(fnr: String): Set<Long>

    suspend fun hentPersonSakOgRolle(fnr: String): PersonMedSakerOgRoller

    suspend fun leggInnNyttGrunnlag(
        behandlingId: UUID,
        opplysningsbehov: Opplysningsbehov,
        brukerTokenInfo: BrukerTokenInfo? = null,
    )

    suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        request: OppdaterGrunnlagRequest,
        brukerTokenInfo: BrukerTokenInfo? = null,
    )

    suspend fun hentPersongalleri(behandlingId: UUID): Grunnlagsopplysning<Persongalleri>?

    suspend fun lagreNyeSaksopplysninger(
        behandlingId: UUID,
        saksopplysninger: NyeSaksopplysninger,
        brukerTokenInfo: BrukerTokenInfo? = null,
    )

    suspend fun lagreNyeSaksopplysningerBareSak(
        sakId: SakId,
        saksopplysninger: NyeSaksopplysninger,
        brukerTokenInfo: BrukerTokenInfo? = null,
    )

    suspend fun leggInnNyttGrunnlagSak(
        sakId: SakId,
        opplysningsbehov: Opplysningsbehov,
        brukerTokenInfo: BrukerTokenInfo? = null,
    )

    suspend fun laasTilGrunnlagIBehandling(
        id: UUID,
        forrigeBehandling: UUID,
    )

    suspend fun hentPersonopplysningerForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        sakType: SakType,
    ): PersonopplysningerResponse
}

class GrunnlagKlientException(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)

class GrunnlagKlientImpl(
    config: Config,
    httpClient: HttpClient,
) : GrunnlagKlient {
    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val resourceUrl = config.getString("grunnlag.resource.url")
    private val resourceApiUrl = resourceUrl.plus("/api")

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
        sakId: SakId,
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

    override suspend fun leggInnNyttGrunnlag(
        behandlingId: UUID,
        opplysningsbehov: Opplysningsbehov,
        brukerTokenInfo: BrukerTokenInfo?,
    ) {
        downstreamResourceClient
            .post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceApiUrl/grunnlag/behandling/$behandlingId/opprett-grunnlag",
                    ),
                postBody = opplysningsbehov.toJson(),
                brukerTokenInfo = brukerTokenInfo ?: Kontekst.get().brukerTokenInfo!!,
            ).mapBoth(
                success = { _ -> },
                failure = { errorResponse -> throw errorResponse },
            )
    }

    override suspend fun leggInnNyttGrunnlagSak(
        sakId: SakId,
        opplysningsbehov: Opplysningsbehov,
        brukerTokenInfo: BrukerTokenInfo?,
    ) {
        downstreamResourceClient
            .post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceApiUrl/grunnlag/sak/$sakId/opprett-grunnlag",
                    ),
                postBody = opplysningsbehov.toJson(),
                brukerTokenInfo = brukerTokenInfo ?: Kontekst.get().brukerTokenInfo!!,
            ).mapBoth(
                success = { _ -> },
                failure = { errorResponse -> throw errorResponse },
            )
    }

    override suspend fun laasTilGrunnlagIBehandling(
        id: UUID,
        forrigeBehandling: UUID,
    ) {
        downstreamResourceClient
            .post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceApiUrl/grunnlag/behandling/$id/laas-til-behandling/$forrigeBehandling",
                    ),
                postBody = "",
                brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
            ).mapBoth(
                success = { _ -> },
                failure = { errorResponse -> throw errorResponse },
            )
    }

    override suspend fun hentPersonopplysningerForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        sakType: SakType,
    ): PersonopplysningerResponse {
        logger.info("Henter personopplysninger for behandling med id=$behandlingId")

        return downstreamResourceClient
            .get(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceApiUrl/grunnlag/behandling/$behandlingId/personopplysninger?=sakType=$sakType",
                    ),
                brukerTokenInfo = brukerTokenInfo,
            ).mapBoth(
                success = { resource -> resource.response!!.let { objectMapper.readValue(it.toString()) } },
                failure = { errorResponse -> throw errorResponse },
            )
    }

    override suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        request: OppdaterGrunnlagRequest,
        brukerTokenInfo: BrukerTokenInfo?,
    ) {
        downstreamResourceClient
            .post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceApiUrl/grunnlag/behandling/$behandlingId/oppdater-grunnlag",
                    ),
                postBody = request.toJson(),
                brukerTokenInfo = brukerTokenInfo ?: Kontekst.get().brukerTokenInfo!!,
            ).mapBoth(
                success = { _ -> },
                failure = { errorResponse -> throw errorResponse },
            )
    }

    override suspend fun lagreNyeSaksopplysninger(
        behandlingId: UUID,
        saksopplysninger: NyeSaksopplysninger,
        brukerTokenInfo: BrukerTokenInfo?,
    ) {
        downstreamResourceClient
            .post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceApiUrl/grunnlag/behandling/$behandlingId/nye-opplysninger",
                    ),
                postBody = saksopplysninger.toJson(),
                brukerTokenInfo = brukerTokenInfo ?: Kontekst.get().brukerTokenInfo!!,
            ).mapBoth(
                success = { _ -> },
                failure = { errorResponse -> throw errorResponse },
            )
    }

    override suspend fun lagreNyeSaksopplysningerBareSak(
        sakId: SakId,
        saksopplysninger: NyeSaksopplysninger,
        brukerTokenInfo: BrukerTokenInfo?,
    ) {
        downstreamResourceClient
            .post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceApiUrl/grunnlag/sak/$sakId/nye-opplysninger",
                    ),
                postBody = saksopplysninger.toJson(),
                brukerTokenInfo = brukerTokenInfo ?: Kontekst.get().brukerTokenInfo!!,
            ).mapBoth(
                success = { _ -> },
                failure = { errorResponse -> throw errorResponse },
            )
    }

    override suspend fun hentGrunnlag(sakId: SakId): Grunnlag? =
        downstreamResourceClient
            .get(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceApiUrl/grunnlag/sak/$sakId",
                    ),
                brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
            ).mapBoth(
                success = { resource -> deserialize(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse },
            )

    override suspend fun hentPersongalleri(behandlingId: UUID): Grunnlagsopplysning<Persongalleri>? =
        downstreamResourceClient
            .get(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceApiUrl/grunnlag/behandling/$behandlingId/${Opplysningstype.PERSONGALLERI_V1}",
                    ),
                brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
            ).mapBoth(
                success = { resource -> deserialize(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse },
            )

    override suspend fun hentAlleSakIder(fnr: String): Set<Long> =
        downstreamResourceClient
            .post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceApiUrl/grunnlag/person/saker",
                    ),
                brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                postBody = FoedselsnummerDTO(fnr).toJson(),
            ).mapBoth(
                success = { resource -> deserialize(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse },
            )

    override suspend fun hentPersonSakOgRolle(fnr: String): PersonMedSakerOgRoller =
        downstreamResourceClient
            .post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceApiUrl/grunnlag/person/roller",
                    ),
                brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                postBody = FoedselsnummerDTO(fnr).toJson(),
            ).mapBoth(
                success = { resource -> deserialize(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse },
            )

    override val serviceName: String
        get() = "Grunnlagklient"
    override val beskrivelse: String
        get() = "Henter lagret grunnlag for sak eller behandling"
    override val endpoint: String
        get() = this.resourceUrl

    override suspend fun ping(konsument: String?): PingResult =
        downstreamResourceClient.ping(
            pingUrl = resourceUrl.plus("/health/isready"),
            logger = logger,
            serviceName = serviceName,
            beskrivelse = beskrivelse,
        )
}
