package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.jackson.jackson
import no.nav.etterlatte.adresse.AdresseKlient
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.brev.MottakerServiceImpl
import no.nav.etterlatte.brev.brevRoute
import no.nav.etterlatte.db.BrevRepository
import no.nav.etterlatte.grunnbeloep.GrunnbeloepKlient
import no.nav.etterlatte.grunnlag.GrunnlagKlient
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.dokument.JournalpostClient
import no.nav.etterlatte.dokument.dokumentRoute
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.norg2.Norg2Klient
import no.nav.etterlatte.pdf.PdfGeneratorKlient
import no.nav.etterlatte.security.ktor.clientCredential
import no.nav.etterlatte.vedtak.VedtakServiceImpl
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

class ApplicationBuilder {
    private val config = ConfigFactory.load()

    private val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }
    private val pdfGenerator = PdfGeneratorKlient(httpClient(), env["ETTERLATTE_PDFGEN_URL"]!!)
    private val vedtakService = VedtakServiceImpl(config, httpClient())
    private val grunnlagKlient = GrunnlagKlient(config, httpClient())
    private val grunnlagService = GrunnlagService(grunnlagKlient)
    private val grunnbeloepKlient = GrunnbeloepKlient(httpClient())
    private val norg2Klient = Norg2Klient(env["NORG2_URL"]!!, httpClient())
    private val datasourceBuilder = DataSourceBuilder(env)
    private val db = BrevRepository.using(datasourceBuilder.dataSource)

    private val mottakerService = MottakerServiceImpl(httpClient(), env["ETTERLATTE_BRREG_URL"]!!)
    private val adresseService = AdresseKlient(proxyClient(), env["ETTERLATTE_PROXY_URL"]!!)

    private val brevService = BrevService(
        db,
        pdfGenerator,
        vedtakService,
        grunnlagService,
        norg2Klient,
        grunnbeloepKlient,
        adresseService,
        ::sendToRapid
    )

    private val journalpostService = JournalpostClient(httpClient(), env["SAF_BASE_URL"]!!, env["SAF_SCOPE"]!!)

    private val rapidsConnection: RapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
            .withKtorModule {
                apiModule {
                    brevRoute(brevService, mottakerService)
                    dokumentRoute(journalpostService)
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
                FerdigstillVedtaksbrev(this, brevService)
            }

    private fun sendToRapid(message: String) = rapidsConnection.publish(message)

    fun start() = rapidsConnection.start()

    private fun httpClient() = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            jackson()
        }
        defaultRequest {
            header(X_CORRELATION_ID, getCorrelationId())
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }

    private fun proxyClient() = HttpClient(OkHttp) {
        expectSuccess = true
        val inntektsConfig = config.getConfig("no.nav.etterlatte.tjenester.proxy")
        val env = mutableMapOf(
            "AZURE_APP_CLIENT_ID" to inntektsConfig.getString("clientId"),
            "AZURE_APP_WELL_KNOWN_URL" to inntektsConfig.getString("wellKnownUrl"),
            "AZURE_APP_OUTBOUND_SCOPE" to inntektsConfig.getString("outbound"),
            "AZURE_APP_JWK" to inntektsConfig.getString("clientJwk")
        )
        install(ContentNegotiation) { jackson { objectMapper } }
        install(Auth) {
            clientCredential {
                config = env
            }
        }
        defaultRequest {
            header(X_CORRELATION_ID, getCorrelationId())
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }
}