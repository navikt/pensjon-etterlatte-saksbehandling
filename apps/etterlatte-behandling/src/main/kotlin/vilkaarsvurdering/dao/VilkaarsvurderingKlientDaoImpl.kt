package no.nav.etterlatte.vilkaarsvurdering.dao

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vilkaarsvurdering.OppdaterVurdertVilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.StatusOppdatertDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.vilkaarsvurdering.VurdertVilkaarsvurderingDto
import no.nav.etterlatte.vilkaarsvurdering.MigrertYrkesskadefordel
import no.nav.etterlatte.vilkaarsvurdering.OpprettVilkaarsvurderingFraBehandling
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

interface VilkaarsvurderingKlientDao {
    suspend fun hent(behandlingId: UUID): Vilkaarsvurdering?

    suspend fun erMigrertYrkesskadefordel(
        behandlingId: UUID,
        sakId: SakId,
    ): MigrertYrkesskadefordel

    suspend fun opprettVilkaarsvurdering(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering

    suspend fun kopierVilkaarsvurdering(vilkaarsvurdering: OpprettVilkaarsvurderingFraBehandling): Vilkaarsvurdering

    suspend fun slettVilkaarsvurderingResultat(behandlingId: UUID): Vilkaarsvurdering

    suspend fun lagreVilkaarsvurderingResultatvanlig(
        behandlingId: UUID,
        vurdertVilkaarsvurderingDto: VurdertVilkaarsvurderingDto,
    ): Vilkaarsvurdering

    suspend fun slettVilkaarsvurdering(
        behandlingId: UUID,
        vilkaarsvurderingId: UUID,
    ): Vilkaarsvurdering

    suspend fun oppdaterGrunnlagsversjon(
        behandlingId: UUID,
        grunnlagVersjon: Long,
    ): StatusOppdatertDto

    suspend fun slettVurderingPaaVilkaar(
        behandlingId: UUID,
        vilkaarId: UUID,
    ): Vilkaarsvurdering

    suspend fun oppdaterVurderingPaaVilkaar(oppdatervv: OppdaterVurdertVilkaar): Vilkaarsvurdering
}

class VilkaarsvurderingKlientDaoImpl(
    config: Config,
    httpClient: HttpClient,
) : VilkaarsvurderingKlientDao {
    private val clientId = config.getString("vilkaarsvurdering.client.id")
    private val resourceUrl = config.getString("vilkaarsvurdering.resource.url")
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun hent(behandlingId: UUID): Vilkaarsvurdering? =
        downstreamResourceClient
            .get(
                resource = Resource(clientId = clientId, url = "$resourceUrl/api/vilkaarsvurdering/$behandlingId"),
                brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
            ).mapBoth(
                success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )

    override suspend fun erMigrertYrkesskadefordel(
        behandlingId: UUID,
        sakId: SakId,
    ): MigrertYrkesskadefordel =
        downstreamResourceClient
            .get(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/vilkaarsvurdering/$behandlingId/migrert-yrkesskadefordel/$sakId",
                    ),
                brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
            ).mapBoth(
                success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )

    override suspend fun opprettVilkaarsvurdering(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering =
        downstreamResourceClient
            .post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/vilkaarsvurdering/${vilkaarsvurdering.behandlingId}/opprett",
                    ),
                brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                postBody = vilkaarsvurdering.toJson(),
            ).mapBoth(
                success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )

    override suspend fun kopierVilkaarsvurdering(vilkaarsvurdering: OpprettVilkaarsvurderingFraBehandling): Vilkaarsvurdering =
        downstreamResourceClient
            .post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/vilkaarsvurdering/${vilkaarsvurdering.vilkaarsvurdering.behandlingId}/kopier",
                    ),
                brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                postBody = vilkaarsvurdering.toJson(),
            ).mapBoth(
                success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )

    override suspend fun slettVilkaarsvurderingResultat(behandlingId: UUID): Vilkaarsvurdering =
        downstreamResourceClient
            .delete(
                resource = Resource(clientId = clientId, url = "$resourceUrl/api/vilkaarsvurdering/resultat/$behandlingId"),
                brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
            ).mapBoth(
                success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )

    override suspend fun lagreVilkaarsvurderingResultatvanlig(
        behandlingId: UUID,
        vurdertVilkaarsvurderingDto: VurdertVilkaarsvurderingDto,
    ): Vilkaarsvurdering =
        downstreamResourceClient
            .post(
                resource = Resource(clientId = clientId, url = "$resourceUrl/api/vilkaarsvurdering/resultat/$behandlingId"),
                brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                postBody = vurdertVilkaarsvurderingDto,
            ).mapBoth(
                success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )

    override suspend fun oppdaterVurderingPaaVilkaar(oppdatervv: OppdaterVurdertVilkaar): Vilkaarsvurdering =
        downstreamResourceClient
            .post(
                resource = Resource(clientId = clientId, url = "$resourceUrl/api/vilkaarsvurdering/${oppdatervv.behandlingId}"),
                brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                postBody = oppdatervv.toJson(),
            ).mapBoth(
                success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )

    override suspend fun slettVurderingPaaVilkaar(
        behandlingId: UUID,
        vilkaarId: UUID,
    ): Vilkaarsvurdering =
        downstreamResourceClient
            .delete(
                resource = Resource(clientId = clientId, url = "$resourceUrl/api/vilkaarsvurdering/$behandlingId/$vilkaarId"),
                brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
            ).mapBoth(
                success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )

    override suspend fun oppdaterGrunnlagsversjon(
        behandlingId: UUID,
        grunnlagVersjon: Long,
    ): StatusOppdatertDto =
        downstreamResourceClient
            .post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/vilkaarsvurdering/$behandlingId/oppdater-status/$grunnlagVersjon",
                    ),
                brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                postBody = "",
            ).mapBoth(
                success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )

    override suspend fun slettVilkaarsvurdering(
        behandlingId: UUID,
        vilkaarsvurderingId: UUID,
    ): Vilkaarsvurdering =
        downstreamResourceClient
            .delete(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/vilkaarsvurdering/$behandlingId/$vilkaarsvurderingId",
                    ),
                brukerTokenInfo = Kontekst.get().brukerTokenInfo!!,
                postBody = "",
            ).mapBoth(
                success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )
}
