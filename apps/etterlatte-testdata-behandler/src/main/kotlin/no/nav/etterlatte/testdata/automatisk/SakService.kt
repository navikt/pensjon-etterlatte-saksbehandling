package no.nav.etterlatte.testdata.automatisk

import io.ktor.client.HttpClient
import io.ktor.client.request.get

class SakService(private val klient: HttpClient, private val url: String) {
    suspend fun hentSak(sakId: Long) = klient.get("$url/saker/$sakId")
}
