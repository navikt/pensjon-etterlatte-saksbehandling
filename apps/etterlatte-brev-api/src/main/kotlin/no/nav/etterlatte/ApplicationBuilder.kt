package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.BrevKey.BREVBAKER_SCOPE
import no.nav.etterlatte.BrevKey.BREVBAKER_URL
import no.nav.etterlatte.BrevKey.CLAMAV_ENDPOINT_URL
import no.nav.etterlatte.BrevKey.DOKDIST_SCOPE
import no.nav.etterlatte.BrevKey.DOKDIST_URL
import no.nav.etterlatte.BrevKey.REGOPPSLAG_SCOPE
import no.nav.etterlatte.BrevKey.REGOPPSLAG_URL
import no.nav.etterlatte.BrevKey.SAF_BASE_URL
import no.nav.etterlatte.BrevKey.SAF_SCOPE
import no.nav.etterlatte.EnvKey.DOKARKIV_SCOPE
import no.nav.etterlatte.EnvKey.DOKARKIV_URL
import no.nav.etterlatte.EnvKey.NORG2_URL
import no.nav.etterlatte.EnvKey.PDFGEN_URL
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.InnholdTilRedigerbartBrevHenter
import no.nav.etterlatte.brev.JournalfoerBrevService
import no.nav.etterlatte.brev.MigreringBrevDataService
import no.nav.etterlatte.brev.NotatService
import no.nav.etterlatte.brev.NyNotatService
import no.nav.etterlatte.brev.RedigerbartVedleggHenter
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.Norg2Klient
import no.nav.etterlatte.brev.adresse.RegoppslagKlient
import no.nav.etterlatte.brev.adresse.saksbehandler.SaksbehandlerKlient
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.behandlingklient.OppgaveKlient
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
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningKlient
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningService
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagKlient
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagService
import no.nav.etterlatte.brev.hentinformasjon.trygdetid.TrygdetidKlient
import no.nav.etterlatte.brev.hentinformasjon.trygdetid.TrygdetidService
import no.nav.etterlatte.brev.hentinformasjon.vedtaksvurdering.VedtaksvurderingKlient
import no.nav.etterlatte.brev.hentinformasjon.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.brev.hentinformasjon.vilkaarsvurdering.BehandlingVilkaarsvurderingKlient
import no.nav.etterlatte.brev.hentinformasjon.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.brev.model.BrevDataMapperFerdigstillingVedtak
import no.nav.etterlatte.brev.model.BrevDataMapperRedigerbartUtfallVedtak
import no.nav.etterlatte.brev.model.BrevKodeMapperVedtak
import no.nav.etterlatte.brev.notat.NotatRepository
import no.nav.etterlatte.brev.notat.notatRoute
import no.nav.etterlatte.brev.oppgave.OppgaveService
import no.nav.etterlatte.brev.oversendelsebrev.OversendelseBrevServiceImpl
import no.nav.etterlatte.brev.oversendelsebrev.oversendelseBrevRoute
import no.nav.etterlatte.brev.pdf.PDFGenerator
import no.nav.etterlatte.brev.pdf.PDFService
import no.nav.etterlatte.brev.pdfgen.PdfGeneratorKlient
import no.nav.etterlatte.brev.varselbrev.BrevDataMapperFerdigstillVarsel
import no.nav.etterlatte.brev.varselbrev.VarselbrevService
import no.nav.etterlatte.brev.varselbrev.varselbrevRoute
import no.nav.etterlatte.brev.vedtaksbrev.VedtaksbrevService
import no.nav.etterlatte.brev.vedtaksbrev.vedtaksbrevRoute
import no.nav.etterlatte.brev.virusskanning.ClamAvClient
import no.nav.etterlatte.brev.virusskanning.VirusScanService
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_OUTBOUND_SCOPE
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.ktor.clientCredential
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.rapidsandrivers.configFromEnvironment
import no.nav.etterlatte.rapidsandrivers.getRapidEnv
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.pensjon.brevbaker.api.model.LetterMarkup
import org.slf4j.Logger
import rapidsandrivers.initRogR

val sikkerLogg: Logger = sikkerlogger()

class ApplicationBuilder {
    private val config = ConfigFactory.load()

    private val env = getRapidEnv()

    private val brevbaker =
        BrevbakerKlient(
            httpClient(BREVBAKER_SCOPE),
            env.requireEnvValue(BREVBAKER_URL),
        )

    private val regoppslagKlient =
        RegoppslagKlient(httpClient(REGOPPSLAG_SCOPE), env.requireEnvValue(REGOPPSLAG_URL))
    private val saksbehandlerKlient = SaksbehandlerKlient(config, httpClient())
    private val grunnlagKlient = GrunnlagKlient(config, httpClient())
    private val vedtakKlient = VedtaksvurderingKlient(config, httpClient())
    private val beregningKlient = BeregningKlient(config, httpClient())
    private val behandlingKlient = BehandlingKlient(config, httpClient())
    private val oppgaveKlient = OppgaveKlient(config, httpClient())
    private val trygdetidKlient = TrygdetidKlient(config, httpClient())
    private val vilkaarsvurderingKlient = BehandlingVilkaarsvurderingKlient(config, httpClient())

    private val behandlingService = BehandlingService(behandlingKlient)
    private val oppgaveService = OppgaveService(oppgaveKlient)
    private val trygdetidService = TrygdetidService(trygdetidKlient)

    private val beregningService = BeregningService(beregningKlient)
    private val norg2Klient = Norg2Klient(env.requireEnvValue(NORG2_URL), httpClient())
    private val adresseService = AdresseService(norg2Klient, saksbehandlerKlient, regoppslagKlient)

    private val grunnlagService = GrunnlagService(grunnlagKlient, adresseService)
    private val vedtaksvurderingService = VedtaksvurderingService(vedtakKlient)
    private val vilkaarsvurderingService = VilkaarsvurderingService(vilkaarsvurderingKlient)

    private val datasource = DataSourceBuilder.createDataSource(env)

    private val brevdataFacade =
        BrevdataFacade(
            vedtaksvurderingService,
            grunnlagService,
            behandlingService,
        )

    private val db = BrevRepository(datasource)

    private val dokarkivKlient =
        DokarkivKlient(httpClient(DOKARKIV_SCOPE, false), env.requireEnvValue(DOKARKIV_URL))

    private val dokarkivService = DokarkivServiceImpl(dokarkivKlient)

    private val distribusjonKlient =
        DistribusjonKlient(httpClient(DOKDIST_SCOPE, false), env.requireEnvValue(DOKDIST_URL))

    private val distribusjonService = DistribusjonServiceImpl(distribusjonKlient, db)

    private val migreringBrevDataService = MigreringBrevDataService(beregningService)

    private val brevDataMapperRedigerbartUtfallVedtak =
        BrevDataMapperRedigerbartUtfallVedtak(behandlingService, beregningService, migreringBrevDataService)

    private val brevDataMapperFerdigstilling =
        BrevDataMapperFerdigstillingVedtak(
            beregningService,
            trygdetidService,
            behandlingService,
            vilkaarsvurderingService,
        )

    private val brevKodeMappingVedtak = BrevKodeMapperVedtak()

    private val brevbakerService = BrevbakerService(brevbaker)

    private val brevdistribuerer = Brevdistribuerer(db, distribusjonService)

    private val redigerbartVedleggHenter = RedigerbartVedleggHenter(brevbakerService, adresseService, behandlingService)

    private val innholdTilRedigerbartBrevHenter =
        InnholdTilRedigerbartBrevHenter(
            brevdataFacade,
            brevbakerService,
            adresseService,
            redigerbartVedleggHenter,
        )
    private val brevoppretter =
        Brevoppretter(adresseService, db, innholdTilRedigerbartBrevHenter)

    private val pdfGenerator =
        PDFGenerator(db, brevdataFacade, adresseService, brevbakerService)

    private val vedtaksbrevService =
        VedtaksbrevService(
            db,
            vedtaksvurderingService,
            brevKodeMappingVedtak,
            brevoppretter,
            pdfGenerator,
            brevDataMapperRedigerbartUtfallVedtak,
            brevDataMapperFerdigstilling,
            behandlingService,
        )
    private val brevDataMapperFerdigstillVarsel =
        BrevDataMapperFerdigstillVarsel(beregningService, trygdetidService, vilkaarsvurderingService)

    private val varselbrevService =
        VarselbrevService(db, brevoppretter, behandlingService, pdfGenerator, brevDataMapperFerdigstillVarsel)

    private val journalfoerBrevService = JournalfoerBrevService(db, behandlingService, dokarkivService, vedtaksbrevService)

    private val brevService =
        BrevService(
            db,
            brevoppretter,
            journalfoerBrevService,
            pdfGenerator,
            brevdistribuerer,
            oppgaveService,
        )

    private val clamAvClient = ClamAvClient(httpClient(), env.requireEnvValue(CLAMAV_ENDPOINT_URL))
    private val virusScanService = VirusScanService(clamAvClient)
    private val pdfService = PDFService(db, virusScanService)

    private val safService =
        SafService(
            SafKlient(httpClient(), env.requireEnvValue(SAF_BASE_URL), env.requireEnvValue(SAF_SCOPE)),
        )

    private val oversendelseBrevService =
        OversendelseBrevServiceImpl(
            brevRepository = db,
            pdfGenerator = pdfGenerator,
            adresseService = adresseService,
            behandlingService = behandlingService,
            grunnlagService = grunnlagService,
        )

    private val notatRepository = NotatRepository(datasource)
    private val pdfGeneratorKlient = PdfGeneratorKlient(httpClient(), env.requireEnvValue(PDFGEN_URL))
    private val nyNotatService = NyNotatService(notatRepository, pdfGeneratorKlient, dokarkivService, behandlingService)
    private val notatService = NotatService(notatRepository, pdfGeneratorKlient, dokarkivService, grunnlagService)

    private val tilgangssjekker = Tilgangssjekker(config, httpClient())

    private val rapidsConnection =
        initRogR(
            applikasjonsnavn = "brev-api",
            restModule = {
                restModule(sikkerLogg, routePrefix = "api", config = HoconApplicationConfig(config)) {
                    brevRoute(brevService, pdfService, brevdistribuerer, tilgangssjekker, grunnlagService, behandlingService)
                    vedtaksbrevRoute(vedtaksbrevService, journalfoerBrevService, tilgangssjekker)
                    dokumentRoute(safService, dokarkivService, tilgangssjekker)
                    varselbrevRoute(varselbrevService, tilgangssjekker)
                    notatRoute(notatService, nyNotatService, tilgangssjekker)
                    oversendelseBrevRoute(oversendelseBrevService, tilgangssjekker)
                }
            },
            configFromEnvironment = { configFromEnvironment(it) },
        ) { rapidsConnection, _ ->
            rapidsConnection.register(
                object : RapidsConnection.StatusListener {
                    override fun onStartup(rapidsConnection: RapidsConnection) {
                        datasource.migrate()
                    }
                },
            )
            /*
                val ferdigstillJournalfoerOgDistribuerBrev =
                    FerdigstillJournalfoerOgDistribuerBrev(
                        pdfGenerator,
                        journalfoerBrevService,
                        brevdistribuerer,
                    )
                OpprettJournalfoerOgDistribuerRiver(
                    rapidsConnection,
                    grunnlagService,
                    oppgaveService,
                    brevoppretter,
                    ferdigstillJournalfoerOgDistribuerBrev,
                )

                JournalfoerVedtaksbrevRiver(rapidsConnection, journalfoerBrevService)
                VedtaksbrevUnderkjentRiver(rapidsConnection, vedtaksbrevService)
                DistribuerBrevRiver(rapidsConnection, brevdistribuerer)
                SamordningsnotatRiver(rapidsConnection, nyNotatService)
            }

             */
        }

    private fun httpClient(
        scope: EnvEnum? = null,
        forventStatusSuccess: Boolean = true,
    ) = httpClient(
        forventSuksess = forventStatusSuccess,
        ekstraJacksoninnstillinger = {
            it.addMixIn(LetterMarkup.Block::class.java, BrevbakerJSONBlockMixIn::class.java)
            it.addMixIn(LetterMarkup.ParagraphContent::class.java, BrevbakerJSONParagraphMixIn::class.java)
        },
        auth = {
            if (scope != null) {
                it.install(Auth) {
                    clientCredential {
                        config =
                            env.append(AZURE_APP_OUTBOUND_SCOPE) { requireNotNull(it.get(scope)) }
                    }
                }
                it.install(HttpTimeout)
            }
        },
    )
}

enum class BrevKey : EnvEnum {
    BREVBAKER_SCOPE,
    BREVBAKER_URL,
    CLAMAV_ENDPOINT_URL,
    DOKDIST_SCOPE,
    DOKDIST_URL,
    REGOPPSLAG_SCOPE,
    REGOPPSLAG_URL,
    SAF_BASE_URL,
    SAF_SCOPE,
    ;

    override fun key() = name
}
