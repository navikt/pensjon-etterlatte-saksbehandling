package no.nav.etterlatte

import JournalpostServiceMock
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.json.*
import journalpost.JournalpostService
import journalpost.JournalpostServiceImpl
import no.nav.etterlatte.journalpost.JournalpostKlient
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.security.ktor.clientCredential

class AppBuilder(private val props: Map<String, String>) {
    val journalpostService: JournalpostService = if (props["BREV_LOCAL_DEV"].toBoolean()) {
        JournalpostServiceMock()
    } else {
        val journalpostKlient = JournalpostKlient(journalpostHttpKlient(), requireNotNull(props["JOURNALPOST_URL"]))
        JournalpostServiceImpl(journalpostKlient)
    }

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
