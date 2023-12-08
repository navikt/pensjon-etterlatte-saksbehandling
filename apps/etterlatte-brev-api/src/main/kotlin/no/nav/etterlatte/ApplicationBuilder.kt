package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.brev.MigreringBrevDataService
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.Norg2Klient
import no.nav.etterlatte.brev.adresse.RegoppslagKlient
import no.nav.etterlatte.brev.adresse.navansatt.NavansattKlient
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
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
import no.nav.etterlatte.brev.hentinformasjon.BeregningKlient
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.hentinformasjon.GrunnlagKlient
import no.nav.etterlatte.brev.hentinformasjon.SakService
import no.nav.etterlatte.brev.hentinformasjon.SoekerService
import no.nav.etterlatte.brev.hentinformasjon.Tilgangssjekker
import no.nav.etterlatte.brev.hentinformasjon.TrygdetidKlient
import no.nav.etterlatte.brev.hentinformasjon.TrygdetidService
import no.nav.etterlatte.brev.hentinformasjon.VedtaksvurderingKlient
import no.nav.etterlatte.brev.hentinformasjon.VedtaksvurderingService
import no.nav.etterlatte.brev.model.BrevDataMapper
import no.nav.etterlatte.brev.model.BrevProsessTypeFactory
import no.nav.etterlatte.brev.vedtaksbrevRoute
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.requireEnvValue
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.rapidsandrivers.migrering.FIKS_BREV_MIGRERING
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rivers.DistribuerBrevRiver
import no.nav.etterlatte.rivers.JournalfoerVedtaksbrevRiver
import no.nav.etterlatte.rivers.VedtaksbrevUnderkjentRiver
import no.nav.etterlatte.rivers.migrering.FiksEnkeltbrevRiver
import no.nav.etterlatte.rivers.migrering.OpprettVedtaksbrevForMigreringRiver
import no.nav.etterlatte.rivers.migrering.OpprettVedtaksbrevForOmregningNyRegelRiver
import no.nav.etterlatte.rivers.migrering.SUM
import no.nav.etterlatte.security.ktor.clientCredential
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.pensjon.brevbaker.api.model.RenderedJsonLetter
import org.slf4j.Logger
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.getRapidEnv
import java.util.UUID
import kotlin.concurrent.thread

val sikkerLogg: Logger = sikkerlogger()

class ApplicationBuilder {
    private val config = ConfigFactory.load()

    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-brev-api")
    }

    private val env = getRapidEnv()

    private val proxyClient: HttpClient by lazy {
        val clientCredentialsConfig = config.getConfig("no.nav.etterlatte.tjenester.clientcredentials")
        httpClientClientCredentials(
            azureAppClientId = clientCredentialsConfig.getString("clientId"),
            azureAppJwk = clientCredentialsConfig.getString("clientJwk"),
            azureAppWellKnownUrl = clientCredentialsConfig.getString("wellKnownUrl"),
            azureAppScope = config.getString("proxy.outbound"),
        )
    }

    private val navansattHttpKlient: HttpClient by lazy {
        val clientCredentialsConfig = config.getConfig("no.nav.etterlatte.tjenester.clientcredentials")
        httpClientClientCredentials(
            azureAppClientId = clientCredentialsConfig.getString("clientId"),
            azureAppJwk = clientCredentialsConfig.getString("clientJwk"),
            azureAppWellKnownUrl = clientCredentialsConfig.getString("wellKnownUrl"),
            azureAppScope = config.getString("navansatt.outbound"),
        )
    }

    private val brevbaker =
        BrevbakerKlient(
            httpClient("BREVBAKER_SCOPE"),
            env.requireEnvValue("BREVBAKER_URL"),
        )

    private val regoppslagKlient = RegoppslagKlient(proxyClient, env.requireEnvValue("ETTERLATTE_PROXY_URL"))
    private val navansattKlient = NavansattKlient(navansattHttpKlient, env.requireEnvValue("NAVANSATT_URL"))
    private val grunnlagKlient = GrunnlagKlient(config, httpClient())
    private val vedtakKlient = VedtaksvurderingKlient(config, httpClient())
    private val beregningKlient = BeregningKlient(config, httpClient())
    private val behandlingKlient = BehandlingKlient(config, httpClient())
    private val trygdetidKlient = TrygdetidKlient(config, httpClient())
    private val trygdetidService = TrygdetidService(trygdetidKlient)

    private val sakService = SakService(behandlingKlient)

    private val brevdataFacade =
        BrevdataFacade(
            vedtakKlient,
            grunnlagKlient,
            beregningKlient,
            behandlingKlient,
            sakService,
            trygdetidService,
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

    private val migreringBrevDataService = MigreringBrevDataService(brevdataFacade)

    private val brevDataMapper = BrevDataMapper(featureToggleService, brevdataFacade, migreringBrevDataService)

    private val brevbakerService = BrevbakerService(brevbaker, adresseService, brevDataMapper)

    private val brevProsessTypeFactory = BrevProsessTypeFactory(featureToggleService)

    private val soekerService = SoekerService(grunnlagKlient)

    private val vedtaksvurderingService = VedtaksvurderingService(vedtakKlient)

    private val brevService =
        BrevService(
            db,
            sakService,
            soekerService,
            adresseService,
            dokarkivService,
            distribusjonService,
            brevbakerService,
        )

    private val vedtaksbrevService =
        VedtaksbrevService(
            db,
            brevdataFacade,
            vedtaksvurderingService,
            adresseService,
            dokarkivService,
            brevbakerService,
            brevDataMapper,
            brevProsessTypeFactory,
            migreringBrevDataService,
        )

    private val journalpostService =
        SafClient(httpClient(), env.requireEnvValue("SAF_BASE_URL"), env.requireEnvValue("SAF_SCOPE"))

    private val tilgangssjekker = Tilgangssjekker(config, httpClient())

    private val rapidsConnection: RapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
            .withKtorModule {
                restModule(sikkerLogg, routePrefix = "api", config = HoconApplicationConfig(config)) {
                    brevRoute(brevService, tilgangssjekker)
                    vedtaksbrevRoute(vedtaksbrevService, tilgangssjekker)
                    dokumentRoute(journalpostService, dokarkivService, tilgangssjekker)
                }
            }
            .build()
            .apply {
                register(
                    object : RapidsConnection.StatusListener {
                        override fun onStartup(rapidsConnection: RapidsConnection) {
                            datasource.migrate()
                        }
                    },
                )
                OpprettVedtaksbrevForMigreringRiver(this, vedtaksbrevService)
                FiksEnkeltbrevRiver(this, vedtaksbrevService, vedtaksvurderingService)
                    .also { fiksEnkeltbrev() }

                OpprettVedtaksbrevForOmregningNyRegelRiver(this, vedtaksbrevService)

                JournalfoerVedtaksbrevRiver(this, vedtaksbrevService)
                VedtaksbrevUnderkjentRiver(this, vedtaksbrevService)
                DistribuerBrevRiver(this, vedtaksbrevService, distribusjonService)
            }

    private fun fiksEnkeltbrev() {
        thread {
            Thread.sleep(60_000)
            behandlingerAaLageBrevFor.forEach {
                rapidsConnection.publish(
                    message = lagMelding(behandlingId = it),
                    key = UUID.randomUUID().toString(),
                )
                Thread.sleep(3000)
            }
        }
    }

    private fun lagMelding(behandlingId: Pair<String, Int>) =
        JsonMessage.newMessage(
            mapOf(
                EVENT_NAME_KEY to Migreringshendelser.FIKS_ENKELTBREV,
                BEHANDLING_ID_KEY to behandlingId.first,
                FIKS_BREV_MIGRERING to true,
                SUM to behandlingId.second,
            ),
        ).toJson()

    private val behandlingerAaLageBrevFor =
        listOf(
            Pair("a60d9a29-5e6f-4f66-87e5-a55d1e04776b", 2409),
            Pair("7a3c70c9-8f07-4673-a102-56a8915b75fb", 2409),
            Pair("09750ea8-87a4-4af6-8507-f2e225d0a10f", 3064),
            Pair("2d0cac13-c9f3-4a73-b4a8-7103eb0aed91", 3460),
            Pair("a44f9508-0f12-4c67-8dac-a8591b711150", 2570),
            Pair("74c403aa-9f8e-40ec-8fe7-3ed9e9a32252", 3460),
            Pair("0e664ad2-d27d-4083-84b9-c3aa73184d32", 2274),
            Pair("fc1f78b6-8216-4350-9444-454600ad99a4", 2274),
            Pair("fe39bbe1-87e5-4b8e-862a-88abfed9909f", 2471),
            Pair("da139e13-3454-4813-a8c8-bad29726a6ff", 2768),
            Pair("9fad4fb0-cfaa-40f9-81e4-7686838b8f0f", 1285),
            Pair("de1cb209-afdd-4fd5-8dd9-e28c3daab5de", 3064),
            Pair("73e869c3-5c64-4cbf-9767-4a8b77986b4e", 3064),
            Pair("7f0ffc77-fefd-49f3-b981-a8a00e73e85b", 2972),
            Pair("f394d137-c697-474b-96e8-59e1373963f9", 2972),
            Pair("feca949d-c1f8-4ea1-8c6c-78860e4a5edf", 3163),
            Pair("2dbf5dc0-d169-4d99-b163-eb5ca82d9cd9", 1825),
            Pair("34f85646-3c35-4ddd-8d10-f351c1624c96", 1236),
            Pair("403e54da-8de1-462a-b92c-08a22bbb6275", 1825),
            Pair("139e2c61-3c02-4ff6-aa9d-1f59126be19a", 831),
            Pair("1308e7b2-f7ac-4033-886f-d09a17f62a32", 831),
            Pair("9fb00a24-584d-4e23-9175-0246b6937ef9", 845),
            Pair("a360b5ba-29f8-4136-8bee-cf838c4ca6b8", 322),
            Pair("446c4cd9-6844-4bca-bea8-f40aa69ffd0d", 322),
            Pair("e44a0fbc-5563-4437-8661-880e6ae6e237", 1510),
            Pair("477dcba4-fe4c-4ba3-8456-e714b222b989", 629),
            Pair("23656d93-dbb5-49b3-8abc-5c435d7a7db5", 432),
            Pair("ba8d39e1-9425-41bb-ab94-0834740d8720", 1732),
            Pair("88933285-9473-497d-b0e8-f1a4aef9cb8b", 1141),
            Pair("2d7ea4f4-1ea7-407b-ab13-6e048061497e", 560),
            Pair("28df8377-8cb7-4e55-b083-54a8b6056304", 1506),
            Pair("ad4971cf-422e-453f-8f25-d2933610cdd0", 1506),
            Pair("4317a4fa-c5d3-4d65-9e24-a00067e808ae", 2175),
        )

    private fun featureToggleProperties(config: Config) =
        FeatureToggleProperties(
            applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
            host = config.getString("funksjonsbrytere.unleash.host"),
            apiKey = config.getString("funksjonsbrytere.unleash.token"),
        )

    fun start() = setReady().also { rapidsConnection.start() }

    private fun httpClient(
        scope: String? = null,
        forventStatusSuccess: Boolean = true,
    ) = httpClient(
        forventSuksess = forventStatusSuccess,
        ekstraJacksoninnstillinger = {
            it.addMixIn(RenderedJsonLetter.Block::class.java, BrevbakerJSONBlockMixIn::class.java)
            it.addMixIn(RenderedJsonLetter.ParagraphContent::class.java, BrevbakerJSONParagraphMixIn::class.java)
        },
        auth = {
            if (scope != null) {
                it.install(Auth) {
                    clientCredential {
                        config =
                            env.toMutableMap()
                                .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get(scope))) }
                    }
                }
            }
        },
    )
}
