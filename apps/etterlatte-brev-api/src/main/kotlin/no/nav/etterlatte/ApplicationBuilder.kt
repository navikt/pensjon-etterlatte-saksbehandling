package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders.XCorrelationId
import io.ktor.serialization.jackson.jackson
import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.Norg2Klient
import no.nav.etterlatte.brev.adresse.RegoppslagKlient
import no.nav.etterlatte.brev.adresse.enhetsregister.BrregKlient
import no.nav.etterlatte.brev.adresse.enhetsregister.BrregService
import no.nav.etterlatte.brev.behandling.SakOgBehandlingService
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.beregning.BeregningKlient
import no.nav.etterlatte.brev.brevRoute
import no.nav.etterlatte.brev.brevbaker.BrevbakerJSONBlockMixIn
import no.nav.etterlatte.brev.brevbaker.BrevbakerJSONParagraphMixIn
import no.nav.etterlatte.brev.brevbaker.BrevbakerKlient
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.DistribusjonKlient
import no.nav.etterlatte.brev.distribusjon.DistribusjonServiceImpl
import no.nav.etterlatte.brev.dokarkiv.DokarkivKlient
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
import no.nav.etterlatte.brev.dokument.SafClient
import no.nav.etterlatte.brev.dokument.dokumentRoute
import no.nav.etterlatte.brev.grunnlag.GrunnlagKlient
import no.nav.etterlatte.brev.navansatt.NavansattKlient
import no.nav.etterlatte.brev.vedtak.VedtaksvurderingKlient
import no.nav.etterlatte.brev.vedtaksbrevRoute
import no.nav.etterlatte.libs.common.logging.NAV_CALL_ID
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.requireEnvValue
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.rivers.DistribuerBrev
import no.nav.etterlatte.rivers.JournalfoerVedtaksbrev
import no.nav.etterlatte.rivers.VedtaksbrevUnderkjent
import no.nav.etterlatte.security.ktor.clientCredential
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.pensjon.brevbaker.api.model.RenderedJsonLetter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rapidsandrivers.getRapidEnv

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

class ApplicationBuilder {
    private val config = ConfigFactory.load()

    init {
        sikkerLogg.info("SikkerLogg: etterlatte-brev-api oppstart")
    }

    private val env = getRapidEnv()

    private val proxyClient: HttpClient by lazy {
        val clientCredentialsConfig = config.getConfig("no.nav.etterlatte.tjenester.clientcredentials")
        httpClientClientCredentials(
            azureAppClientId = clientCredentialsConfig.getString("clientId"),
            azureAppJwk = clientCredentialsConfig.getString("clientJwk"),
            azureAppWellKnownUrl = clientCredentialsConfig.getString("wellKnownUrl"),
            azureAppScope = config.getString("proxy.outbound")
        )
    }

    private val navansattHttpKlient: HttpClient by lazy {
        val clientCredentialsConfig = config.getConfig("no.nav.etterlatte.tjenester.clientcredentials")
        httpClientClientCredentials(
            azureAppClientId = clientCredentialsConfig.getString("clientId"),
            azureAppJwk = clientCredentialsConfig.getString("clientJwk"),
            azureAppWellKnownUrl = clientCredentialsConfig.getString("wellKnownUrl"),
            azureAppScope = config.getString("navansatt.outbound")
        )
    }

    private val brevbaker =
        BrevbakerKlient(
            httpClient("BREVBAKER_SCOPE"),
            env.requireEnvValue("BREVBAKER_URL")
        )

    private val brregService = BrregService(BrregKlient(httpClient(), env.requireEnvValue("BRREG_URL")))
    private val regoppslagKlient = RegoppslagKlient(proxyClient, env.requireEnvValue("ETTERLATTE_PROXY_URL"))
    private val navansattKlient = NavansattKlient(navansattHttpKlient, env.requireEnvValue("NAVANSATT_URL"))
    private val grunnlagKlient = GrunnlagKlient(config, httpClient())
    private val vedtakKlient = VedtaksvurderingKlient(config, httpClient())
    private val beregningKlient = BeregningKlient(config, httpClient())
    private val behandlingKlient = BehandlingKlient(config, httpClient())
    private val sakOgBehandlingService = SakOgBehandlingService(
        vedtakKlient,
        grunnlagKlient,
        beregningKlient,
        behandlingKlient
    )
    private val norg2Klient = Norg2Klient(env.requireEnvValue("NORG2_URL"), httpClient())
    private val datasource = DataSourceBuilder.createDataSource(env)
    private val db = BrevRepository(datasource)

    private val adresseService = AdresseService(norg2Klient, navansattKlient, regoppslagKlient)

    private val dokarkivKlient = DokarkivKlient(httpClient("DOKARKIV_SCOPE"), env.requireEnvValue("DOKARKIV_URL"))
    private val dokarkivService = DokarkivServiceImpl(dokarkivKlient, db)

    private val distribusjonKlient =
        DistribusjonKlient(httpClient("DOKDIST_SCOPE", false), env.requireEnvValue("DOKDIST_URL"))
    private val distribusjonService = DistribusjonServiceImpl(distribusjonKlient, db)

    private val brevService =
        BrevService(db, sakOgBehandlingService, adresseService, dokarkivService, distribusjonService, brevbaker)

    private val vedtaksbrevService =
        VedtaksbrevService(
            db,
            sakOgBehandlingService,
            adresseService,
            dokarkivService,
            brevbaker
        )

    private val journalpostService =
        SafClient(httpClient(), env.requireEnvValue("SAF_BASE_URL"), env.requireEnvValue("SAF_SCOPE"))

    private val rapidsConnection: RapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
            .withKtorModule {
                restModule(sikkerLogg, routePrefix = "api", config = HoconApplicationConfig(config)) {
                    brevRoute(brevService, behandlingKlient)
                    vedtaksbrevRoute(vedtaksbrevService, behandlingKlient)
                    dokumentRoute(journalpostService, behandlingKlient)
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
                VedtaksbrevUnderkjent(this, vedtaksbrevService)
                DistribuerBrev(this, vedtaksbrevService, distribusjonService)
            }

    fun start() = setReady().also { rapidsConnection.start() }

    private fun httpClient(scope: String? = null, forventStatusSuccess: Boolean = true) = HttpClient(OkHttp) {
        expectSuccess = forventStatusSuccess
        install(ContentNegotiation) {
            jackson() {
                addMixIn(RenderedJsonLetter.Block::class.java, BrevbakerJSONBlockMixIn::class.java)
                addMixIn(RenderedJsonLetter.ParagraphContent::class.java, BrevbakerJSONParagraphMixIn::class.java)
            }
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
            header(XCorrelationId, getCorrelationId())
            header(NAV_CALL_ID, getCorrelationId())
        }
    }.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }
}