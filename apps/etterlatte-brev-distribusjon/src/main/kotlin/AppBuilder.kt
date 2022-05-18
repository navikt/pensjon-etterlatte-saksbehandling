package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import journalpost.JournalpostService
import no.nav.etterlatte.journalpost.JournalpostKlient
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.security.ktor.clientCredential

class AppBuilder(private val props: Map<String, String>) {

    private val journalpostKlient =
        JournalpostKlient(journalpostHttpKlient(), requireNotNull(props["JOURNALPOST_URL"]))
    val journalpostService = JournalpostService(journalpostKlient)

    private fun journalpostHttpKlient() = HttpClient(OkHttp) {
        install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }

        install(Auth) {
            clientCredential {
                config = props.toMutableMap()
                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("AZURE_DOKARKIV_SCOPE"))) }
            }
        }
    }.also {
        Runtime.getRuntime().addShutdownHook(Thread { it.close() })
    }
}
