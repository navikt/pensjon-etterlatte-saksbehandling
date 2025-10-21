package no.nav.etterlatte.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.avkorting.AvkortingReparerAarsoppgjoeret
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.AvkortingService
import no.nav.etterlatte.avkorting.AvkortingTidligAlderspensjonService
import no.nav.etterlatte.avkorting.etteroppgjoer.EtteroppgjoerRepository
import no.nav.etterlatte.avkorting.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.avkorting.inntektsjustering.AarligInntektsjusteringService
import no.nav.etterlatte.avkorting.inntektsjustering.MottattInntektsjusteringService
import no.nav.etterlatte.beregning.AnvendtTrygdetidRepository
import no.nav.etterlatte.beregning.BeregnBarnepensjonService
import no.nav.etterlatte.beregning.BeregnOmstillingsstoenadService
import no.nav.etterlatte.beregning.BeregnOverstyrBeregningService
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagRepository
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.grunnbeloep.GrunnbeloepService
import no.nav.etterlatte.klienter.BehandlingKlientImpl
import no.nav.etterlatte.klienter.BehandlingVilkaarsvurderingKlientImpl
import no.nav.etterlatte.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.klienter.TrygdetidKlient
import no.nav.etterlatte.klienter.VedtaksvurderingKlientImpl
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.ApplicationProperties
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.sanksjon.SanksjonRepository
import no.nav.etterlatte.sanksjon.SanksjonService
import no.nav.etterlatte.ytelseMedGrunnlag.BeregningOgAvkortingBrevService

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
    )

class ApplicationContext {
    val config: Config = ConfigFactory.load()
    private val env = Miljoevariabler.systemEnv()
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(env)
    val dataSource = DataSourceBuilder.createDataSource(properties)

    private val featureToggleService: FeatureToggleService =
        FeatureToggleService.initialiser(
            properties = featureToggleProperties(config),
        )

    val vilkaarsvurderingKlient = BehandlingVilkaarsvurderingKlientImpl(config, httpClient())
    val vedtaksvurderingKlient = VedtaksvurderingKlientImpl(config, httpClient())
    val grunnlagKlient = GrunnlagKlientImpl(config, httpClient())
    val trygdetidKlient = TrygdetidKlient(config, httpClient())
    val behandlingKlient = BehandlingKlientImpl(config, httpClient())

    val sanksjonService =
        SanksjonService(
            sanksjonRepository = SanksjonRepository(dataSource),
            behandlingKlient = behandlingKlient,
            featureToggleService = featureToggleService,
        )
    private val beregningsGrunnlagRepository = BeregningsGrunnlagRepository(dataSource)
    val beregningRepository = BeregningRepository(dataSource)
    val anvendtTrygdetidRepository = AnvendtTrygdetidRepository(dataSource)
    val beregningsGrunnlagService =
        BeregningsGrunnlagService(
            beregningsGrunnlagRepository = beregningsGrunnlagRepository,
            beregningRepository = beregningRepository,
            behandlingKlient = behandlingKlient,
            vedtaksvurderingKlient = vedtaksvurderingKlient,
            grunnlagKlient = grunnlagKlient,
        )

    val beregnBarnepensjonService =
        BeregnBarnepensjonService(
            grunnlagKlient = grunnlagKlient,
            vilkaarsvurderingKlient = vilkaarsvurderingKlient,
            beregningsGrunnlagService = beregningsGrunnlagService,
            trygdetidKlient = trygdetidKlient,
            anvendtTrygdetidRepository = anvendtTrygdetidRepository,
        )
    val beregnOmstillingsstoenadService =
        BeregnOmstillingsstoenadService(
            grunnlagKlient = grunnlagKlient,
            vilkaarsvurderingKlient = vilkaarsvurderingKlient,
            trygdetidKlient = trygdetidKlient,
            beregningsGrunnlagService = beregningsGrunnlagService,
            featureToggleService = featureToggleService,
            sanksjonService = sanksjonService,
        )

    val beregnOverstyrBeregningService =
        BeregnOverstyrBeregningService(
            beregningsGrunnlagService = beregningsGrunnlagService,
            grunnlagKlient = grunnlagKlient,
            vilkaarsvurderingKlient = vilkaarsvurderingKlient,
        )
    val beregningService =
        BeregningService(
            beregningRepository = beregningRepository,
            behandlingKlient = behandlingKlient,
            beregnBarnepensjonService = beregnBarnepensjonService,
            beregnOmstillingsstoenadService = beregnOmstillingsstoenadService,
            beregnOverstyrBeregningService = beregnOverstyrBeregningService,
            sanksjonService = sanksjonService,
        )
    val avkortingRepository = AvkortingRepository(dataSource)

    val etteroppgjoerRepository = EtteroppgjoerRepository(dataSource)

    val avkortingReparerAarsoppgjoeret = AvkortingReparerAarsoppgjoeret(avkortingRepository)

    val avkortingService =
        AvkortingService(
            behandlingKlient = behandlingKlient,
            avkortingRepository = avkortingRepository,
            beregningService = beregningService,
            sanksjonService = sanksjonService,
            grunnlagKlient = grunnlagKlient,
            vedtakKlient = vedtaksvurderingKlient,
            avkortingReparerAarsoppgjoeret = avkortingReparerAarsoppgjoeret,
        )
    val avkortingTidligAlderspensjonService =
        AvkortingTidligAlderspensjonService(
            behandlingKlient = behandlingKlient,
            avkortingRepository = avkortingRepository,
        )
    val aarligInntektsjusteringService =
        AarligInntektsjusteringService(
            avkortingService = avkortingService,
            avkortingRepository = avkortingRepository,
            sanksjonService = sanksjonService,
        )
    val mottattInntektsjusteringService = MottattInntektsjusteringService(avkortingService)
    val etteroppgjoerService =
        EtteroppgjoerService(
            avkortingRepository = avkortingRepository,
            sanksjonService = sanksjonService,
            etteroppgjoerRepository = etteroppgjoerRepository,
            avkortingService = avkortingService,
            reparerAarsoppgjoeret = avkortingReparerAarsoppgjoeret,
            vedtakKlient = vedtaksvurderingKlient,
            behandlingKlient = behandlingKlient,
        )
    val beregningOgAvkortingBrevService =
        BeregningOgAvkortingBrevService(
            beregningRepository = beregningRepository,
            avkortingService = avkortingService,
            beregningsGrunnlagService = beregningsGrunnlagService,
            behandlingKlient = behandlingKlient,
        )
    val grunnbeloepService = GrunnbeloepService(repository = GrunnbeloepRepository)
}
