package no.nav.etterlatte

import io.ktor.client.HttpClient
import no.nav.etterlatte.enhetsregister.EnhetsregKlient
import no.nav.etterlatte.enhetsregister.EnhetsregService
import no.nav.etterlatte.libs.common.requireEnvValue
import no.nav.etterlatte.libs.ktor.httpClient

class ApplicationContext {
    private val env = System.getenv()

    private val httpClient: HttpClient by lazy {
        httpClient(forventSuksess = true)
    }

    val service = EnhetsregService(EnhetsregKlient(env.requireEnvValue("BRREG_URL"), httpClient))
}

fun main() {
    ApplicationContext().also {
        Server(it).run()
    }
}