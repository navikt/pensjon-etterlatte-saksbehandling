package no.nav.etterlatte

import JournalpostServiceMock
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.json.*
import journalpost.JournalpostService
import journalpost.JournalpostServiceImpl
import no.nav.etterlatte.distribusjon.DistribusjonKlient
import no.nav.etterlatte.distribusjon.DistribusjonService
import no.nav.etterlatte.distribusjon.DistribusjonServiceImpl
import no.nav.etterlatte.distribusjon.DistribusjonServiceMock
import no.nav.etterlatte.journalpost.JournalpostKlient
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.security.ktor.clientCredential

class AppBuilder {
    val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }
    private val localDev = env["BREV_LOCAL_DEV"].toBoolean()

    val journalpostService: JournalpostService = when (localDev) {
        true -> JournalpostServiceMock()
        false -> {
            val klient = JournalpostKlient(httpClient("AZURE_DOKARKIV_SCOPE"), requireNotNull(env["JOURNALPOST_URL"]))
            JournalpostServiceImpl(klient)
        }
    }

    val distribusjonService: DistribusjonService = when (localDev) {
        true -> DistribusjonServiceMock()
        false -> {
            val klient = DistribusjonKlient(httpClient("DOKDISTFORDEL_SCOPE"), requireNotNull(env["DOKDISTFORDEL_URL"]))
            DistribusjonServiceImpl(klient)
        }
    }

    private fun httpClient(scope: String) = HttpClient(OkHttp) {
        install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }

        install(Auth) {
            clientCredential {
                config = env.toMutableMap()
                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get(scope))) }
            }
        }
    }.also {
        Runtime.getRuntime().addShutdownHook(Thread { it.close() })
    }
}
