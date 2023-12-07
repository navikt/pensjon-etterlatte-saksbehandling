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
import no.nav.etterlatte.rivers.migrering.SUM
import no.nav.etterlatte.rivers.migrering.OpprettVedtaksbrevForOmregningNyRegelRiver
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
            Pair("395abd84-6007-4cff-8a06-7b91fedb59e6", 2409),
            Pair("b191aa19-172d-4c44-bc43-1b3e547b21fb", 2409),
            Pair("9fa2a79b-aa15-4350-819e-a96941aaa7e1", 3064),
            Pair("ea173700-bf70-4631-bea6-0d9acbee0688", 3460),
            Pair("2f57f91c-8425-4995-b712-743d2d93e7dc", 2570),
            Pair("09bad214-d1bd-41d1-aeef-3e19ad7e4cfb", 3460),
            Pair("43bbcc45-73d7-4f01-8899-8bf18fcc52c1", 2274),
            Pair("49e8032c-a250-4877-aaa8-70789d992116", 2274),
            Pair("668b777d-a900-401d-b2a6-720aa12cb704", 2471),
            Pair("a81e42de-2c2a-4b80-a219-e6d0bb29c47d", 2768),
            Pair("51b5023e-bc4e-4319-a037-5764a08a5f4e", 1285),
            Pair("a6c606e5-b641-4a5c-a1f2-6e05a816427b", 3064),
            Pair("3553a6f7-618a-4343-a79e-2493894812e7", 3064),
            Pair("c45932c1-de4a-49ff-b757-f288348fee62", 2972),
            Pair("34468878-598a-47e0-8153-62c5f4a87bce", 2972),
            Pair("caa388d0-829f-43af-8273-e1ebfb980be5", 3163),
            Pair("c5873f24-97be-43a9-b0c1-8c2f14f0f9c2", 1825),
            Pair("0c492fd1-e260-4e69-ab60-0ed19406e3ff", 1236),
            Pair("0e6c41bc-892e-40c2-a15a-9bb076dd71c4", 1825),
            Pair("ff0f516c-32ed-4bf8-bb9a-f3232de8f4a4", 831),
            Pair("6aac01be-5e32-470f-bf3e-dfd0ec107d26", 831),
            Pair("98b6aba0-bb92-4ae2-b13e-5e691189085d", 845),
            Pair("20a9a824-6a5a-4522-8a7a-8f3b0e866b1f", 322),
            Pair("1653e0d5-f625-48e0-81dd-c65b24f6fba1", 322),
            Pair("275752df-5120-46f2-be95-d1022ca372f8", 1510),
            Pair("0dbfe3df-d316-4f3e-8998-a43348c94011", 629),
            Pair("e293020c-0aa5-4884-bf32-c7feb0e8f279", 432),
            Pair("7f582e42-72e8-4d46-ae36-283db0e54a3d", 1732),
            Pair("88ebbfe3-4d1c-446d-b0fc-87d8b229ad11", 1141),
            Pair("f49bda43-9c15-4572-8473-e6a334e8b12c", 560),
            Pair("10ab6ab8-d2d6-49be-8863-63057732ea2a", 1506),
            Pair("20a0a144-5cfd-4d07-b4e2-2b3cfa125b2a", 1506),
            Pair("24bdbed5-36a0-42de-a678-73cc6b5eabcc", 2175),
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
