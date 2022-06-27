
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import journalpost.JournalpostServiceMock
import no.nav.etterlatte.BrevService
import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.apiModule
import no.nav.etterlatte.brevRoute
import no.nav.etterlatte.db.BrevRepository
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import pdf.PdfGeneratorKlient
import journalpost.JournalpostClient
import vedtak.VedtakServiceMock

class ApplicationBuilder {
    private val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }
    private val localDevelopment: Boolean = env["BREV_LOCAL_DEV"].toBoolean()
    private val pdfGenerator = PdfGeneratorKlient(pdfHttpClient(), env["ETTERLATTE_PDFGEN_URL"]!!)
    private val vedtakService = VedtakServiceMock()
    private val datasourceBuilder = DataSourceBuilder(env)
    private val db = BrevRepository.using(datasourceBuilder.dataSource)
    private val brevService = BrevService(db, pdfGenerator, vedtakService, ::sendToRapid)
    private val journalpostService = JournalpostClient(pdfHttpClient(), env["HENT_DOKUMENT_URL"]!!) // TODO: Oppdater readme med env

    private val journalpostServiceMock = JournalpostServiceMock();

    private val rapidsConnection: RapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
            .withKtorModule { apiModule(localDevelopment) {
                brevRoute(brevService, journalpostServiceMock)
            } }
            .build()
            .apply {
                register(object : RapidsConnection.StatusListener {
                    override fun onStartup(rapidsConnection: RapidsConnection) {
                        datasourceBuilder.migrate()
                    }
                })
                OppdaterDistribusjonStatus(this, db)
            }

    private fun sendToRapid(message: String) = rapidsConnection.publish(message)

    fun start() = rapidsConnection.start()

    private fun pdfHttpClient() = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = JacksonSerializer(objectMapper)
        }
        defaultRequest {
            header(X_CORRELATION_ID, getCorrelationId())
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }
}
