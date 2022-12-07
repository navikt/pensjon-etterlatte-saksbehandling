package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.jackson.jackson
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.RegoppslagKlient
import no.nav.etterlatte.brev.behandling.SakOgBehandlingService
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.brev.adresse.MottakerService
import no.nav.etterlatte.brev.adresse.Norg2Klient
import no.nav.etterlatte.brev.brevRoute
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.db.DataSourceBuilder
import no.nav.etterlatte.brev.grunnbeloep.GrunnbeloepKlient
import no.nav.etterlatte.brev.grunnlag.GrunnlagKlient
import no.nav.etterlatte.brev.dokument.JournalpostClient
import no.nav.etterlatte.brev.dokument.dokumentRoute
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.brev.pdf.PdfGeneratorKlient
import no.nav.etterlatte.security.ktor.clientCredential
import no.nav.etterlatte.brev.behandling.VedtaksvurderingKlient
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

class ApplicationBuilder {
    private val config = ConfigFactory.load()

    private val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }
    private val pdfGenerator = PdfGeneratorKlient(httpClient(), env["ETTERLATTE_PDFGEN_URL"]!!)
    private val mottakerService = MottakerService(httpClient(), env["ETTERLATTE_BRREG_URL"]!!)
    private val regoppslagKlient = RegoppslagKlient(proxyClient(), env["ETTERLATTE_PROXY_URL"]!!)
    private val grunnlagKlient = GrunnlagKlient(config, httpClient())
    private val vedtakKlient = VedtaksvurderingKlient(config, httpClient())
    private val grunnbeloepKlient = GrunnbeloepKlient(httpClient())
    private val sakOgBehandlingService = SakOgBehandlingService(vedtakKlient, grunnlagKlient, grunnbeloepKlient)
    private val norg2Klient = Norg2Klient(env["NORG2_URL"]!!, httpClient())
    private val datasourceBuilder = DataSourceBuilder(env)
    private val db = BrevRepository.using(datasourceBuilder.dataSource)

    private val adresseService = AdresseService(norg2Klient, regoppslagKlient)

    private val brevService = BrevService(
        db,
        pdfGenerator,
        sakOgBehandlingService,
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