package no.nav.etterlatte

import com.fasterxml.jackson.core.type.TypeReference
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.jackson.jackson
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.MottakerService
import no.nav.etterlatte.brev.adresse.Norg2Klient
import no.nav.etterlatte.brev.adresse.RegoppslagKlient
import no.nav.etterlatte.brev.behandling.SakOgBehandlingService
import no.nav.etterlatte.brev.beregning.BeregningKlient
import no.nav.etterlatte.brev.brevRoute
import no.nav.etterlatte.brev.db.BrevRepository
<<<<<<< HEAD
import no.nav.etterlatte.brev.db.DataSourceBuilder
import no.nav.etterlatte.brev.distribusjon.DistribusjonKlient
import no.nav.etterlatte.brev.distribusjon.DistribusjonServiceImpl
import no.nav.etterlatte.brev.dokarkiv.DokarkivKlient
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
=======
>>>>>>> 5d1900e4 (Bruker etterlatte-database i brev-api)
import no.nav.etterlatte.brev.dokument.SafClient
import no.nav.etterlatte.brev.dokument.dokumentRoute
import no.nav.etterlatte.brev.grunnlag.GrunnlagKlient
import no.nav.etterlatte.brev.navansatt.NavansattKlient
import no.nav.etterlatte.brev.pdf.PdfGeneratorKlient
import no.nav.etterlatte.brev.vedtak.VedtaksvurderingKlient
import no.nav.etterlatte.brev.vedtaksbrevRoute
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.helsesjekk.setReady
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.security.ktor.clientCredential
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ApplicationBuilder {
    private val config = ConfigFactory.load()
    val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

    init {
        sikkerLogg.info("SikkerLogg: etterlatte-brev-api oppstart")
    }

    private val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }
    private val pdfGenerator = PdfGeneratorKlient(httpClient(), env["ETTERLATTE_PDFGEN_URL"]!!)
    private val mottakerService = MottakerService(httpClient(), env["ETTERLATTE_BRREG_URL"]!!)
    private val regoppslagKlient = RegoppslagKlient(proxyClient(), env["ETTERLATTE_PROXY_URL"]!!)
    private val navansattKlient = NavansattKlient(proxyClient(), env["NAVANSATT_URL"]!!)
    private val grunnlagKlient = GrunnlagKlient(config, httpClient())
    private val vedtakKlient = VedtaksvurderingKlient(config, httpClient())
    private val beregningKlient = BeregningKlient(config, httpClient())
    private val sakOgBehandlingService = SakOgBehandlingService(
        vedtakKlient,
        grunnlagKlient,
        beregningKlient,
        saksbehandlere = getSaksbehandlere()
    )
    private val norg2Klient = Norg2Klient(env["NORG2_URL"]!!, httpClient())
    private val datasource = DataSourceBuilder.createDataSource(env)
    private val db = BrevRepository.using(datasource)

    private val adresseService = AdresseService(norg2Klient, regoppslagKlient)

    private val dokarkivKlient = DokarkivKlient(httpClient("DOKARKIV_SCOPE"), requireNotNull(env["DOKARKIV_URL"]))
    private val dokarkivService = DokarkivServiceImpl(dokarkivKlient, db)

    private val distribusjonKlient = DistribusjonKlient(httpClient("DOKDIST_SCOPE"), requireNotNull(env["DOKDIST_URL"]))
    private val distribusjonService = DistribusjonServiceImpl(distribusjonKlient, db)

    private val brevService = BrevService(db, pdfGenerator, adresseService)
    private val vedtaksbrevService =
        VedtaksbrevService(
            db,
            pdfGenerator,
            sakOgBehandlingService,
            adresseService,
            dokarkivService,
            navansattKlient
        )

    private val journalpostService = SafClient(httpClient(), env["SAF_BASE_URL"]!!, env["SAF_SCOPE"]!!)

    private val rapidsConnection: RapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
            .withKtorModule {
                restModule(sikkerLogg, routePrefix = "api") {
                    brevRoute(brevService, mottakerService)
                    vedtaksbrevRoute(vedtaksbrevService)
                    dokumentRoute(journalpostService)
                }
            }
            .build()
            .apply {
                register(object : RapidsConnection.StatusListener {
                    override fun onStartup(rapidsConnection: RapidsConnection) {
                        datasource.migrate()
                    }
                })
                JournalfoerVedtaksbrev(this, vedtaksbrevService)
                DistribuerBrev(this, distribusjonService, brevService)
            }

    private fun getSaksbehandlere(): Map<String, String> {
        val saksbehandlereSecret = env["saksbehandlere"]!!
        return objectMapper.readValue(
            saksbehandlereSecret,
            object : TypeReference<Map<String, String>>() {}
        )
    }

    private fun sendToRapid(message: String) = rapidsConnection.publish(message)

    fun start() = setReady().also { rapidsConnection.start() }

    private fun httpClient(scope: String? = null) = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            jackson()
        }
        if (scope != null) {
            install(Auth) {
                clientCredential {
                    config = env.toMutableMap()
                        .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get(scope))) }
                }
            }
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
        install(ContentNegotiation) { jackson() }
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