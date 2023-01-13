package no.nav.etterlatte.grunnlagsendring

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag

interface GrunnlagClient {

    suspend fun hentGrunnlag(sakId: Long): Grunnlag?
}

class GrunnlagClientImpl(private val grunnlagHttpClient: HttpClient) : GrunnlagClient {

    override suspend fun hentGrunnlag(sakId: Long): Grunnlag? {
        println("kun endring for test")
        return grunnlagHttpClient.get("grunnlag/$sakId") {
            accept(ContentType.Application.Json)
        }.body()
    }
}