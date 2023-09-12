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

interface GrunnlagKlient {
    suspend fun hentGrunnlag(sakId: Long): Grunnlag?
    suspend fun hentAlleSakIder(fnr: String): Set<Long>
    suspend fun hentPersonSakOgRolle(fnr: String): PersonMedSakerOgRoller
    suspend fun leggInnNyttGrunnlag(opplysningsbehov: Opplysningsbehov)
    suspend fun hentPersongalleri(sakId: Long): Grunnlagsopplysning<Persongalleri>?
    suspend fun lagreNyeSaksopplysninger(sakId: Long, saksopplysninger: NyeSaksopplysninger)
}

class GrunnlagKlientImpl(
    config: Config,
    private val grunnlagHttpClient: HttpClient
) : GrunnlagKlient {

    private val url = config.getString("grunnlag.resource.url")

    override suspend fun leggInnNyttGrunnlag(opplysningsbehov: Opplysningsbehov) {
        return grunnlagHttpClient
            .post("$url/grunnlag/person/oppdater-grunnlag") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(opplysningsbehov)
            }.body()
    }

    override suspend fun lagreNyeSaksopplysninger(sakId: Long, saksopplysninger: NyeSaksopplysninger) {
        return grunnlagHttpClient
            .post("$url/grunnlag/$sakId/nye-opplysninger") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(saksopplysninger)
            }.body()
    }

    override suspend fun hentGrunnlag(sakId: Long): Grunnlag? {
        return grunnlagHttpClient.get("$url/grunnlag/$sakId") {
            accept(ContentType.Application.Json)
        }.body()
    }

    override suspend fun hentPersongalleri(sakId: Long): Grunnlagsopplysning<Persongalleri>? {
        return grunnlagHttpClient.get("$url/grunnlag/$sakId/${Opplysningstype.PERSONGALLERI_V1}") {
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