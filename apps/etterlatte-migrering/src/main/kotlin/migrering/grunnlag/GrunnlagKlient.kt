package no.nav.etterlatte.migrering.grunnlag

import com.typesafe.config.Config
import grunnlag.VurdertBostedsland
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.FoedselsnummerDTO

class GrunnlagKlient(
    config: Config,
    private val grunnlagHttpClient: HttpClient,
) {
    private val url = config.getString("grunnlag.resource.url")

    suspend fun hentBostedslandBostedsland(fnr: String): VurdertBostedsland {
        val post =
            grunnlagHttpClient.post("$url/grunnlag/person/bostedsland") {
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
