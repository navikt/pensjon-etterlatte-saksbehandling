package no.nav.etterlatte.config.modules

import no.nav.etterlatte.EnvKey.BRUK_NY_VEDTAK_KLIENT
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
import no.nav.etterlatte.behandling.etteroppgjoer.revurdering.EtteroppgjoerRevurderingService
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.klienter.VedtakInternalService
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.behandling.revurdering.AutomatiskRevurderingService
import no.nav.etterlatte.behandling.revurdering.ManuellRevurderingService
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.behandling.sjekkliste.SjekklisteService
import no.nav.etterlatte.behandling.vedtaksbehandling.BehandlingMedBrevService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakEtteroppgjoerService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakKlageService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakSamordningService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakTilbakekrevingService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtaksvurderingRapidService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtaksvurderingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagHenter
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.grunnlag.GrunnlagServiceImpl
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringsHendelseFilter
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseService
import no.nav.etterlatte.inntektsjustering.selvbetjening.InntektsjusteringSelvbetjeningService
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kodeverk.KodeverkService
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
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
    private val env: Miljoevariabler,
    grunnlagServiceOverride: GrunnlagService? = null,
    vedtakKlientOverride: VedtakKlient? = null,
) {
    val brukerService: BrukerService by lazy {
        BrukerServiceImpl(pdltjenesterKlient = klientModule.pdlTjenesterKlient, norg2Klient = klientModule.norg2Klient)
    }

    val saksbehandlerService: SaksbehandlerService by lazy {
        SaksbehandlerServiceImpl(
            dao = daoModule.saksbehandlerInfoDao,
            navAnsattKlient = klientModule.navAnsattKlient,
            entraProxyKlient = klientModule.entraProxyKlient,
        )
    }

    val oppgaveService: OppgaveService by lazy {
        OppgaveService(
            oppgaveDao = daoModule.oppgaveDaoEndringer,
            sakDao = daoModule.sakLesDao,
            hendelseDao = daoModule.hendelseDao,
            hendelser = kafkaModule.behandlingsHendelser,
            saksbehandlerService = saksbehandlerService,
        )
    }

    val oppgaveKommentarService by lazy {
        OppgaveKommentarService(
            oppgaveKommentarDao = daoModule.oppgaveKommentarDao,
            oppgaveService = oppgaveService,
            sakLesDao = daoModule.sakLesDao,
        )
    }

    val nyAldersovergangService by lazy {
        AldersovergangService(daoModule.aldersovergangDao)
    }

    val sakTilgang: SakTilgang by lazy {
        SakTilgangImpl(skrivDao = daoModule.sakSkrivDao, lesDao = daoModule.sakLesDao)
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
            pdltjenesterKlient = klientModule.pdlTjenesterKlient,
            opplysningDao = daoModule.opplysningDao,
            grunnlagHenter = GrunnlagHenter(klientModule.pdlTjenesterKlient),
            oppdaterTilgangService = oppdaterTilgangService,
        )
    }

    val kodeverkService by lazy { KodeverkService(klientModule.kodeverkKlient) }

    val tilgangService by lazy { TilgangServiceSjekkerImpl(daoModule.sakTilgangDao) }

    val sakService: SakServiceImpl by lazy {
        SakServiceImpl(
            skrivDao = daoModule.sakSkrivDao,
            lesDao = daoModule.sakLesDao,
            endringerDao = daoModule.sakendringerDao,
            skjermingKlient = klientModule.skjermingKlient,
            brukerService = brukerService,
            grunnlagService = grunnlagService,
            krrKlient = klientModule.krrKlient,
            pdltjenesterKlient = klientModule.pdlTjenesterKlient,
            featureToggle = featureToggleService,
            tilgangsService = oppdaterTilgangService,
            sakTilgang = sakTilgang,
            aldersovergangService = nyAldersovergangService,
        )
    }

    val doedshendelseService by lazy {
        DoedshendelseService(
            doedshendelseDao = daoModule.doedshendelseDao,
            pdlTjenesterKlient = klientModule.pdlTjenesterKlient,
            gosysOppgaveKlient = klientModule.gosysOppgaveKlient,
            ukjentBeroertDao = daoModule.ukjentBeroertDao,
        )
    }

    val etteroppgjoerOppgaveService by lazy {
        EtteroppgjoerOppgaveService(oppgaveService)
    }

    val etteroppgjoerHendelseService by lazy {
        EtteroppgjoerForbehandlingHendelseService(
            rapidPubliserer = rapid,
            hendelseDao = daoModule.hendelseDao,
            etteroppgjoerForbehandlingDao = daoModule.etteroppgjoerForbehandlingDao,
        )
    }

    val etteroppgjoerService: EtteroppgjoerService by lazy {
        EtteroppgjoerService(
            dao = daoModule.etteroppgjoerDao,
            vedtakKlient = vedtakKlient,
            behandlingService = behandlingService,
            beregningKlient = klientModule.beregningKlient,
            sigrunKlient = klientModule.sigrunKlient,
            etteroppgjoerOppgaveService = etteroppgjoerOppgaveService,
            hendelseDao = daoModule.hendelseDao,
        )
    }

    val etteroppgjoerDataService by lazy {
        EtteroppgjoerDataService(
            behandlingService = behandlingService,
            featureToggleService = featureToggleService,
            vedtakKlient = vedtakKlient,
            beregningKlient = klientModule.beregningKlient,
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
            vedtakKlient = vedtakKlient,
            etteroppgjoerOppgaveService = etteroppgjoerOppgaveService,
            etteroppgjoerDataService = etteroppgjoerDataService,
        )
    }

    val kommerBarnetTilGodeService by lazy {
        KommerBarnetTilGodeService(
            kommerBarnetTilGodeDao = daoModule.kommerBarnetTilGodeDao,
            behandlingDao = daoModule.behandlingDao,
        )
    }

    val aktivitetspliktKopierService by lazy {
        AktivitetspliktKopierService(
            aktivitetspliktAktivitetsgradDao = daoModule.aktivitetspliktAktivitetsgradDao,
            aktivitetspliktUnntakDao = daoModule.aktivitetspliktUnntakDao,
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
            repository = daoModule.vilkaarsvurderingDao,
            behandlingService = behandlingService,
            grunnlagService = grunnlagService,
            behandlingStatus = behandlingsStatusService,
        )
    }

    val aldersovergangService by lazy {
        // TOOD: Se på denne importen.
        no.nav.etterlatte.vilkaarsvurdering.service
            .AldersovergangService(vilkaarsvurderingService)
    }

    val generellBehandlingService by lazy {
        GenerellBehandlingService(
            generellBehandlingDao = daoModule.generellbehandlingDao,
            oppgaveService = oppgaveService,
            behandlingService = behandlingService,
            grunnlagService = grunnlagService,
            hendelseDao = daoModule.hendelseDao,
            saksbehandlerInfoDao = daoModule.saksbehandlerInfoDao,
        )
    }

    val behandlingMedBrevService by lazy {
        BehandlingMedBrevService(daoModule.behandlingMedBrevDao)
    }

    val sjekklisteService by lazy {
        SjekklisteService(
            dao = daoModule.sjekklisteDao,
            behandlingService = behandlingService,
            oppgaveService = oppgaveService,
        )
    }

    val automatiskRevurderingService by lazy {
        AutomatiskRevurderingService(
            revurderingService = revurderingService,
            behandlingService = behandlingService,
            grunnlagService = grunnlagService,
            vedtakKlient = vedtakKlient,
            beregningKlient = klientModule.beregningKlient,
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
        GrunnlagsendringsHendelseFilter(vedtakKlient = vedtakKlient, behandlingService = behandlingService)
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

    val inntektsjusteringSelvbetjeningService by lazy {
        InntektsjusteringSelvbetjeningService(
            oppgaveService = oppgaveService,
            behandlingService = behandlingService,
            vedtakKlient = vedtakKlient,
            rapid = rapid,
            featureToggleService = featureToggleService,
            beregningKlient = klientModule.beregningKlient,
        )
    }

    val behandlingsStatusService: BehandlingStatusServiceImpl by lazy {
        BehandlingStatusServiceImpl(
            behandlingDao = daoModule.behandlingDao,
            behandlingService = behandlingService,
            behandlingInfoDao = daoModule.behandlingInfoDao,
            oppgaveService = oppgaveService,
            grunnlagsendringshendelseService = grunnlagsendringshendelseService,
            generellBehandlingService = generellBehandlingService,
            aktivitetspliktService = aktivitetspliktService,
            saksbehandlerService = saksbehandlerService,
            etteroppgjoerService = etteroppgjoerService,
            forbehandlingService = etteroppgjoerForbehandlingService,
            grunnlagService = grunnlagService,
        )
    }

    val vedtaksvurderingService by lazy {
        VedtaksvurderingService(daoModule.vedtaksvurderingRepository)
    }

    val vedtaksvurderingRapidService by lazy {
        VedtaksvurderingRapidService(
            publiser = { key, melding -> rapid.publiser(key.toString(), verdi = melding) },
        )
    }

    val vedtakKlageService by lazy {
        VedtakKlageService(
            vedtaksvurderingRepository = daoModule.vedtaksvurderingRepository,
            vedtaksvurderingRapidService = vedtaksvurderingRapidService,
        )
    }

    val vedtakTilbakekrevingService by lazy {
        VedtakTilbakekrevingService(
            repository = daoModule.vedtaksvurderingRepository,
            featureToggleService = featureToggleService,
        )
    }

    private val vedtakSamordningService by lazy {
        VedtakSamordningService(daoModule.vedtaksvurderingRepository)
    }

    val vedtakEtteroppgjoerService by lazy {
        VedtakEtteroppgjoerService(
            repository = daoModule.vedtaksvurderingRepository,
            vedtakSamordningService = vedtakSamordningService,
        )
    }

    val trygdetidKlient by lazy {
        klientModule.trygdetidKlient
    }

    val beregningKlient by lazy {
        klientModule.beregningKlient
    }

    val etteroppgjoerRevurderingService: EtteroppgjoerRevurderingService by lazy {
        EtteroppgjoerRevurderingService(
            behandlingService = behandlingService,
            etteroppgjoerService = etteroppgjoerService,
            etteroppgjoerForbehandlingService = etteroppgjoerForbehandlingService,
            grunnlagService = grunnlagService,
            revurderingService = revurderingService,
            vilkaarsvurderingService = vilkaarsvurderingService,
            trygdetidKlient = trygdetidKlient,
            beregningKlient = beregningKlient,
            etteroppgjoerDataService = etteroppgjoerDataService,
        )
    }

    val vedtakKlient: VedtakKlient by lazy {
        vedtakKlientOverride ?: run {
            val brukNyVedtakKlientInternal: Boolean =
                env[BRUK_NY_VEDTAK_KLIENT]?.toBoolean() ?: throw InternfeilException(
                    "Fant ikke miljøvariabel: $BRUK_NY_VEDTAK_KLIENT",
                )

            if (brukNyVedtakKlientInternal) {
                VedtakInternalService(
                    vedtakTilbakekrevingService = vedtakTilbakekrevingService,
                    vedtakKlageService = vedtakKlageService,
                    vedtaksvurderingService = vedtaksvurderingService,
                )
            } else {
                klientModule.vedtakKlient()
            }
        }
    }
}
