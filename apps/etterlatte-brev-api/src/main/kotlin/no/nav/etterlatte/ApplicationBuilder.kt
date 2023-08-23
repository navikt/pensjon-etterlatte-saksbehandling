package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
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
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.DistribusjonKlient
import no.nav.etterlatte.brev.distribusjon.DistribusjonServiceImpl
import no.nav.etterlatte.brev.dokarkiv.DokarkivKlient
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
import no.nav.etterlatte.brev.dokument.SafClient
import no.nav.etterlatte.brev.dokument.dokumentRoute
import no.nav.etterlatte.brev.grunnlag.GrunnlagKlient
import no.nav.etterlatte.brev.model.BrevDataMapper
import no.nav.etterlatte.brev.model.BrevProsessTypeFactory
import no.nav.etterlatte.brev.navansatt.NavansattKlient
import no.nav.etterlatte.brev.trygdetid.TrygdetidKlient
import no.nav.etterlatte.brev.vedtak.VedtaksvurderingKlient
import no.nav.etterlatte.brev.vedtaksbrevRoute
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.requireEnvValue
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.httpClient
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
import java.net.URI

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
    private val trygdetidKlient = TrygdetidKlient(config, httpClient())
    private val sakOgBehandlingService = SakOgBehandlingService(
        vedtakKlient,
        grunnlagKlient,
        beregningKlient,
        behandlingKlient,
        trygdetidKlient
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

    private val featureToggleService = FeatureToggleService.initialiser(featureToggleProperties(config))

    private val brevDataMapper = BrevDataMapper(featureToggleService)

    private val brevbakerService = BrevbakerService(brevbaker, adresseService, brevDataMapper)

    private val brevProsessTypeFactory = BrevProsessTypeFactory(featureToggleService)

    private val brevService =
        BrevService(db, sakOgBehandlingService, adresseService, dokarkivService, distribusjonService, brevbakerService)

    private val vedtaksbrevService =
        VedtaksbrevService(
            db,
            sakOgBehandlingService,
            adresseService,
            dokarkivService,
            brevbakerService,
            brevDataMapper,
            brevProsessTypeFactory
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

    private fun featureToggleProperties(config: Config) = FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        uri = URI(config.getString("funksjonsbrytere.unleash.uri")),
        cluster = config.getString("funksjonsbrytere.unleash.cluster")
    )

    fun start() = setReady().also { rapidsConnection.start() }

    private fun httpClient(scope: String? = null, forventStatusSuccess: Boolean = true) = httpClient(
        forventSuksess = forventStatusSuccess,
        ekstraJacksoninnstillinger = {
            it.addMixIn(RenderedJsonLetter.Block::class.java, BrevbakerJSONBlockMixIn::class.java)
            it.addMixIn(RenderedJsonLetter.ParagraphContent::class.java, BrevbakerJSONParagraphMixIn::class.java)
        },
        auth = {
            if (scope != null) {
                it.install(Auth) {
                    clientCredential {
                        config = env.toMutableMap()
                            .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get(scope))) }
                    }
                }
            }
        }
    )
}