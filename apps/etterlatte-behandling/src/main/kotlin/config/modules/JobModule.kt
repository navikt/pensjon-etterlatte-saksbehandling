package no.nav.etterlatte.config.modules

import behandling.jobs.etteroppgjoer.LesSkatteoppgjoerHendelserJob
import behandling.jobs.uttrekk.UttrekkLoependeYtelseEtter67Job
import behandling.jobs.uttrekk.UttrekkLoependeYtelseEtter67JobService
import no.nav.etterlatte.behandling.doedshendelse.DoedshendelseReminderService
import no.nav.etterlatte.behandling.jobs.aktivitetsplikt.AktivitetspliktOppgaveUnntakUtloeperJob
import no.nav.etterlatte.behandling.jobs.aktivitetsplikt.AktivitetspliktOppgaveUnntakUtloeperJobService
import no.nav.etterlatte.behandling.jobs.doedsmelding.DoedsmeldingJob
import no.nav.etterlatte.behandling.jobs.doedsmelding.DoedsmeldingReminderJob
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.EtteroppgjoerSvarfristUtloeptJob
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.EtteroppgjoerSvarfristUtloeptJobService
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.LesSkatteoppgjoerHendelserJobService
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.OppdaterSkatteoppgjoerIkkeMottattJob
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.OppdaterSkatteoppgjoerIkkeMottattJobService
import no.nav.etterlatte.behandling.jobs.saksbehandler.SaksbehandlerJob
import no.nav.etterlatte.behandling.jobs.saksbehandler.SaksbehandlerJobService
import no.nav.etterlatte.behandling.jobs.sjekkadressebeskyttelse.SjekkAdressebeskyttelseJob
import no.nav.etterlatte.behandling.jobs.sjekkadressebeskyttelse.SjekkAdressebeskyttelseJobService
import no.nav.etterlatte.behandling.vedtaksvurdering.outbox.OutboxJob
import no.nav.etterlatte.config.JobbKeys.JOBB_DOEDSMELDINGER_REMINDER_OPENING_HOURS
import no.nav.etterlatte.config.JobbKeys.JOBB_METRIKKER_OPENING_HOURS
import no.nav.etterlatte.config.JobbKeys.JOBB_SAKSBEHANDLER_OPENING_HOURS
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseJobService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktService
import no.nav.etterlatte.inntektsjustering.AarligInntektsjusteringJobbService
import no.nav.etterlatte.jobs.MetrikkerJob
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.OpeningHours
import no.nav.etterlatte.libs.common.isProd
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.AppConfig.ELECTOR_PATH
import no.nav.etterlatte.metrics.BehandlingMetrics
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.sql.DataSource

class JobModule(
    private val env: Miljoevariabler,
    private val dataSource: DataSource,
    private val daoModule: DaoModule,
    private val klientModule: KlientModule,
    private val kafkaModule: KafkaModule,
    private val serviceModule: ServiceModule,
    private val featureToggleService: FeatureToggleService,
    private val rapid: KafkaProdusent<String, String>,
) {
    private val leaderElectionKlient by lazy {
        LeaderElection(env[ELECTOR_PATH], klientModule.leaderElectionHttpClient)
    }

    private fun erLeader(): Boolean = leaderElectionKlient.isLeader()

    private val saksbehandlerJobService by lazy {
        SaksbehandlerJobService(
            saksbehandlerInfoDao = daoModule.saksbehandlerInfoDao,
            navAnsattKlient = klientModule.navAnsattKlient,
            entraProxyKlient = klientModule.entraProxyKlient,
        )
    }

    private val doedshendelseReminderService by lazy {
        DoedshendelseReminderService(
            doedshendelseDao = daoModule.doedshendelseDao,
            behandlingService = serviceModule.behandlingService,
            oppgaveService = serviceModule.oppgaveService,
            sakLesDao = daoModule.sakLesDao,
        )
    }

    private val doedshendelseJobService by lazy {
        DoedshendelseJobService(
            doedshendelseDao = daoModule.doedshendelseDao,
            doedshendelseKontrollpunktService =
                DoedshendelseKontrollpunktService(
                    pdlTjenesterKlient = klientModule.pdlTjenesterKlient,
                    grunnlagsendringshendelseDao = daoModule.grunnlagsendringshendelseDao,
                    oppgaveService = serviceModule.oppgaveService,
                    sakService = serviceModule.sakService,
                    pesysKlient = klientModule.pesysKlient,
                    behandlingService = serviceModule.behandlingService,
                ),
            grunnlagsendringshendelseService = serviceModule.grunnlagsendringshendelseService,
            sakService = serviceModule.sakService,
            dagerGamleHendelserSomSkalKjoeres = if (isProd()) 5 else 0,
            deodshendelserProducer = kafkaModule.doedshendelserProducer,
            grunnlagService = serviceModule.grunnlagService,
            pdlTjenesterKlient = klientModule.pdlTjenesterKlient,
            krrKlient = klientModule.krrKlient,
        )
    }

    val lesSkatteoppgjoerHendelserJobService by lazy {
        LesSkatteoppgjoerHendelserJobService(
            dao = daoModule.skatteoppgjoerHendelserDao,
            sigrunKlient = klientModule.sigrunKlient,
            etteroppgjoerService = serviceModule.etteroppgjoerService,
            sakService = serviceModule.sakService,
        )
    }

    private val uttrekkLoependeYtelseEtter67JobService by lazy {
        UttrekkLoependeYtelseEtter67JobService(
            vedtakKlient = serviceModule.vedtakKlient,
            sakService = serviceModule.sakService,
            aldersovergangService = serviceModule.nyAldersovergangService,
            featureToggleService = featureToggleService,
        )
    }

    val oppdaterSkatteoppgjoerIkkeMottattJobService by lazy {
        OppdaterSkatteoppgjoerIkkeMottattJobService(
            featureToggleService = featureToggleService,
            etteroppgjoerOppgaveService = serviceModule.etteroppgjoerOppgaveService,
            etteroppgjoerService = serviceModule.etteroppgjoerService,
            vedtakKlient = serviceModule.vedtakKlient,
        )
    }

    val etteroppgjoerSvarfristUtloeptJobService by lazy {
        EtteroppgjoerSvarfristUtloeptJobService(
            etteroppgjoerService = serviceModule.etteroppgjoerService,
            oppgaveService = serviceModule.oppgaveService,
            featureToggleService = featureToggleService,
        )
    }

    private val aktivitetspliktOppgaveUnntakUtloeperJobService by lazy {
        AktivitetspliktOppgaveUnntakUtloeperJobService(
            aktivitetspliktDao = daoModule.aktivitetspliktDao,
            aktivitetspliktService = serviceModule.aktivitetspliktService,
            oppgaveService = serviceModule.oppgaveService,
            vedtakKlient = serviceModule.vedtakKlient,
            featureToggleService = featureToggleService,
        )
    }

    val sjekkAdressebeskyttelseJobService by lazy {
        SjekkAdressebeskyttelseJobService(
            sjekkAdressebeskyttelseJobDao = daoModule.sjekkAdressebeskyttelseJobDao,
            pdlTjenesterKlient = klientModule.pdlTjenesterKlient,
            tilgangService = serviceModule.oppdaterTilgangService,
            grunnlagService = serviceModule.grunnlagService,
            featureToggleService = featureToggleService,
            sakLesDao = daoModule.sakLesDao,
        )
    }

    val aarligInntektsjusteringJobbService by lazy {
        AarligInntektsjusteringJobbService(
            omregningService = serviceModule.omregningService,
            sakService = serviceModule.sakService,
            behandlingService = serviceModule.behandlingService,
            revurderingService = serviceModule.revurderingService,
            vedtakKlient = serviceModule.vedtakKlient,
            grunnlagService = serviceModule.grunnlagService,
            beregningKlient = klientModule.beregningKlient,
            pdlTjenesterKlient = klientModule.pdlTjenesterKlient,
            oppgaveService = serviceModule.oppgaveService,
            rapid = rapid,
            featureToggleService = featureToggleService,
            aldersovergangService = serviceModule.nyAldersovergangService,
            etteroppgjoerForbehandlingService = serviceModule.etteroppgjoerForbehandlingService,
        )
    }

    val metrikkerJob: MetrikkerJob by lazy {
        MetrikkerJob(
            uthenter =
                BehandlingMetrics(
                    oppgaveMetrikkerDao = daoModule.oppgaveMetrikkerDao,
                    behandlingerMetrikkerDao = daoModule.behandlingMetrikkerDao,
                    gjenopprettingDao = daoModule.gjenopprettingMetrikkerDao,
                ),
            erLeader = { erLeader() },
            initialDelay = Duration.of(6, ChronoUnit.MINUTES).toMillis(),
            periode = Duration.of(10, ChronoUnit.MINUTES),
            openingHours = env.requireEnvValue(JOBB_METRIKKER_OPENING_HOURS).let { OpeningHours.of(it) },
        )
    }

    val uttrekkLoependeYtelseEtter67Job: UttrekkLoependeYtelseEtter67Job by lazy {
        UttrekkLoependeYtelseEtter67Job(
            service = uttrekkLoependeYtelseEtter67JobService,
            dataSource = dataSource,
            sakTilgangDao = daoModule.sakTilgangDao,
            erLeader = { erLeader() },
            initialDelay = Duration.of(8, ChronoUnit.MINUTES).toMillis(),
            interval = Duration.of(1, ChronoUnit.HOURS),
        )
    }

    val aktivitetspliktOppgaveUnntakUtloeperJob: AktivitetspliktOppgaveUnntakUtloeperJob by lazy {
        AktivitetspliktOppgaveUnntakUtloeperJob(
            aktivitetspliktOppgaveUnntakUtloeperJobService = aktivitetspliktOppgaveUnntakUtloeperJobService,
            erLeader = { erLeader() },
            initialDelay = Duration.of(5, ChronoUnit.MINUTES).toMillis(),
            interval = Duration.of(1, ChronoUnit.HOURS),
        )
    }

    val doedsmeldingerJob: DoedsmeldingJob by lazy {
        DoedsmeldingJob(
            doedshendelseService = doedshendelseJobService,
            erLeader = { erLeader() },
            initialDelay =
                if (isProd()) {
                    Duration.of(3, ChronoUnit.MINUTES).toMillis()
                } else {
                    Duration.of(20, ChronoUnit.MINUTES).toMillis()
                },
            interval = if (isProd()) Duration.of(1, ChronoUnit.HOURS) else Duration.of(10, ChronoUnit.HOURS),
            dataSource = dataSource,
            sakTilgangDao = daoModule.sakTilgangDao,
        )
    }

    val doedsmeldingerReminderJob: DoedsmeldingReminderJob by lazy {
        DoedsmeldingReminderJob(
            doedshendelseReminderService = doedshendelseReminderService,
            erLeader = { erLeader() },
            initialDelay = Duration.of(4, ChronoUnit.MINUTES).toMillis(),
            interval = if (isProd()) Duration.of(1, ChronoUnit.DAYS) else Duration.of(1, ChronoUnit.HOURS),
            dataSource = dataSource,
            sakTilgangDao = daoModule.sakTilgangDao,
            openingHours = env.requireEnvValue(JOBB_DOEDSMELDINGER_REMINDER_OPENING_HOURS).let { OpeningHours.of(it) },
        )
    }

    val saksbehandlerJob: SaksbehandlerJob by lazy {
        SaksbehandlerJob(
            saksbehandlerJobService = saksbehandlerJobService,
            { erLeader() },
            initialDelay = Duration.of(2, ChronoUnit.MINUTES).toMillis(),
            interval = Duration.of(20, ChronoUnit.MINUTES),
            openingHours = env.requireEnvValue(JOBB_SAKSBEHANDLER_OPENING_HOURS).let { OpeningHours.of(it) },
        )
    }

    val oppdaterSkatteoppgjoerIkkeMottattJob: OppdaterSkatteoppgjoerIkkeMottattJob by lazy {
        OppdaterSkatteoppgjoerIkkeMottattJob(
            oppdaterSkatteoppgjoerIkkeMottattJobService = oppdaterSkatteoppgjoerIkkeMottattJobService,
            erLeader = { erLeader() },
            initialDelay = Duration.of(1, ChronoUnit.MINUTES).toMillis(),
            interval = Duration.of(5, ChronoUnit.MINUTES),
            dataSource = dataSource,
            sakTilgangDao = daoModule.sakTilgangDao,
        )
    }

    val etteroppgjoerSvarfristUtloeptJob: EtteroppgjoerSvarfristUtloeptJob by lazy {
        EtteroppgjoerSvarfristUtloeptJob(
            etteroppgjoerSvarfristUtloeptJobService = etteroppgjoerSvarfristUtloeptJobService,
            erLeader = { erLeader() },
            initialDelay = Duration.of(5, ChronoUnit.MINUTES).toMillis(),
            interval = if (isProd()) Duration.of(1, ChronoUnit.DAYS) else Duration.of(6, ChronoUnit.MINUTES),
            dataSource = dataSource,
            sakTilgangDao = daoModule.sakTilgangDao,
        )
    }

    val sjekkAdressebeskyttelseJob: SjekkAdressebeskyttelseJob by lazy {
        SjekkAdressebeskyttelseJob(
            service = sjekkAdressebeskyttelseJobService,
            sakTilgangDao = daoModule.sakTilgangDao,
            dataSource = dataSource,
            erLeader = { erLeader() },
            initialDelay = Duration.of(5, ChronoUnit.MINUTES).toMillis(),
            interval = Duration.of(5, ChronoUnit.MINUTES),
        )
    }

    val lesSkatteoppgjoerHendelserJob: LesSkatteoppgjoerHendelserJob by lazy {
        LesSkatteoppgjoerHendelserJob(
            lesSkatteoppgjoerHendelserJobService = lesSkatteoppgjoerHendelserJobService,
            erLeader = { erLeader() },
            initialDelay = Duration.of(3, ChronoUnit.MINUTES).toMillis(),
            interval =
                if (isProd()) {
                    Duration.of(1, ChronoUnit.HOURS)
                } else {
                    Duration.of(5, ChronoUnit.MINUTES)
                },
            hendelserBatchSize = 1000,
            dataSource = dataSource,
            sakTilgangDao = daoModule.sakTilgangDao,
            featureToggleService = featureToggleService,
        )
    }

    val outboxJob: OutboxJob by lazy {
        OutboxJob(
            erLeader = { erLeader() },
            outboxService = serviceModule.outboxService,
            initialDelay = Duration.of(2, ChronoUnit.MINUTES).toMillis(),
            periode = Duration.of(1, ChronoUnit.MINUTES),
        )
    }
}
