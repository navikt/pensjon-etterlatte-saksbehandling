package no.nav.etterlatte.config.modules

import no.nav.etterlatte.behandling.BehandlingServiceImpl
import no.nav.etterlatte.behandling.BehandlingStatusServiceImpl
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.behandling.BrukerServiceImpl
import no.nav.etterlatte.behandling.GyldighetsproevingServiceImpl
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktKopierService
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerDataService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingHendelseService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentService
import no.nav.etterlatte.behandling.etteroppgjoer.oppgave.EtteroppgjoerOppgaveService
import no.nav.etterlatte.behandling.etteroppgjoer.pensjonsgivendeinntekt.PensjonsgivendeInntektService
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.behandling.revurdering.AutomatiskRevurderingService
import no.nav.etterlatte.behandling.revurdering.ManuellRevurderingService
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.behandling.sjekkliste.SjekklisteService
import no.nav.etterlatte.behandling.vedtaksbehandling.BehandlingMedBrevService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagHenter
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.grunnlag.GrunnlagServiceImpl
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringsHendelseFilter
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseService
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kodeverk.KodeverkService
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.oppgave.kommentar.OppgaveKommentarService
import no.nav.etterlatte.sak.SakServiceImpl
import no.nav.etterlatte.sak.SakTilgang
import no.nav.etterlatte.sak.SakTilgangImpl
import no.nav.etterlatte.sak.TilgangServiceSjekkerImpl
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import no.nav.etterlatte.saksbehandler.SaksbehandlerServiceImpl
import no.nav.etterlatte.tilgangsstyring.OppdaterTilgangService
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService

class ServiceModule(
    private val daoModule: DaoModule,
    private val klientModule: KlientModule,
    private val kafkaModule: KafkaModule,
    private val featureToggleService: FeatureToggleService,
    private val rapid: KafkaProdusent<String, String>,
    grunnlagServiceOverride: GrunnlagService? = null,
) {
    // === Grunnleggende services ===

    val brukerService: BrukerService by lazy {
        BrukerServiceImpl(klientModule.pdlTjenesterKlient, klientModule.norg2Klient)
    }

    val saksbehandlerService: SaksbehandlerService by lazy {
        SaksbehandlerServiceImpl(
            daoModule.saksbehandlerInfoDao,
            klientModule.navAnsattKlient,
            klientModule.entraProxyKlient,
        )
    }

    val oppgaveService: OppgaveService by lazy {
        OppgaveService(
            daoModule.oppgaveDaoEndringer,
            daoModule.sakLesDao,
            daoModule.hendelseDao,
            kafkaModule.behandlingsHendelser,
            saksbehandlerService,
        )
    }

    val oppgaveKommentarService by lazy {
        OppgaveKommentarService(
            daoModule.oppgaveKommentarDao,
            oppgaveService,
            daoModule.sakLesDao,
        )
    }

    val nyAldersovergangService by lazy {
        no.nav.etterlatte.grunnlag.aldersovergang
            .AldersovergangService(daoModule.aldersovergangDao)
    }

    val sakTilgang: SakTilgang by lazy {
        SakTilgangImpl(daoModule.sakSkrivDao, daoModule.sakLesDao)
    }

    val oppdaterTilgangService: OppdaterTilgangService by lazy {
        OppdaterTilgangService(
            skjermingKlient = klientModule.skjermingKlient,
            pdltjenesterKlient = klientModule.pdlTjenesterKlient,
            brukerService = brukerService,
            oppgaveService = oppgaveService,
            sakSkrivDao = daoModule.sakSkrivDao,
            sakTilgang = sakTilgang,
            sakLesDao = daoModule.sakLesDao,
            featureToggleService = featureToggleService,
        )
    }

    val grunnlagService: GrunnlagService by lazy {
        grunnlagServiceOverride ?: GrunnlagServiceImpl(
            klientModule.pdlTjenesterKlient,
            daoModule.opplysningDao,
            GrunnlagHenter(klientModule.pdlTjenesterKlient),
            oppdaterTilgangService,
        )
    }

    val kodeverkService by lazy { KodeverkService(klientModule.kodeverkKlient) }

    val tilgangService by lazy { TilgangServiceSjekkerImpl(daoModule.sakTilgangDao) }

    val sakService: SakServiceImpl by lazy {
        SakServiceImpl(
            daoModule.sakSkrivDao,
            daoModule.sakLesDao,
            daoModule.sakendringerDao,
            klientModule.skjermingKlient,
            brukerService,
            grunnlagService,
            klientModule.krrKlient,
            klientModule.pdlTjenesterKlient,
            featureToggleService,
            oppdaterTilgangService,
            sakTilgang,
            nyAldersovergangService,
        )
    }

    val doedshendelseService by lazy {
        DoedshendelseService(
            daoModule.doedshendelseDao,
            klientModule.pdlTjenesterKlient,
            klientModule.gosysOppgaveKlient,
            daoModule.ukjentBeroertDao,
        )
    }

    // === Etteroppgjør services ===

    val etteroppgjoerOppgaveService by lazy {
        EtteroppgjoerOppgaveService(oppgaveService)
    }

    val etteroppgjoerHendelseService by lazy {
        EtteroppgjoerForbehandlingHendelseService(
            rapid,
            daoModule.hendelseDao,
            daoModule.etteroppgjoerForbehandlingDao,
        )
    }

    val etteroppgjoerService: EtteroppgjoerService by lazy {
        EtteroppgjoerService(
            dao = daoModule.etteroppgjoerDao,
            vedtakKlient = klientModule.vedtakKlient,
            behandlingService = behandlingService,
            beregningKlient = klientModule.beregningKlient,
            sigrunKlient = klientModule.sigrunKlient,
            etteroppgjoerOppgaveService = etteroppgjoerOppgaveService,
            hendelseDao = daoModule.hendelseDao,
        )
    }

    val etteroppgjoerDataService by lazy {
        EtteroppgjoerDataService(
            behandlingService,
            featureToggleService,
            klientModule.vedtakKlient,
            klientModule.beregningKlient,
        )
    }

    private val inntektskomponentService by lazy {
        InntektskomponentService(
            klient = klientModule.inntektskomponentKlient,
            featureToggleService = featureToggleService,
        )
    }

    private val pensjonsgivendeInntektService by lazy {
        PensjonsgivendeInntektService(sigrunKlient = klientModule.sigrunKlient)
    }

    val etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService by lazy {
        EtteroppgjoerForbehandlingService(
            dao = daoModule.etteroppgjoerForbehandlingDao,
            etteroppgjoerService = etteroppgjoerService,
            sakDao = daoModule.sakLesDao,
            oppgaveService = oppgaveService,
            inntektskomponentService = inntektskomponentService,
            pensjonsgivendeInntektService = pensjonsgivendeInntektService,
            hendelserService = etteroppgjoerHendelseService,
            beregningKlient = klientModule.beregningKlient,
            behandlingService = behandlingService,
            vedtakKlient = klientModule.vedtakKlient,
            etteroppgjoerOppgaveService = etteroppgjoerOppgaveService,
            etteroppgjoerDataService = etteroppgjoerDataService,
        )
    }

    // === Behandling services ===

    val kommerBarnetTilGodeService by lazy {
        KommerBarnetTilGodeService(daoModule.kommerBarnetTilGodeDao, daoModule.behandlingDao)
    }

    val aktivitetspliktKopierService by lazy {
        AktivitetspliktKopierService(
            daoModule.aktivitetspliktAktivitetsgradDao,
            daoModule.aktivitetspliktUnntakDao,
        )
    }

    val revurderingService: RevurderingService by lazy {
        RevurderingService(
            oppgaveService = oppgaveService,
            grunnlagService = grunnlagService,
            behandlingHendelser = kafkaModule.behandlingsHendelser,
            behandlingDao = daoModule.behandlingDao,
            hendelseDao = daoModule.hendelseDao,
            kommerBarnetTilGodeService = kommerBarnetTilGodeService,
            revurderingDao = daoModule.revurderingDao,
            aktivitetspliktDao = daoModule.aktivitetspliktDao,
            aktivitetspliktKopierService = aktivitetspliktKopierService,
        )
    }

    val gyldighetsproevingService by lazy {
        GyldighetsproevingServiceImpl(behandlingDao = daoModule.behandlingDao)
    }

    val behandlingService: BehandlingServiceImpl by lazy {
        BehandlingServiceImpl(
            behandlingDao = daoModule.behandlingDao,
            behandlingHendelser = kafkaModule.behandlingsHendelser,
            grunnlagsendringshendelseDao = daoModule.grunnlagsendringshendelseDao,
            hendelseDao = daoModule.hendelseDao,
            kommerBarnetTilGodeDao = daoModule.kommerBarnetTilGodeDao,
            oppgaveService = oppgaveService,
            grunnlagService = grunnlagService,
            beregningKlient = klientModule.beregningKlient,
            etteroppgjoerDao = daoModule.etteroppgjoerDao,
            etteroppgjoerForbehandlingDao = daoModule.etteroppgjoerForbehandlingDao,
            etteroppgjoerForbehandlingHendelseService = etteroppgjoerHendelseService,
            etteroppgjoerOppgaveService = etteroppgjoerOppgaveService,
        )
    }

    val vilkaarsvurderingService: VilkaarsvurderingService by lazy {
        VilkaarsvurderingService(
            daoModule.vilkaarsvurderingDao,
            behandlingService,
            grunnlagService,
            behandlingsStatusService,
        )
    }

    val aldersovergangService by lazy {
        no.nav.etterlatte.vilkaarsvurdering.service
            .AldersovergangService(vilkaarsvurderingService)
    }

    val generellBehandlingService by lazy {
        GenerellBehandlingService(
            daoModule.generellbehandlingDao,
            oppgaveService,
            behandlingService,
            grunnlagService,
            daoModule.hendelseDao,
            daoModule.saksbehandlerInfoDao,
        )
    }

    val behandlingMedBrevService by lazy {
        BehandlingMedBrevService(daoModule.behandlingMedBrevDao)
    }

    val sjekklisteService by lazy {
        SjekklisteService(daoModule.sjekklisteDao, behandlingService, oppgaveService)
    }

    val automatiskRevurderingService by lazy {
        AutomatiskRevurderingService(
            revurderingService,
            behandlingService,
            grunnlagService,
            klientModule.vedtakKlient,
            klientModule.beregningKlient,
        )
    }

    val manuellRevurderingService by lazy {
        ManuellRevurderingService(
            revurderingService = revurderingService,
            behandlingService = behandlingService,
            grunnlagService = grunnlagService,
            oppgaveService = oppgaveService,
            grunnlagsendringshendelseDao = daoModule.grunnlagsendringshendelseDao,
        )
    }

    val aktivitetspliktService: AktivitetspliktService by lazy {
        AktivitetspliktService(
            aktivitetspliktDao = daoModule.aktivitetspliktDao,
            aktivitetspliktAktivitetsgradDao = daoModule.aktivitetspliktAktivitetsgradDao,
            aktivitetspliktUnntakDao = daoModule.aktivitetspliktUnntakDao,
            behandlingService = behandlingService,
            grunnlagService = grunnlagService,
            revurderingService = revurderingService,
            statistikkKafkaProducer = kafkaModule.behandlingsHendelser,
            oppgaveService = oppgaveService,
            aktivitetspliktKopierService = aktivitetspliktKopierService,
            featureToggleService = featureToggleService,
        )
    }

    val omregningService by lazy {
        OmregningService(
            behandlingService = behandlingService,
            omregningDao = daoModule.omregningDao,
            oppgaveService = oppgaveService,
        )
    }

    private val grunnlagsendringsHendelseFilter by lazy {
        GrunnlagsendringsHendelseFilter(klientModule.vedtakKlient, behandlingService)
    }

    val grunnlagsendringshendelseService: GrunnlagsendringshendelseService by lazy {
        GrunnlagsendringshendelseService(
            oppgaveService = oppgaveService,
            grunnlagsendringshendelseDao = daoModule.grunnlagsendringshendelseDao,
            behandlingService = behandlingService,
            pdltjenesterKlient = klientModule.pdlTjenesterKlient,
            grunnlagService = grunnlagService,
            sakService = sakService,
            doedshendelseService = doedshendelseService,
            grunnlagsendringsHendelseFilter = grunnlagsendringsHendelseFilter,
            tilgangsService = oppdaterTilgangService,
        )
    }

    val behandlingsStatusService: BehandlingStatusServiceImpl by lazy {
        BehandlingStatusServiceImpl(
            daoModule.behandlingDao,
            behandlingService,
            daoModule.behandlingInfoDao,
            oppgaveService,
            grunnlagsendringshendelseService,
            generellBehandlingService,
            aktivitetspliktService,
            saksbehandlerService,
            etteroppgjoerService,
            etteroppgjoerForbehandlingService,
            grunnlagService,
        )
    }
}
