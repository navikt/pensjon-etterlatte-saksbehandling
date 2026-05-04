package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import no.nav.etterlatte.BrevKey.BREVBAKER_SCOPE
import no.nav.etterlatte.BrevKey.BREVBAKER_URL
import no.nav.etterlatte.BrevKey.CLAMAV_ENDPOINT_URL
import no.nav.etterlatte.BrevKey.REGOPPSLAG_SCOPE
import no.nav.etterlatte.BrevKey.REGOPPSLAG_URL
import no.nav.etterlatte.BrevKey.SAF_BASE_URL
import no.nav.etterlatte.BrevKey.SAF_SCOPE
import no.nav.etterlatte.EnvKey.HTTP_PORT
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
import no.nav.etterlatte.brev.brevbaker.BrevbakerKlient
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.distribusjon.DistribusjonKlient
import no.nav.etterlatte.brev.distribusjon.DistribusjonServiceImpl
import no.nav.etterlatte.brev.distribusjon.DokDistKanalKlient
import no.nav.etterlatte.brev.dokarkiv.DokarkivKlient
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
import no.nav.etterlatte.brev.dokument.SafKlient
import no.nav.etterlatte.brev.dokument.SafService
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningKlient
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningService
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
import no.nav.etterlatte.brev.oppgave.OppgaveService
import no.nav.etterlatte.brev.oversendelsebrev.OversendelseBrevServiceImpl
import no.nav.etterlatte.brev.pdf.PDFGenerator
import no.nav.etterlatte.brev.pdf.PDFService
import no.nav.etterlatte.brev.pdfgen.PdfGeneratorKlient
import no.nav.etterlatte.brev.pdl.PdlTjenesterKlient
import no.nav.etterlatte.brev.varselbrev.BrevDataMapperFerdigstillVarsel
import no.nav.etterlatte.brev.varselbrev.VarselbrevService
import no.nav.etterlatte.brev.vedtaksbrev.StrukturertBrevService
import no.nav.etterlatte.brev.vedtaksbrev.VedtaksbrevService
import no.nav.etterlatte.brev.virusskanning.ClamAvClient
import no.nav.etterlatte.brev.virusskanning.VirusScanService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_OUTBOUND_SCOPE
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.ktor.clientCredential
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import org.slf4j.Logger

val sikkerLogg: Logger = sikkerlogger()

internal class ApplicationContext {
    val config = ConfigFactory.load()

    val env = Miljoevariabler.systemEnv()
    val httpPort = env.getOrDefault(HTTP_PORT, "8080").toInt()

    val brevbaker =
        BrevbakerKlient(
            httpClient(BREVBAKER_SCOPE),
            env.requireEnvValue(BREVBAKER_URL),
        )
    val featureToggleService =
        FeatureToggleService.initialiser(
            FeatureToggleProperties(
                applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
                host = config.getString("funksjonsbrytere.unleash.host"),
                apiKey = config.getString("funksjonsbrytere.unleash.token"),
            ),
        )

    val regoppslagKlient =
        RegoppslagKlient(httpClient(REGOPPSLAG_SCOPE), env.requireEnvValue(REGOPPSLAG_URL))
    val saksbehandlerKlient = SaksbehandlerKlient(config, httpClient())
    val vedtakKlient = VedtaksvurderingKlient(config, httpClient())
    val beregningKlient = BeregningKlient(config, httpClient())
    val pdltjenesterKlient = PdlTjenesterKlient(config, httpClient())
    val behandlingKlient = BehandlingKlient(config, httpClient())
    val oppgaveKlient = OppgaveKlient(config, httpClient())
    val trygdetidKlient = TrygdetidKlient(config, httpClient())
    val vilkaarsvurderingKlient = BehandlingVilkaarsvurderingKlient(config, httpClient())

    val behandlingService = BehandlingService(behandlingKlient)
    val oppgaveService = OppgaveService(oppgaveKlient)
    val trygdetidService = TrygdetidService(trygdetidKlient)

    val beregningService = BeregningService(beregningKlient)
    val norg2Klient = Norg2Klient(env.requireEnvValue(NORG2_URL), httpClient())
    val adresseService = AdresseService(norg2Klient, saksbehandlerKlient, regoppslagKlient, pdltjenesterKlient)

    val grunnlagService = GrunnlagService(behandlingKlient, adresseService)
    val vedtaksvurderingService = VedtaksvurderingService(vedtakKlient)
    val vilkaarsvurderingService = VilkaarsvurderingService(vilkaarsvurderingKlient)

    val datasource = DataSourceBuilder.createDataSource(env)

    val brevdataFacade =
        BrevdataFacade(
            vedtaksvurderingService,
            grunnlagService,
            behandlingService,
        )

    val db = BrevRepository(datasource)

    val dokarkivKlient = DokarkivKlient(config)

    val dokarkivService = DokarkivServiceImpl(dokarkivKlient)

    val distribusjonKlient = DistribusjonKlient(config)
    val dokdistKanalKlient = DokDistKanalKlient(config)

    val distribusjonService = DistribusjonServiceImpl(distribusjonKlient)

    val migreringBrevDataService = MigreringBrevDataService(beregningService)

    val brevDataMapperRedigerbartUtfallVedtak =
        BrevDataMapperRedigerbartUtfallVedtak(
            behandlingService,
            beregningService,
            migreringBrevDataService,
            trygdetidService,
        )

    val brevDataMapperFerdigstilling =
        BrevDataMapperFerdigstillingVedtak(
            beregningService,
            trygdetidService,
            behandlingService,
            vilkaarsvurderingService,
        )

    val brevKodeMappingVedtak = BrevKodeMapperVedtak()

    val brevbakerService = BrevbakerService(brevbaker)

    val brevdistribuerer = Brevdistribuerer(db, distribusjonService, featureToggleService)

    val redigerbartVedleggHenter = RedigerbartVedleggHenter(brevbakerService, adresseService, behandlingService)

    val innholdTilRedigerbartBrevHenter =
        InnholdTilRedigerbartBrevHenter(
            brevdataFacade,
            brevbakerService,
            adresseService,
            redigerbartVedleggHenter,
        )
    val brevoppretter =
        Brevoppretter(adresseService, db, innholdTilRedigerbartBrevHenter)

    val pdfGenerator =
        PDFGenerator(db, brevdataFacade, adresseService, brevbakerService)

    val vedtaksbrevService =
        VedtaksbrevService(
            db,
            vedtaksvurderingService,
            brevKodeMappingVedtak,
            brevoppretter,
            pdfGenerator,
            brevDataMapperRedigerbartUtfallVedtak,
            brevDataMapperFerdigstilling,
            behandlingService,
            featureToggleService,
            behandlingKlient,
        )
    val brevDataMapperFerdigstillVarsel =
        BrevDataMapperFerdigstillVarsel(beregningService, trygdetidService, behandlingService, vilkaarsvurderingService)

    val varselbrevService =
        VarselbrevService(
            db,
            brevoppretter,
            behandlingService,
            pdfGenerator,
            brevDataMapperFerdigstillVarsel,
            behandlingKlient,
        )

    val journalfoerBrevService = JournalfoerBrevService(db, behandlingService, dokarkivService, vedtaksbrevService)

    val clamAvClient = ClamAvClient(httpClient(), env.requireEnvValue(CLAMAV_ENDPOINT_URL))
    val virusScanService = VirusScanService(clamAvClient)
    val pdfService = PDFService(db, virusScanService, pdfGenerator)

    val brevService =
        BrevService(
            db,
            brevoppretter,
            journalfoerBrevService,
            pdfService,
            brevdistribuerer,
            dokdistKanalKlient,
            oppgaveService,
            brevdataFacade,
            adresseService,
            featureToggleService,
        )

    val safService =
        SafService(
            SafKlient(httpClient(), env.requireEnvValue(SAF_BASE_URL), env.requireEnvValue(SAF_SCOPE)),
        )

    val oversendelseBrevService =
        OversendelseBrevServiceImpl(
            brevRepository = db,
            pdfGenerator = pdfGenerator,
            adresseService = adresseService,
            behandlingService = behandlingService,
            grunnlagService = grunnlagService,
        )

    val notatRepository = NotatRepository(datasource)
    val pdfGeneratorKlient = PdfGeneratorKlient(httpClient(), env.requireEnvValue(PDFGEN_URL))
    val nyNotatService = NyNotatService(notatRepository, pdfGeneratorKlient, dokarkivService, behandlingService)
    val notatService = NotatService(notatRepository, pdfGeneratorKlient, dokarkivService, grunnlagService)

    val tilgangssjekker = Tilgangssjekker(config, httpClient())

    val tilbakekrevingBrevService =
        StrukturertBrevService(
            brevbakerService,
            adresseService,
            db,
            journalfoerBrevService,
            brevdistribuerer,
        )

    private fun httpClient(
        scope: EnvEnum? = null,
        forventStatusSuccess: Boolean = true,
    ) = httpClient(
        forventSuksess = forventStatusSuccess,
        auth = {
            if (scope != null) {
                it.install(Auth) {
                    clientCredential {
                        config =
                            env.append(AZURE_APP_OUTBOUND_SCOPE) { variabler ->
                                krevIkkeNull(variabler[scope]) {
                                    "Mangler outbound scope"
                                }
                            }
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
    REGOPPSLAG_SCOPE,
    REGOPPSLAG_URL,
    ETTERLATTE_PDLTJENESTER_URL,
    ETTERLATTE_PDLTJENESTER_CLIENT_ID,
    SAF_BASE_URL,
    SAF_SCOPE,
    ;

    override fun key() = name
}
