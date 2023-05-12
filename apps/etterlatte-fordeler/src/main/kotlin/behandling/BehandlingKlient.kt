package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.FoedselsNummerMedGradering
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering

class BehandlingKlient(
    private val httpClient: HttpClient,
    private val url: String
) {
    suspend fun hentSak(fnr: String, sakType: SakType, gradering: AdressebeskyttelseGradering?): Long {
        return httpClient.post("$url/personer/saker/$sakType") {
            contentType(ContentType.Application.Json)
            setBody(FoedselsNummerMedGradering(fnr, gradering))
            header(X_CORRELATION_ID, getXCorrelationId())
            header("Nav_Call_Id", getXCorrelationId())
        }.body<ObjectNode>()["id"].longValue()
    }
}