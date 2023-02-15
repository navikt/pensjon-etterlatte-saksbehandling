package no.nav.etterlatte.vilkaarsvurderingasynk

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Omberegningshendelse

class VilkaarsvurderingService(
    private val behandling_app: HttpClient,
    private val url: String
) {
    fun opprettOmberegning(omberegningshendelse: Omberegningshendelse): HttpResponse = runBlocking {
        behandling_app.post("$url/api/vilkaarsvurdering/omberegning") {
            contentType(ContentType.Application.Json)
            setBody(omberegningshendelse)
        }
    }
}