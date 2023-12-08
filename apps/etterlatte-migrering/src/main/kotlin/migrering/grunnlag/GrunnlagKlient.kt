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
import no.nav.etterlatte.libs.common.person.BrevMottaker

class GrunnlagKlient(
    config: Config,
    private val grunnlagHttpClient: HttpClient,
) {
    private val url = config.getString("grunnlag.resource.url")

    suspend fun hentBostedsland(fnr: String): VurdertBostedsland {
        val post =
            grunnlagHttpClient.post("$url/grunnlag/migrering/bostedsland") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
            }
        if (post.status == HttpStatusCode.NotFound) {
            return VurdertBostedsland.finsIkkeIPDL
        }
        return post.body()
    }

    suspend fun hentVergesAdresse(fnr: String): BrevMottaker? {
        val post =
            grunnlagHttpClient.post("$url/grunnlag/person/vergeadresse") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
            }
        if (post.status == HttpStatusCode.NotFound) {
            return null
        }
        return post.body()
    }
}
