package no.nav.etterlatte.grunnlagsendring.klienter

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
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag

interface GrunnlagKlient {

    suspend fun hentGrunnlag(sakId: Long): Grunnlag?
    suspend fun hentAlleSakIder(fnr: String): Set<Long>
    suspend fun hentPersonSakOgRolle(fnr: String): PersonMedSakerOgRoller
}

class GrunnlagKlientImpl(
    private val grunnlagHttpClient: HttpClient,
    private val url: String
) : GrunnlagKlient {

    override suspend fun hentGrunnlag(sakId: Long): Grunnlag? {
        return grunnlagHttpClient.get("$url/api/grunnlag/$sakId") {
            accept(ContentType.Application.Json)
        }.body()
    }

    override suspend fun hentAlleSakIder(fnr: String): Set<Long> {
        return grunnlagHttpClient.post("$url/api/grunnlag/person/saker") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(FoedselsnummerDTO(fnr))
        }.body()
    }

    override suspend fun hentPersonSakOgRolle(fnr: String): PersonMedSakerOgRoller {
        return grunnlagHttpClient.post("$url/api/grunnlag/person/roller") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(FoedselsnummerDTO(fnr))
        }.body()
    }
}