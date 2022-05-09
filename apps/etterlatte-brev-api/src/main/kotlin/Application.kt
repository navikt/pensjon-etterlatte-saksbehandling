package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import journalpost.JournalpostService
import no.nav.etterlatte.db.BrevRepository
import no.nav.etterlatte.journalpost.JournalpostKlient
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.security.ktor.clientCredential
import pdf.PdfGeneratorKlient

class ApplicationContext {
    private val conf: Config = ConfigFactory.load()
    val localDevelopment: Boolean = conf.getBoolean("localDevelopment")

    private val pdfGenerator = PdfGeneratorKlient(pdfHttpClient(), conf.getString("pdfgen.url"))
    private val journalpostKlient = JournalpostKlient(journalpostHttpKlient(), conf.getString("journalpost.url"))
    private val journalpostService = JournalpostService(journalpostKlient)
    private val datasourceBuilder = DataSourceBuilder(System.getenv())
    private val db = BrevRepository.using(datasourceBuilder.dataSource)

    val brevService = BrevService(db, pdfGenerator, journalpostService)

    init {
        datasourceBuilder.migrate()
    }

    private fun pdfHttpClient() = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = JacksonSerializer(objectMapper)
        }
        defaultRequest {
            header(X_CORRELATION_ID, getCorrelationId())
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

    private fun journalpostHttpKlient() = HttpClient(OkHttp) {
        install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }

        if (!localDevelopment) {
            install(Auth) {
                clientCredential {
                    config = System.getenv().toMutableMap().apply {
                        put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(conf.getString("journalpost.scope")))
                    }
                }
            }
        }
    }.also {
        Runtime.getRuntime().addShutdownHook(Thread { it.close() })
    }
}

fun main() {
    ApplicationContext()
        .also { Server(it).run() }
}
