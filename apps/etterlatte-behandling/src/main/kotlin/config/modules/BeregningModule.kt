package no.nav.etterlatte.config.modules

import io.ktor.server.routing.Route
import no.nav.etterlatte.EnvKey.BRUK_INTERN_BEREGNING
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.modules.beregning.BeregnBarnepensjonService
import no.nav.etterlatte.modules.beregning.BeregnOmstillingsstoenadService
import no.nav.etterlatte.modules.beregning.BeregnOverstyrBeregningService
import no.nav.etterlatte.modules.beregning.BeregningService
import no.nav.etterlatte.modules.beregning.BeregningTilgangService
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingReparerAarsoppgjoeret
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingService
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingTidligAlderspensjonService
import no.nav.etterlatte.modules.beregning.avkorting.avkorting
import no.nav.etterlatte.modules.beregning.avkorting.inntektsjustering.AarligInntektsjusteringService
import no.nav.etterlatte.modules.beregning.avkorting.inntektsjustering.MottattInntektsjusteringService
import no.nav.etterlatte.modules.beregning.beregning
import no.nav.etterlatte.modules.beregning.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.modules.beregning.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.modules.beregning.grunnbeloep.GrunnbeloepService
import no.nav.etterlatte.modules.beregning.grunnbeloep.grunnbeloep
import no.nav.etterlatte.modules.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.modules.beregning.grunnlag.beregningsGrunnlag
import no.nav.etterlatte.modules.beregning.sanksjon.SanksjonService
import no.nav.etterlatte.modules.beregning.sanksjon.sanksjon
import no.nav.etterlatte.modules.beregning.ytelseMedGrunnlag.BeregningOgAvkortingBrevService
import no.nav.etterlatte.modules.beregning.ytelseMedGrunnlag.ytelseMedGrunnlag

class BeregningModule(
    env: Miljoevariabler,
    private val repositoryModule: BeregningRepositoryModule,
    private val serviceModule: ServiceModule,
    private val featureToggleService: FeatureToggleService,
) {
    private val aktivert = env.getOrDefault(BRUK_INTERN_BEREGNING, "false").toBooleanStrict()

    private val tilgangService by lazy { BeregningTilgangService(serviceModule.tilgangService) }

    private val sanksjonService by lazy {
        SanksjonService(
            behandlingService = serviceModule.behandlingService,
            sanksjonRepository = repositoryModule.sanksjonRepository,
            featureToggleService = featureToggleService,
            behandlingStatusService = serviceModule.behandlingsStatusService,
        )
    }

    private val beregningsGrunnlagService by lazy {
        BeregningsGrunnlagService(
            beregningsGrunnlagRepository = repositoryModule.beregningsGrunnlagRepository,
            beregningRepository = repositoryModule.beregningRepository,
            behandlingService = serviceModule.behandlingService,
            vedtaksvurderingService = serviceModule.vedtaksvurderingService,
            grunnlagService = serviceModule.grunnlagService,
            sanksjonService = sanksjonService,
            behandlingStatusService = serviceModule.behandlingsStatusService,
            behandlingTilgangsSjekk = tilgangService,
        )
    }

    private val beregnBarnepensjonService by lazy {
        BeregnBarnepensjonService(
            grunnlagService = serviceModule.grunnlagService,
            vilkaarsvurderingService = serviceModule.vilkaarsvurderingService,
            beregningsGrunnlagService = beregningsGrunnlagService,
            trygdetidService = serviceModule.trygdetidService,
            anvendtTrygdetidRepository = repositoryModule.anvendtTrygdetidRepository,
            featureToggleService = featureToggleService,
        )
    }

    private val beregnOmstillingsstoenadService by lazy {
        BeregnOmstillingsstoenadService(
            grunnlagService = serviceModule.grunnlagService,
            vilkaarsvurderingService = serviceModule.vilkaarsvurderingService,
            trygdetidService = serviceModule.trygdetidService,
            beregningsGrunnlagService = beregningsGrunnlagService,
            sanksjonService = sanksjonService,
            featureToggleService = featureToggleService,
        )
    }

    private val beregnOverstyrBeregningService by lazy {
        BeregnOverstyrBeregningService(
            grunnlagService = serviceModule.grunnlagService,
            beregningsGrunnlagService = beregningsGrunnlagService,
            vilkaarsvurderingService = serviceModule.vilkaarsvurderingService,
        )
    }

    private val beregningService by lazy {
        BeregningService(
            beregningRepository = repositoryModule.beregningRepository,
            behandlingService = serviceModule.behandlingService,
            beregnBarnepensjonService = beregnBarnepensjonService,
            beregnOmstillingsstoenadService = beregnOmstillingsstoenadService,
            beregnOverstyrBeregningService = beregnOverstyrBeregningService,
            behandlingStatusService = serviceModule.behandlingsStatusService,
        )
    }

    private val reparerAarsoppgjoeret by lazy { AvkortingReparerAarsoppgjoeret(repositoryModule.avkortingRepository) }

    private val avkortingService by lazy {
        AvkortingService(
            behandlingService = serviceModule.behandlingService,
            avkortingRepository = repositoryModule.avkortingRepository,
            beregningService = beregningService,
            sanksjonService = sanksjonService,
            aldersovergangService = serviceModule.nyAldersovergangService,
            vedtaksvurderingService = serviceModule.vedtaksvurderingService,
            avkortingReparerAarsoppgjoeret = reparerAarsoppgjoeret,
            featureToggleService = featureToggleService,
            behandlingStatusService = serviceModule.behandlingsStatusService,
            behandlingTilgangsSjekk = tilgangService,
        )
    }

    private val tidligAlderspensjonService by lazy {
        AvkortingTidligAlderspensjonService(
            behandlingService = serviceModule.behandlingService,
            avkortingRepository = repositoryModule.avkortingRepository,
            oppgaveService = serviceModule.oppgaveService,
        )
    }

    private val aarligInntektsjusteringService by lazy {
        AarligInntektsjusteringService(
            avkortingService = avkortingService,
            avkortingRepository = repositoryModule.avkortingRepository,
            sanksjonService = sanksjonService,
        )
    }

    private val mottattInntektsjusteringService by lazy { MottattInntektsjusteringService(avkortingService) }

    private val etteroppgjoerService by lazy {
        EtteroppgjoerService(
            avkortingRepository = repositoryModule.avkortingRepository,
            sanksjonService = sanksjonService,
            etteroppgjoerRepository = repositoryModule.etteroppgjoerRepository,
            avkortingService = avkortingService,
            reparerAarsoppgjoeret = reparerAarsoppgjoeret,
            vedtakEtteroppgjoerService = serviceModule.vedtakEtteroppgjoerService,
            behandlingService = serviceModule.behandlingService,
            featureToggleService = featureToggleService,
            forbehandlingService = serviceModule.etteroppgjoerForbehandlingService,
        )
    }

    private val beregningOgAvkortingBrevService by lazy {
        BeregningOgAvkortingBrevService(
            beregningRepository = repositoryModule.beregningRepository,
            avkortingService = avkortingService,
            beregningsGrunnlagService = beregningsGrunnlagService,
            behandlingService = serviceModule.behandlingService,
        )
    }

    fun registrerRoutes(route: Route) {
        if (!aktivert) return

        with(route) {
            beregning(beregningService, tilgangService)
            beregningsGrunnlag(beregningsGrunnlagService, tilgangService)
            avkorting(
                avkortingService,
                tilgangService,
                tidligAlderspensjonService,
                aarligInntektsjusteringService,
                mottattInntektsjusteringService,
                etteroppgjoerService,
            )
            sanksjon(sanksjonService, tilgangService)
            ytelseMedGrunnlag(beregningOgAvkortingBrevService, tilgangService)
            grunnbeloep(GrunnbeloepService(GrunnbeloepRepository))
        }
    }
}
