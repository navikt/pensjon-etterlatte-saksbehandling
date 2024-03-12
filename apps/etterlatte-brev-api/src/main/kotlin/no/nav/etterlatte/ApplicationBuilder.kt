package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.JournalfoerBrevService
import no.nav.etterlatte.brev.MigreringBrevDataService
import no.nav.etterlatte.brev.NotatService
import no.nav.etterlatte.brev.PDFGenerator
import no.nav.etterlatte.brev.PDFService
import no.nav.etterlatte.brev.RedigerbartVedleggHenter
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
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.distribusjon.DistribusjonKlient
import no.nav.etterlatte.brev.distribusjon.DistribusjonServiceImpl
import no.nav.etterlatte.brev.dokarkiv.DokarkivKlient
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
import no.nav.etterlatte.brev.dokument.SafKlient
import no.nav.etterlatte.brev.dokument.SafService
import no.nav.etterlatte.brev.dokument.dokumentRoute
import no.nav.etterlatte.brev.hentinformasjon.BeregningKlient
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.hentinformasjon.GrunnlagKlient
import no.nav.etterlatte.brev.hentinformasjon.SakService
import no.nav.etterlatte.brev.hentinformasjon.Tilgangssjekker
import no.nav.etterlatte.brev.hentinformasjon.TrygdetidKlient
import no.nav.etterlatte.brev.hentinformasjon.TrygdetidService
import no.nav.etterlatte.brev.hentinformasjon.VedtaksvurderingKlient
import no.nav.etterlatte.brev.hentinformasjon.VedtaksvurderingService
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningService
import no.nav.etterlatte.brev.model.BrevDataMapperFerdigstillingVedtak
import no.nav.etterlatte.brev.model.BrevDataMapperRedigerbartUtfallVedtak
import no.nav.etterlatte.brev.model.BrevKodeMapperVedtak
import no.nav.etterlatte.brev.notatRoute
import no.nav.etterlatte.brev.oversendelsebrev.OversendelseBrevServiceImpl
import no.nav.etterlatte.brev.oversendelsebrev.oversendelseBrevRoute
import no.nav.etterlatte.brev.varselbrev.BrevDataMapperFerdigstillVarsel
import no.nav.etterlatte.brev.varselbrev.VarselbrevService
import no.nav.etterlatte.brev.varselbrev.varselbrevRoute
import no.nav.etterlatte.brev.vedtaksbrevRoute
import no.nav.etterlatte.brev.virusskanning.ClamAvClient
import no.nav.etterlatte.brev.virusskanning.VirusScanService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.requireEnvValue
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.etterlatte.rapidsandrivers.migrering.FIKS_BREV_MIGRERING
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rivers.DistribuerBrevRiver
import no.nav.etterlatte.rivers.FerdigstillJournalfoerOgDistribuerBrev
import no.nav.etterlatte.rivers.JournalfoerVedtaksbrevRiver
import no.nav.etterlatte.rivers.OpprettJournalfoerOgDistribuerRiver
import no.nav.etterlatte.rivers.StartBrevgenereringRepository
import no.nav.etterlatte.rivers.StartInformasjonsbrevgenereringRiver
import no.nav.etterlatte.rivers.VedtaksbrevUnderkjentRiver
import no.nav.etterlatte.rivers.migrering.FiksEnkeltbrevRiver
import no.nav.etterlatte.rivers.migrering.OpprettVarselbrevForGjenopprettaRiver
import no.nav.etterlatte.rivers.migrering.OpprettVedtaksbrevForMigreringRiver
import no.nav.etterlatte.rivers.migrering.behandlingerAaJournalfoereBrevFor
import no.nav.etterlatte.security.ktor.clientCredential
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.pensjon.brevbaker.api.model.RenderedJsonLetter
import org.slf4j.Logger
import java.util.UUID
import kotlin.concurrent.thread

val sikkerLogg: Logger = sikkerlogger()

class ApplicationBuilder {
    private val config = ConfigFactory.load()

    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-brev-api")
    }

    private val env = getRapidEnv()

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

    private val regoppslagKlient =
        RegoppslagKlient(httpClient("REGOPPSLAG_SCOPE"), env.requireEnvValue("REGOPPSLAG_URL"))
    private val navansattKlient = NavansattKlient(navansattHttpKlient, env.requireEnvValue("NAVANSATT_URL"))
    private val grunnlagKlient = GrunnlagKlient(config, httpClient())
    private val vedtakKlient = VedtaksvurderingKlient(config, httpClient())
    private val beregningKlient = BeregningKlient(config, httpClient())
    private val behandlingKlient = BehandlingKlient(config, httpClient())
    private val trygdetidKlient = TrygdetidKlient(config, httpClient())
    private val trygdetidService = TrygdetidService(trygdetidKlient, beregningKlient)

    private val sakService = SakService(behandlingKlient)

    private val beregningService = BeregningService(beregningKlient)
    private val brevdataFacade =
        BrevdataFacade(
            vedtakKlient,
            grunnlagKlient,
            beregningService,
            behandlingKlient,
            sakService,
            trygdetidService,
        )
    private val norg2Klient = Norg2Klient(env.requireEnvValue("NORG2_URL"), httpClient())
    private val datasource = DataSourceBuilder.createDataSource(env)

    private val db = BrevRepository(datasource)

    private val brevgenereringRepository = StartBrevgenereringRepository(datasource)

    private val adresseService = AdresseService(norg2Klient, navansattKlient, regoppslagKlient)
    private val dokarkivKlient =
        DokarkivKlient(httpClient("DOKARKIV_SCOPE", false), env.requireEnvValue("DOKARKIV_URL"))

    private val dokarkivService = DokarkivServiceImpl(dokarkivKlient, db)

    private val distribusjonKlient =
        DistribusjonKlient(httpClient("DOKDIST_SCOPE", false), env.requireEnvValue("DOKDIST_URL"))

    private val distribusjonService = DistribusjonServiceImpl(distribusjonKlient, db)

    private val migreringBrevDataService = MigreringBrevDataService(brevdataFacade)

    private val brevDataMapperRedigerbartUtfallVedtak =
        BrevDataMapperRedigerbartUtfallVedtak(brevdataFacade, migreringBrevDataService)

    private val brevDataMapperFerdigstilling = BrevDataMapperFerdigstillingVedtak(brevdataFacade)

    private val brevKodeMapperVedtak = BrevKodeMapperVedtak()

    private val brevbakerService = BrevbakerService(brevbaker, adresseService)

    private val vedtaksvurderingService = VedtaksvurderingService(vedtakKlient)

    private val brevdistribuerer = Brevdistribuerer(db, distribusjonService)

    private val redigerbartVedleggHenter = RedigerbartVedleggHenter(brevbakerService, brevdataFacade)

    private val brevoppretter =
        Brevoppretter(adresseService, db, brevdataFacade, brevbakerService, redigerbartVedleggHenter)

    private val pdfGenerator =
        PDFGenerator(db, brevdataFacade, adresseService, brevbakerService)

    private val vedtaksbrevService =
        VedtaksbrevService(
            db,
            vedtaksvurderingService,
            brevKodeMapperVedtak,
            brevoppretter,
            pdfGenerator,
            brevDataMapperRedigerbartUtfallVedtak,
            brevDataMapperFerdigstilling,
            behandlingKlient,
        )
    private val brevDataMapperFerdigstillVarsel = BrevDataMapperFerdigstillVarsel(beregningService, trygdetidService)

    private val varselbrevService =
        VarselbrevService(db, brevoppretter, behandlingKlient, pdfGenerator, brevDataMapperFerdigstillVarsel)

    private val journalfoerBrevService = JournalfoerBrevService(db, sakService, dokarkivService, vedtaksbrevService)

    private val brevService =
        BrevService(
            db,
            brevoppretter,
            journalfoerBrevService,
            pdfGenerator,
        )

    private val clamAvClient = ClamAvClient(httpClient(), env.requireEnvValue("CLAMAV_ENDPOINT_URL"))
    private val virusScanService = VirusScanService(clamAvClient)
    private val pdfService = PDFService(db, virusScanService)

    private val safService =
        SafService(
            SafKlient(httpClient(), env.requireEnvValue("SAF_BASE_URL"), env.requireEnvValue("SAF_SCOPE")),
        )

    private val oversendelseBrevService =
        OversendelseBrevServiceImpl(
            brevRepository = db,
            pdfGenerator = pdfGenerator,
            adresseService = adresseService,
            brevdataFacade = brevdataFacade,
            behandlingKlient = behandlingKlient,
        )

    private val notatService = NotatService(db, adresseService, brevbakerService, grunnlagKlient, dokarkivKlient)

    private val tilgangssjekker = Tilgangssjekker(config, httpClient())

    private val rapidsConnection: RapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
            .withKtorModule {
                restModule(sikkerLogg, routePrefix = "api", config = HoconApplicationConfig(config)) {
                    brevRoute(brevService, pdfService, brevdistribuerer, tilgangssjekker)
                    vedtaksbrevRoute(vedtaksbrevService, tilgangssjekker)
                    dokumentRoute(safService, dokarkivService, tilgangssjekker)
                    varselbrevRoute(varselbrevService, tilgangssjekker)
                    notatRoute(notatService, tilgangssjekker)
                    oversendelseBrevRoute(oversendelseBrevService, tilgangssjekker)
                }
            }
            .build()
            .apply {
                val brevgenerering =
                    StartInformasjonsbrevgenereringRiver(
                        brevgenereringRepository,
                        this,
                    )
                register(
                    object : RapidsConnection.StatusListener {
                        override fun onStartup(rapidsConnection: RapidsConnection) {
                            datasource.migrate()
                            brevgenerering.init()
                        }
                    },
                )
                OpprettVedtaksbrevForMigreringRiver(this, vedtaksbrevService)
                val ferdigstillJournalfoerOgDistribuerBrev =
                    FerdigstillJournalfoerOgDistribuerBrev(
                        pdfGenerator,
                        journalfoerBrevService,
                        brevdistribuerer,
                    )

                val featureToggleService: FeatureToggleService =
                    FeatureToggleService.initialiser(
                        FeatureToggleProperties(
                            applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
                            host = config.getString("funksjonsbrytere.unleash.host"),
                            apiKey = config.getString("funksjonsbrytere.unleash.token"),
                        ),
                    )
                OpprettVarselbrevForGjenopprettaRiver(
                    this,
                    varselbrevService,
                    ferdigstillJournalfoerOgDistribuerBrev,
                    behandlingKlient,
                    featureToggleService,
                )
                FiksEnkeltbrevRiver(this, varselbrevService, ferdigstillJournalfoerOgDistribuerBrev)
                    .also { fiksEnkeltbrev() }
                OpprettJournalfoerOgDistribuerRiver(
                    this,
                    brevdataFacade,
                    brevoppretter,
                    ferdigstillJournalfoerOgDistribuerBrev,
                )

                JournalfoerVedtaksbrevRiver(this, journalfoerBrevService)
                VedtaksbrevUnderkjentRiver(this, vedtaksbrevService)
                DistribuerBrevRiver(this, brevdistribuerer)
            }

    private fun fiksEnkeltbrev() {
        thread {
            Thread.sleep(60_000)
            behandlingerAaJournalfoereBrevFor.forEach {
                rapidsConnection.publish(
                    message = lagMelding(behandlingId = it),
                    key = UUID.randomUUID().toString(),
                )
                Thread.sleep(3000)
            }
        }
    }

    private fun lagMelding(behandlingId: String) =
        JsonMessage.newMessage(
            mapOf(
                Migreringshendelser.FIKS_ENKELTBREV.lagParMedEventNameKey(),
                BEHANDLING_ID_KEY to behandlingId,
                FIKS_BREV_MIGRERING to true,
            ),
        ).toJson()

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
