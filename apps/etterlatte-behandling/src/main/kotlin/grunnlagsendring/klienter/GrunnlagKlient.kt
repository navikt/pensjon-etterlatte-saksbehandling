package no.nav.etterlatte.grunnlagsendring.klienter

import com.typesafe.config.Config
import grunnlag.VurdertBostedsland
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import java.util.UUID

interface GrunnlagKlient {
    suspend fun hentGrunnlag(sakId: Long): Grunnlag?

    suspend fun hentAlleSakIder(fnr: String): Set<Long>

    suspend fun hentPersonSakOgRolle(fnr: String): PersonMedSakerOgRoller

    suspend fun leggInnNyttGrunnlag(
        behandlingId: UUID,
        opplysningsbehov: Opplysningsbehov,
    )

    suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        request: OppdaterGrunnlagRequest,
    )

    suspend fun hentPersongalleri(behandlingId: UUID): Grunnlagsopplysning<Persongalleri>?

    suspend fun lagreNyeSaksopplysninger(
        behandlingId: UUID,
        saksopplysninger: NyeSaksopplysninger,
    )

    suspend fun hentVurdertBostedsland(fnr: String): VurdertBostedsland
}

class GrunnlagKlientImpl(
    config: Config,
    private val grunnlagHttpClient: HttpClient,
) : GrunnlagKlient {
    private val url = config.getString("grunnlag.resource.url")

    override suspend fun leggInnNyttGrunnlag(
        behandlingId: UUID,
        opplysningsbehov: Opplysningsbehov,
    ) {
        return grunnlagHttpClient
            .post("$url/grunnlag/behandling/$behandlingId/opprett-grunnlag") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(opplysningsbehov)
            }.body()
    }

    override suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        request: OppdaterGrunnlagRequest,
    ) {
        return grunnlagHttpClient
            .post("$url/grunnlag/behandling/$behandlingId/oppdater-grunnlag") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
    }

    override suspend fun lagreNyeSaksopplysninger(
        behandlingId: UUID,
        saksopplysninger: NyeSaksopplysninger,
    ) {
        return grunnlagHttpClient
            .post("$url/grunnlag/behandling/$behandlingId/nye-opplysninger") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(saksopplysninger)
            }.body()
    }

    /**
     * Henter komplett grunnlag for sak
     **/
    override suspend fun hentGrunnlag(sakId: Long): Grunnlag? {
        return grunnlagHttpClient.get("$url/grunnlag/sak/$sakId") {
            accept(ContentType.Application.Json)
        }.body()
    }

    override suspend fun hentPersongalleri(behandlingId: UUID): Grunnlagsopplysning<Persongalleri>? {
        return grunnlagHttpClient.get("$url/grunnlag/behandling/$behandlingId/${Opplysningstype.PERSONGALLERI_V1}") {
            accept(ContentType.Application.Json)
        }.body()
    }

    override suspend fun hentAlleSakIder(fnr: String): Set<Long> {
        return grunnlagHttpClient.post("$url/grunnlag/person/saker") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(FoedselsnummerDTO(fnr))
        }.body()
    }

    override suspend fun hentPersonSakOgRolle(fnr: String): PersonMedSakerOgRoller {
        return grunnlagHttpClient.post("$url/grunnlag/person/roller") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(FoedselsnummerDTO(fnr))
        }.body()
    }

    override suspend fun hentVurdertBostedsland(fnr: String): VurdertBostedsland {
        val post =
            grunnlagHttpClient.post("$url/grunnlag/person/vurdertbostedsland") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
            }
        if (post.status == HttpStatusCode.NoContent) {
            return VurdertBostedsland.finsIkkeIPDL
        }
        return post.body()
    }
}
