package no.nav.etterlatte.config.modules

import no.nav.etterlatte.behandling.BehandlingFactory
import no.nav.etterlatte.behandling.VedtaksbrevService
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktOppgaveService
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoService
import no.nav.etterlatte.behandling.bosattutland.BosattUtlandService
import no.nav.etterlatte.behandling.etteroppgjoer.brev.EtteroppgjoerForbehandlingBrevService
import no.nav.etterlatte.behandling.etteroppgjoer.brev.EtteroppgjoerRevurderingBrevService
import no.nav.etterlatte.behandling.etteroppgjoer.revurdering.EtteroppgjoerRevurderingService
import no.nav.etterlatte.behandling.klage.KlageBrevService
import no.nav.etterlatte.behandling.klage.KlageServiceImpl
import no.nav.etterlatte.behandling.omregning.MigreringService
import no.nav.etterlatte.behandling.revurdering.OmgjoeringKlageRevurderingService
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingService
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.brev.TilbakekrevingBrevService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveServiceImpl

class HighLevelServiceModule(
    private val daoModule: DaoModule,
    private val klientModule: KlientModule,
    private val kafkaModule: KafkaModule,
    private val serviceModule: ServiceModule,
    private val featureToggleService: FeatureToggleService,
) {
    val behandlingInfoService by lazy {
        BehandlingInfoService(
            daoModule.behandlingInfoDao,
            serviceModule.behandlingService,
            serviceModule.behandlingsStatusService,
        )
    }

    val bosattUtlandService by lazy {
        BosattUtlandService(bosattUtlandDao = daoModule.bosattUtlandDao)
    }

    private val klageBrevService by lazy {
        KlageBrevService(klientModule.brevApiKlient)
    }

    val klageService by lazy {
        KlageServiceImpl(
            klageDao = daoModule.klageDao,
            sakDao = daoModule.sakLesDao,
            behandlingService = serviceModule.behandlingService,
            hendelseDao = daoModule.hendelseDao,
            oppgaveService = serviceModule.oppgaveService,
            klageKlient = klientModule.klageKlient,
            klageHendelser = kafkaModule.klageHendelser,
            vedtakKlient = klientModule.vedtakKlient,
            featureToggleService = featureToggleService,
            klageBrevService = klageBrevService,
        )
    }

    val omgjoeringKlageRevurderingService by lazy {
        OmgjoeringKlageRevurderingService(
            revurderingService = serviceModule.revurderingService,
            oppgaveService = serviceModule.oppgaveService,
            klageService = klageService,
            behandlingDao = daoModule.behandlingDao,
            grunnlagService = serviceModule.grunnlagService,
        )
    }

    val tilbakekrevingBrevService by lazy {
        TilbakekrevingBrevService(
            serviceModule.sakService,
            klientModule.brevKlient,
            klientModule.brevApiKlient,
            klientModule.vedtakKlient,
            serviceModule.grunnlagService,
        )
    }

    val etteroppgjoerForbehandlingBrevService by lazy {
        EtteroppgjoerForbehandlingBrevService(
            brevKlient = klientModule.brevKlient,
            grunnlagService = serviceModule.grunnlagService,
            etteroppgjoerForbehandlingService = serviceModule.etteroppgjoerForbehandlingService,
            behandlingService = serviceModule.behandlingService,
            etteroppgjoerOppgaveService = serviceModule.etteroppgjoerOppgaveService,
        )
    }

    val etteroppgjoerRevurderingBrevService by lazy {
        EtteroppgjoerRevurderingBrevService(
            grunnlagService = serviceModule.grunnlagService,
            vedtakKlient = klientModule.vedtakKlient,
            brevKlient = klientModule.brevKlient,
            behandlingService = serviceModule.behandlingService,
            etteroppgjoerForbehandlingService = serviceModule.etteroppgjoerForbehandlingService,
            beregningKlient = klientModule.beregningKlient,
            brevApiKlient = klientModule.brevApiKlient,
            etteroppgjoerService = serviceModule.etteroppgjoerService,
        )
    }

    private val vedtaksbrevService by lazy {
        VedtaksbrevService(
            grunnlagService = serviceModule.grunnlagService,
            vedtakKlient = klientModule.vedtakKlient,
            brevKlient = klientModule.brevKlient,
            behandlingService = serviceModule.behandlingService,
            beregningKlient = klientModule.beregningKlient,
            behandlingInfoService = behandlingInfoService,
            trygdetidKlient = klientModule.trygdetidKlient,
            vilkaarsvurderingService = serviceModule.vilkaarsvurderingService,
            sakService = serviceModule.sakService,
            klageService = klageService,
            kodeverkService = serviceModule.kodeverkService,
        )
    }

    val brevService by lazy {
        BrevService(
            behandlingMedBrevService = serviceModule.behandlingMedBrevService,
            behandlingService = serviceModule.behandlingService,
            brevApiKlient = klientModule.brevApiKlient,
            vedtakKlient = klientModule.vedtakKlient,
            tilbakekrevingBrevService = tilbakekrevingBrevService,
            etteroppgjoerForbehandlingBrevService = etteroppgjoerForbehandlingBrevService,
            etteroppgjoerRevurderingBrevService = etteroppgjoerRevurderingBrevService,
            vedtaksbrevService = vedtaksbrevService,
        )
    }

    val tilbakekrevingService by lazy {
        TilbakekrevingService(
            tilbakekrevingDao = daoModule.tilbakekrevingDao,
            sakDao = daoModule.sakLesDao,
            hendelseDao = daoModule.hendelseDao,
            behandlingService = serviceModule.behandlingService,
            oppgaveService = serviceModule.oppgaveService,
            vedtakKlient = klientModule.vedtakKlient,
            brevApiKlient = klientModule.brevApiKlient,
            brevService = brevService,
            tilbakekrevingKlient = klientModule.tilbakekrevingKlient,
            tilbakekrevinghendelser = kafkaModule.tilbakekrevingHendelserService,
        )
    }

    val gosysOppgaveService by lazy {
        GosysOppgaveServiceImpl(
            klientModule.gosysOppgaveKlient,
            serviceModule.oppgaveService,
            serviceModule.saksbehandlerService,
            daoModule.saksbehandlerInfoDao,
            klientModule.pdlTjenesterKlient,
        )
    }

    val behandlingFactory by lazy {
        BehandlingFactory(
            oppgaveService = serviceModule.oppgaveService,
            grunnlagService = serviceModule.grunnlagService,
            revurderingService = serviceModule.revurderingService,
            gyldighetsproevingService = serviceModule.gyldighetsproevingService,
            sakService = serviceModule.sakService,
            behandlingDao = daoModule.behandlingDao,
            hendelseDao = daoModule.hendelseDao,
            behandlingHendelser = kafkaModule.behandlingsHendelser,
            kommerBarnetTilGodeService = serviceModule.kommerBarnetTilGodeService,
            vilkaarsvurderingService = serviceModule.vilkaarsvurderingService,
            behandlingInfoService = behandlingInfoService,
            tilgangsService = serviceModule.oppdaterTilgangService,
        )
    }

    val etteroppgjoerRevurderingService by lazy {
        EtteroppgjoerRevurderingService(
            serviceModule.behandlingService,
            serviceModule.etteroppgjoerService,
            serviceModule.etteroppgjoerForbehandlingService,
            serviceModule.grunnlagService,
            serviceModule.revurderingService,
            serviceModule.vilkaarsvurderingService,
            klientModule.trygdetidKlient,
            klientModule.beregningKlient,
            serviceModule.etteroppgjoerDataService,
        )
    }

    val migreringService by lazy {
        MigreringService(behandlingService = serviceModule.behandlingService)
    }

    val aktivitetspliktOppgaveService by lazy {
        AktivitetspliktOppgaveService(
            aktivitetspliktService = serviceModule.aktivitetspliktService,
            oppgaveService = serviceModule.oppgaveService,
            sakService = serviceModule.sakService,
            aktivitetspliktBrevDao = daoModule.aktivitetspliktBrevDao,
            brevApiKlient = klientModule.brevApiKlient,
            behandlingService = serviceModule.behandlingService,
            beregningKlient = klientModule.beregningKlient,
        )
    }
}
