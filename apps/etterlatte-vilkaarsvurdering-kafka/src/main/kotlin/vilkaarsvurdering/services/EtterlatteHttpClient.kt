package no.nav.etterlatte.vilkaarsvurdering.services

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post

/*
Denne klassa fins for å kunne skrive testar på HttpClient-kall
Utan denne støter vi i skrivande stund på https://github.com/mockk/mockk/issues/944
Når denne er fiksa, kan vi gjerne gå over til å bruke HttpClient direkte og så slette denne klassa,
gitt at vi ikkje da støter på https://github.com/mockk/mockk/issues/55 og dens like.
 */
class EtterlatteHttpClient(private val httpClient: HttpClient) {

    suspend fun post(urlString: String, block: HttpRequestBuilder.() -> Unit = {}) = httpClient.post(urlString, block)
}