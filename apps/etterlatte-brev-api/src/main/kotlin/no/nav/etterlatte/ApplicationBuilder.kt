package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.jackson.jackson
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.brev.brevRoute
import no.nav.etterlatte.db.BrevRepository
import no.nav.etterlatte.grunnbeloep.GrunnbeloepKlient
import no.nav.etterlatte.journalpost.JournalpostClient
import no.nav.etterlatte.journalpost.JournalpostServiceMock
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.norg2.Norg2Klient
import no.nav.etterlatte.pdf.PdfGeneratorKlient
import no.nav.etterlatte.vedtak.VedtakServiceMock
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

class ApplicationBuilder {
    private val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }
    private val localDevelopment: Boolean = env["BREV_LOCAL_DEV"].toBoolean()
    private val pdfGenerator = PdfGeneratorKlient(httpClient(), env["ETTERLATTE_PDFGEN_URL"]!!)
    private val vedtakService = VedtakServiceMock()
    private val grunnbeloepKlient = GrunnbeloepKlient(httpClient())
    private val norg2Klient = Norg2Klient(env["NORG2_URL"]!!, httpClient())
    private val datasourceBuilder = DataSourceBuilder(env)
    private val db = BrevRepository.using(datasourceBuilder.dataSource)
    private val brevService = BrevService(
        db,
        pdfGenerator,
        vedtakService,
        norg2Klient,
        grunnbeloepKlient,
        ::sendToRapid
    )
    private val journalpostService = if (localDevelopment) {
        JournalpostServiceMock()
    } else {
        JournalpostClient(
            httpClient(),
            env["HENT_DOKUMENT_URL"]!!,
            env["SAF_GRAPHQL_URL"]!!
        )
    }

    private val rapidsConnection: RapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
            .withKtorModule {
                apiModule(localDevelopment) {
                    brevRoute(brevService, journalpostService)
                }
            }
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

    private fun httpClient() = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            jackson { objectMapper }
        }
        defaultRequest {
            header(X_CORRELATION_ID, getCorrelationId())
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }
}