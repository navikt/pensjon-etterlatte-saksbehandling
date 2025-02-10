package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyePersonopplysninger
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

/*
* Midlertidig proxy som går mot etterlatte-grunnlag
*
* Når databasen er overført fra grunnlag til behandling kan denne fjernes og vi kan ta i bruk [GrunnlagService],
*/
@Deprecated("Slå sammen med [GrunnlagServiceImpl]", ReplaceWith("GrunnlagServiceImpl"))
class TempGrunnlagServiceProxy(
    config: Config,
    httpClient: HttpClient,
) : GrunnlagService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val resourceUrl = config.getString("grunnlag.resource.url")

    private val url = "$resourceUrl/api"

    override suspend fun grunnlagFinnesForSak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun hentGrunnlagAvType(
        behandlingId: UUID,
        opplysningstype: Opplysningstype,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlagsopplysning<JsonNode>? {
        TODO("Not yet implemented")
    }

    override suspend fun lagreNyeSaksopplysninger(
        sakId: SakId,
        behandlingId: UUID,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        logger.info("Lagrer nye saksopplysninger på sak $sakId")

        downstreamResourceClient
            .post(
                Resource(clientId, "$url/grunnlag/behandling/$behandlingId/nye-opplysninger"),
                brukerTokenInfo,
                postBody = NyeSaksopplysninger(sakId, nyeOpplysninger),
            ).mapBoth(
                success = {
                    logger.info("Saksopplysninger lagret på sak $sakId")
                },
                failure = { throw it },
            )
    }

    override suspend fun lagreNyeSaksopplysningerBareSak(
        sakId: SakId,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun lagreNyePersonopplysninger(
        sakId: SakId,
        behandlingId: UUID,
        fnr: Folkeregisteridentifikator,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        logger.info("Lagrer nye personopplysninger på sak $sakId")

        downstreamResourceClient
            .post(
                Resource(clientId, "$url/grunnlag/person/behandling/$behandlingId/nye-opplysninger"),
                brukerTokenInfo,
                postBody = NyePersonopplysninger(sakId, fnr, nyeOpplysninger),
            ).mapBoth(
                success = {
                    logger.info("Personopplysninger lagret på sak $sakId")
                },
                failure = { throw it },
            )
    }

    override suspend fun hentOpplysningsgrunnlagForSak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag? {
        TODO("Not yet implemented")
    }

    override suspend fun hentPersongalleri(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Persongalleri? {
        TODO("Not yet implemented")
    }

    override suspend fun hentOpplysningsgrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag? {
        TODO("Not yet implemented")
    }

    override suspend fun hentPersonopplysninger(
        behandlingId: UUID,
        sakstype: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): PersonopplysningerResponse {
        TODO("Not yet implemented")
    }

    override suspend fun hentSakerOgRoller(
        fnr: Folkeregisteridentifikator,
        brukerTokenInfo: BrukerTokenInfo,
    ): PersonMedSakerOgRoller {
        TODO("Not yet implemented")
    }

    override suspend fun laasVersjonForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        logger.info("Låser grunnlagsversjon for behandling (id=$behandlingId)")

        downstreamResourceClient
            .post(
                Resource(clientId, "$url/grunnlag/behandling/$behandlingId/laas"),
                brukerTokenInfo,
                postBody = { },
            ).mapBoth(
                success = {
                    logger.info("Grunnlagsversjon for behandling ble låst (behandlingId=$behandlingId)")
                },
                failure = { throw it },
            )
    }

    override suspend fun hentAlleSakerForFnr(
        fnr: Folkeregisteridentifikator,
        brukerTokenInfo: BrukerTokenInfo,
    ): Set<SakId> {
        TODO("Not yet implemented")
    }

    override suspend fun hentPersonerISak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Map<Folkeregisteridentifikator, PersonMedNavn>? {
        TODO("Not yet implemented")
    }

    override suspend fun opprettGrunnlag(
        behandlingId: UUID,
        opplysningsbehov: Opplysningsbehov,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun oppdaterGrunnlagForSak(
        oppdaterGrunnlagRequest: OppdaterGrunnlagRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun opprettEllerOppdaterGrunnlagForSak(
        sakId: SakId,
        opplysningsbehov: Opplysningsbehov,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        sakId: SakId,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun hentHistoriskForeldreansvar(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlagsopplysning<JsonNode>? {
        TODO("Not yet implemented")
    }

    override suspend fun hentPersongalleriSamsvar(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): PersongalleriSamsvar {
        TODO("Not yet implemented")
    }

    override suspend fun laasTilVersjonForBehandling(
        skalLaasesId: UUID,
        idLaasesTil: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BehandlingGrunnlagVersjon {
        TODO("Not yet implemented")
    }
}
