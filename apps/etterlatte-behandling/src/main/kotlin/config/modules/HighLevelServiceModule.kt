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
import no.nav.etterlatte.behandling.vedtaksvurdering.service.AutomatiskVedtakBehandlingService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakBehandlingService
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
    val bosattUtlandService by lazy {
        BosattUtlandService(daoModule.bosattUtlandDao)
    }

    private val klageBrevService by lazy {
        KlageBrevService(klientModule.brevApiKlient)
    }

    val migreringService by lazy {
        MigreringService(serviceModule.behandlingService)
    }

    val behandlingInfoService by lazy {
        BehandlingInfoService(
            behandlingInfoDao = daoModule.behandlingInfoDao,
            behandlingService = serviceModule.behandlingService,
            behandlingsstatusService = serviceModule.behandlingsStatusService,
        )
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
            vedtakKlient = serviceModule.vedtakKlient,
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
            etteroppgjoerRevurderingService = serviceModule.etteroppgjoerRevurderingService,
            featureToggleService = featureToggleService,
        )
    }

    val vedtakBehandlingService by lazy {
        VedtakBehandlingService(
            vedtaksvurderingRepository = daoModule.vedtaksvurderingRepository,
            beregningKlient = klientModule.beregningKlient,
            vilkaarsvurderingService = serviceModule.vilkaarsvurderingService,
            behandlingStatusService = serviceModule.behandlingsStatusService,
            behandlingService = serviceModule.behandlingService,
            samordningsKlient = klientModule.samordningKlient,
            trygdetidKlient = klientModule.trygdetidKlient,
            etteroppgjorRevurderingService = etteroppgjoerRevurderingService,
            sakLesDao = daoModule.sakLesDao,
        )
    }

    val tilbakekrevingBrevService by lazy {
        TilbakekrevingBrevService(
            sakService = serviceModule.sakService,
            brevKlient = klientModule.brevKlient,
            brevApiKlient = klientModule.brevApiKlient,
            vedtakKlient = serviceModule.vedtakKlient,
            grunnlagService = serviceModule.grunnlagService,
            oppgaveService = serviceModule.oppgaveService,
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
            vedtakKlient = serviceModule.vedtakKlient,
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
            vedtakKlient = serviceModule.vedtakKlient,
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
            vedtakKlient = serviceModule.vedtakKlient,
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
            vedtakKlient = serviceModule.vedtakKlient,
            brevApiKlient = klientModule.brevApiKlient,
            brevService = brevService,
            tilbakekrevingKlient = klientModule.tilbakekrevingKlient,
            tilbakekrevinghendelser = kafkaModule.tilbakekrevingHendelserService,
        )
    }

    val gosysOppgaveService by lazy {
        GosysOppgaveServiceImpl(
            gosysOppgaveKlient = klientModule.gosysOppgaveKlient,
            oppgaveService = serviceModule.oppgaveService,
            saksbehandlerService = serviceModule.saksbehandlerService,
            saksbehandlerInfoDao = daoModule.saksbehandlerInfoDao,
            pdltjeneserKlient = klientModule.pdlTjenesterKlient,
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
            behandlingService = serviceModule.behandlingService,
            etteroppgjoerService = serviceModule.etteroppgjoerService,
            etteroppgjoerForbehandlingService = serviceModule.etteroppgjoerForbehandlingService,
            grunnlagService = serviceModule.grunnlagService,
            revurderingService = serviceModule.revurderingService,
            vilkaarsvurderingService = serviceModule.vilkaarsvurderingService,
            trygdetidKlient = klientModule.trygdetidKlient,
            beregningKlient = klientModule.beregningKlient,
            etteroppgjoerDataService = serviceModule.etteroppgjoerDataService,
        )
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

    val automatiskVedtakBehandlingService by lazy {
        AutomatiskVedtakBehandlingService(
            service = vedtakBehandlingService,
            oppgaveService = serviceModule.oppgaveService,
            vedtaksvurderingService = serviceModule.vedtaksvurderingService,
        )
    }
}
