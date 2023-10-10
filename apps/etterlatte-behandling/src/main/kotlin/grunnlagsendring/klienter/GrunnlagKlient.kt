package no.nav.etterlatte.grunnlagsendring.klienter

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import java.util.UUID

interface GrunnlagKlient {
    suspend fun hentGrunnlag(behandlingId: UUID): Grunnlag?

    suspend fun hentAlleSakIder(fnr: String): Set<Long>

    suspend fun hentPersonSakOgRolle(fnr: String): PersonMedSakerOgRoller

    suspend fun leggInnNyttGrunnlag(
        behandlingId: UUID,
        opplysningsbehov: Opplysningsbehov,
    )

    suspend fun hentPersongalleri(behandlingId: UUID): Grunnlagsopplysning<Persongalleri>?

    suspend fun lagreNyeSaksopplysninger(
        behandlingId: UUID,
        saksopplysninger: NyeSaksopplysninger,
    )

    suspend fun hentGrunnlagForSak(sakId: Long): Grunnlag?
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
            .post("$url/grunnlag/behandlingId/$behandlingId/oppdater-grunnlag") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(opplysningsbehov)
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

    override suspend fun hentGrunnlag(behandlingId: UUID): Grunnlag? {
        return grunnlagHttpClient.get("$url/grunnlag/behandling/$behandlingId") {
            accept(ContentType.Application.Json)
        }.body()
    }

    override suspend fun hentGrunnlagForSak(sakId: Long): Grunnlag? {
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
}
