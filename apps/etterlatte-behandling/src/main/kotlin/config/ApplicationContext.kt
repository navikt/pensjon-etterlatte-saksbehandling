package no.nav.etterlatte.config

import behandling.jobs.etteroppgjoer.LesSkatteoppgjoerHendelserJob
import behandling.jobs.uttrekk.UttrekkLoependeYtelseEtter67Job
import behandling.jobs.uttrekk.UttrekkLoependeYtelseEtter67JobService
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.HttpClient
import no.nav.etterlatte.EnvKey.ETTERLATTE_KLAGE_API_URL
import no.nav.etterlatte.EnvKey.ETTERLATTE_TILBAKEKREVING_URL
import no.nav.etterlatte.EnvKey.HTTP_PORT
import no.nav.etterlatte.EnvKey.NAVANSATT_URL
import no.nav.etterlatte.EnvKey.NORG2_URL
import no.nav.etterlatte.EnvKey.SKJERMING_URL
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.arbeidOgInntekt.ArbeidOgInntektKlient
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingFactory
import no.nav.etterlatte.behandling.BehandlingRequestLogger
import no.nav.etterlatte.behandling.BehandlingServiceImpl
import no.nav.etterlatte.behandling.BehandlingStatusServiceImpl
import no.nav.etterlatte.behandling.BehandlingsHendelserKafkaProducerImpl
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.behandling.BrukerServiceImpl
import no.nav.etterlatte.behandling.GyldighetsproevingServiceImpl
import no.nav.etterlatte.behandling.VedtaksbrevService
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktBrevDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktKopierService
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktOppgaveService
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktService
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakDao
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoDao
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoService
import no.nav.etterlatte.behandling.bosattutland.BosattUtlandDao
import no.nav.etterlatte.behandling.bosattutland.BosattUtlandService
import no.nav.etterlatte.behandling.doedshendelse.DoedshendelseReminderService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerDao
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerDataService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerTempService
import no.nav.etterlatte.behandling.etteroppgjoer.brev.EtteroppgjoerForbehandlingBrevService
import no.nav.etterlatte.behandling.etteroppgjoer.brev.EtteroppgjoerRevurderingBrevService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingDao
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerHendelseService
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentKlient
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentKlientImpl
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentService
import no.nav.etterlatte.behandling.etteroppgjoer.oppgave.EtteroppgjoerOppgaveService
import no.nav.etterlatte.behandling.etteroppgjoer.pensjonsgivendeinntekt.PensjonsgivendeInntektService
import no.nav.etterlatte.behandling.etteroppgjoer.revurdering.EtteroppgjoerRevurderingService
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlientImpl
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SkatteoppgjoerHendelserDao
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingDao
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.jobs.aktivitetsplikt.AktivitetspliktOppgaveUnntakUtloeperJob
import no.nav.etterlatte.behandling.jobs.aktivitetsplikt.AktivitetspliktOppgaveUnntakUtloeperJobService
import no.nav.etterlatte.behandling.jobs.doedsmelding.DoedsmeldingJob
import no.nav.etterlatte.behandling.jobs.doedsmelding.DoedsmeldingReminderJob
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.EtteroppgjoerSvarfristUtloeptJob
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.EtteroppgjoerSvarfristUtloeptJobService
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.OppdaterSkatteoppgjoerIkkeMottattJob
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.OppdaterSkatteoppgjoerIkkeMottattJobService
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.SkatteoppgjoerHendelserService
import no.nav.etterlatte.behandling.jobs.saksbehandler.SaksbehandlerJob
import no.nav.etterlatte.behandling.jobs.saksbehandler.SaksbehandlerJobService
import no.nav.etterlatte.behandling.jobs.sjekkadressebeskyttelse.SjekkAdressebeskyttelseJob
import no.nav.etterlatte.behandling.jobs.sjekkadressebeskyttelse.SjekkAdressebeskyttelseJobDao
import no.nav.etterlatte.behandling.jobs.sjekkadressebeskyttelse.SjekkAdressebeskyttelseJobService
import no.nav.etterlatte.behandling.klage.KlageBrevService
import no.nav.etterlatte.behandling.klage.KlageDaoImpl
import no.nav.etterlatte.behandling.klage.KlageHendelserServiceImpl
import no.nav.etterlatte.behandling.klage.KlageServiceImpl
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.BeregningKlientImpl
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.BrevApiKlientObo
import no.nav.etterlatte.behandling.klienter.EntraProxyKlient
import no.nav.etterlatte.behandling.klienter.EntraProxyKlientImpl
import no.nav.etterlatte.behandling.klienter.KlageKlientImpl
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlientImpl
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.behandling.klienter.Norg2KlientImpl
import no.nav.etterlatte.behandling.klienter.TilbakekrevingKlient
import no.nav.etterlatte.behandling.klienter.TilbakekrevingKlientImpl
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlientImpl
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlientImpl
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.behandling.omregning.MigreringService
import no.nav.etterlatte.behandling.omregning.OmregningDao
import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.behandling.revurdering.AutomatiskRevurderingService
import no.nav.etterlatte.behandling.revurdering.ManuellRevurderingService
import no.nav.etterlatte.behandling.revurdering.OmgjoeringKlageRevurderingService
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.behandling.selftest.SelfTestService
import no.nav.etterlatte.behandling.sjekkliste.SjekklisteDao
import no.nav.etterlatte.behandling.sjekkliste.SjekklisteService
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingDao
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingHendelserServiceImpl
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingService
import no.nav.etterlatte.behandling.vedtaksbehandling.BehandlingMedBrevDao
import no.nav.etterlatte.behandling.vedtaksbehandling.BehandlingMedBrevService
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.BrevKlientImpl
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.brev.TilbakekrevingBrevService
import no.nav.etterlatte.common.ConnectionAutoclosingImpl
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.PesysKlientImpl
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.common.klienter.SkjermingKlientImpl
import no.nav.etterlatte.config.JobbKeys.JOBB_DOEDSMELDINGER_REMINDER_OPENING_HOURS
import no.nav.etterlatte.config.JobbKeys.JOBB_METRIKKER_OPENING_HOURS
import no.nav.etterlatte.config.JobbKeys.JOBB_SAKSBEHANDLER_OPENING_HOURS
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagHenter
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.grunnlag.GrunnlagServiceImpl
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangDao
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringsHendelseFilter
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseJobService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelserKafkaServiceImpl
import no.nav.etterlatte.grunnlagsendring.doedshendelse.UkjentBeroertDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktService
import no.nav.etterlatte.inntektsjustering.AarligInntektsjusteringJobbService
import no.nav.etterlatte.inntektsjustering.selvbetjening.InntektsjusteringSelvbetjeningService
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdDao
import no.nav.etterlatte.jobs.MetrikkerJob
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.KafkaKey.KAFKA_RAPID_TOPIC
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.kafka.standardProducer
import no.nav.etterlatte.kodeverk.KodeverkKlient
import no.nav.etterlatte.kodeverk.KodeverkKlientImpl
import no.nav.etterlatte.kodeverk.KodeverkService
import no.nav.etterlatte.krr.KrrKlient
import no.nav.etterlatte.krr.KrrKlientImpl
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.OpeningHours
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.common.isProd
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.libs.ktor.AppConfig.ELECTOR_PATH
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.metrics.BehandlingMetrics
import no.nav.etterlatte.metrics.BehandlingMetrikkerDao
import no.nav.etterlatte.metrics.GjenopprettingMetrikkerDao
import no.nav.etterlatte.metrics.OppgaveMetrikkerDao
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporingImpl
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.oppgave.kommentar.OppgaveKommentarDaoImpl
import no.nav.etterlatte.oppgave.kommentar.OppgaveKommentarService
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveKlient
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveKlientImpl
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveServiceImpl
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakServiceImpl
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakTilgang
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.sak.SakTilgangImpl
import no.nav.etterlatte.sak.SakendringerDao
import no.nav.etterlatte.sak.TilgangServiceSjekkerImpl
import no.nav.etterlatte.saksbehandler.SaksbehandlerInfoDao
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import no.nav.etterlatte.saksbehandler.SaksbehandlerServiceImpl
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.OppdaterTilgangService
import no.nav.etterlatte.vilkaarsvurdering.dao.DelvilkaarDao
import no.nav.etterlatte.vilkaarsvurdering.dao.VilkaarsvurderingDao
import no.nav.etterlatte.vilkaarsvurdering.service.AldersovergangService
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import java.time.Duration
import java.time.temporal.ChronoUnit

private fun pdlHttpClient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("pdl.azure.scope"),
    )

private fun skjermingHttpClient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("skjerming.azure.scope"),
    )

private fun navAnsattHttpClient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("navansatt.azure.scope"),
    )

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
    )

private fun klageHttpClient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("klage.azure.scope"),
    )

private fun tilbakekrevingHttpClient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("tilbakekreving.azure.scope"),
    )

private fun krrHttKlient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("krr.scope"),
    )

private fun entraProxyKlient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("entraProxy.scope"),
    )

private fun sigrunKlient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("sigrun.scope"),
    )

private fun inntektskomponentKlient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("inntektskomponenten.scope"),
    )

private fun finnBrukerIdent(): String {
    val kontekst = Kontekst.get()
    return when (kontekst) {
        null -> Fagsaksystem.EY.navn
        else -> Kontekst.get().AppUser.name()
    }
}

internal class ApplicationContext(
    val env: Miljoevariabler = Miljoevariabler.systemEnv(),
    val config: Config = ConfigFactory.load(),
    val rapid: KafkaProdusent<String, String> =
        if (appIsInGCP()) {
            GcpKafkaConfig.fromEnv(env).standardProducer(env.requireEnvValue(KAFKA_RAPID_TOPIC))
        } else {
            TestProdusent()
        },
    val featureToggleService: FeatureToggleService =
        FeatureToggleService.initialiser(
            properties = featureToggleProperties(config),
            brukerIdent = { finnBrukerIdent() },
        ),
    val navAnsattKlient: NavAnsattKlient =
        NavAnsattKlientImpl(
            navAnsattHttpClient(config),
            env.requireEnvValue(NAVANSATT_URL),
        ).also {
            it.asyncPing()
        },
    val norg2Klient: Norg2Klient = Norg2KlientImpl(httpClient(), env.requireEnvValue(NORG2_URL)),
    val leaderElectionHttpClient: HttpClient = httpClient(),
    val beregningKlient: BeregningKlient = BeregningKlientImpl(config, httpClient()),
    val trygdetidKlient: TrygdetidKlient = TrygdetidKlientImpl(config, httpClient()),
    val gosysOppgaveKlient: GosysOppgaveKlient = GosysOppgaveKlientImpl(config, httpClient()),
    val vedtakKlient: VedtakKlient = VedtakKlientImpl(config, httpClient()),
    val brevApiKlient: BrevApiKlient = BrevApiKlientObo(config, httpClient(forventSuksess = true)),
    val brevKlient: BrevKlient = BrevKlientImpl(config, httpClient(forventSuksess = true)),
    val klageHttpClient: HttpClient = klageHttpClient(config),
    val tilbakekrevingKlient: TilbakekrevingKlient =
        TilbakekrevingKlientImpl(
            tilbakekrevingHttpClient(config),
            url = env.requireEnvValue(ETTERLATTE_TILBAKEKREVING_URL),
        ),
    val pesysKlient: PesysKlient = PesysKlientImpl(config, httpClient()),
    val krrKlient: KrrKlient = KrrKlientImpl(krrHttKlient(config), url = config.getString("krr.url")),
    val entraProxyKlient: EntraProxyKlient = EntraProxyKlientImpl(entraProxyKlient(config), url = config.getString("entraProxy.url")),
    val pdlTjenesterKlient: PdlTjenesterKlient = PdlTjenesterKlientImpl(config, pdlHttpClient(config)),
    val kodeverkKlient: KodeverkKlient = KodeverkKlientImpl(config, httpClient()),
    val skjermingKlient: SkjermingKlient =
        SkjermingKlientImpl(
            skjermingHttpClient(config),
            env.requireEnvValue(SKJERMING_URL),
        ),
    val inntektskomponentKlient: InntektskomponentKlient =
        InntektskomponentKlientImpl(
            inntektskomponentKlient(config),
            config.getString("inntektskomponenten.url"),
        ),
    val sigrunKlient: SigrunKlient =
        SigrunKlientImpl(
            sigrunKlient(config),
            config.getString("sigrun.url"),
            featureToggleService,
        ),
    val arbeidOgInntektKlient: ArbeidOgInntektKlient = ArbeidOgInntektKlient(httpClient(), config.getString("arbeidOgInntekt.url")),
    val brukerService: BrukerService = BrukerServiceImpl(pdlTjenesterKlient, norg2Klient),
    grunnlagServiceOverride: GrunnlagService? = null,
) {
    val httpPort = env.getOrDefault(HTTP_PORT, "8080").toInt()
    val saksbehandlerGroupIdsByKey = AzureGroup.entries.associateWith { env.requireEnvValue(it.envKey) }
    private val sporingslogg = Sporingslogg()
    val behandlingRequestLogger = BehandlingRequestLogger(sporingslogg)
    val dataSource = DataSourceBuilder.createDataSource(env)

    // Dao
    private val autoClosingDatabase = ConnectionAutoclosingImpl(dataSource)

    val hendelseDao = HendelseDao(autoClosingDatabase)
    val kommerBarnetTilGodeDao = KommerBarnetTilGodeDao(autoClosingDatabase)
    val aktivitetspliktDao = AktivitetspliktDao(autoClosingDatabase)
    val aktivitetspliktAktivitetsgradDao = AktivitetspliktAktivitetsgradDao(autoClosingDatabase)
    private val aktivitetspliktUnntakDao = AktivitetspliktUnntakDao(autoClosingDatabase)
    private val sjekklisteDao = SjekklisteDao(autoClosingDatabase)
    val revurderingDao = RevurderingDao(autoClosingDatabase)
    val behandlingDao = BehandlingDao(kommerBarnetTilGodeDao, revurderingDao, autoClosingDatabase)
    private val generellbehandlingDao = GenerellBehandlingDao(autoClosingDatabase)
    private val behandlingMedBrevDao = BehandlingMedBrevDao(autoClosingDatabase)
    private val oppgaveDaoNy = OppgaveDaoImpl(autoClosingDatabase)
    private val oppgaveDaoEndringer = OppgaveDaoMedEndringssporingImpl(oppgaveDaoNy, autoClosingDatabase)
    private val oppgaveKommentarDao = OppgaveKommentarDaoImpl(autoClosingDatabase)
    val sakLesDao = SakLesDao(autoClosingDatabase)
    private val sakendringerDao = SakendringerDao(autoClosingDatabase)
    val sakSkrivDao = SakSkrivDao(sakendringerDao)
    val grunnlagsendringshendelseDao =
        GrunnlagsendringshendelseDao(
            autoClosingDatabase,
        )
    val institusjonsoppholdDao = InstitusjonsoppholdDao(autoClosingDatabase)
    private val oppgaveMetrikkerDao = OppgaveMetrikkerDao(dataSource)
    private val behandlingMetrikkerDao = BehandlingMetrikkerDao(dataSource)
    private val gjenopprettingMetrikkerDao = GjenopprettingMetrikkerDao(dataSource)
    val klageDao = KlageDaoImpl(autoClosingDatabase)
    val tilbakekrevingDao = TilbakekrevingDao(autoClosingDatabase)
    private val etteroppgjoerForbehandlingDao = EtteroppgjoerForbehandlingDao(autoClosingDatabase)
    private val skatteoppgjoerHendelserDao = SkatteoppgjoerHendelserDao(autoClosingDatabase)
    private val etteroppgjoerDao = EtteroppgjoerDao(autoClosingDatabase)
    val behandlingInfoDao = BehandlingInfoDao(autoClosingDatabase)
    private val bosattUtlandDao = BosattUtlandDao(autoClosingDatabase)
    private val saksbehandlerInfoDao = SaksbehandlerInfoDao(autoClosingDatabase)
    private val aktivitetspliktBrevDao = AktivitetspliktBrevDao(autoClosingDatabase)
    private val doedshendelseDao = DoedshendelseDao(autoClosingDatabase)
    private val omregningDao = OmregningDao(autoClosingDatabase)
    val sakTilgangDao = SakTilgangDao(dataSource)
    private val vilkaarsvurderingDao = VilkaarsvurderingDao(autoClosingDatabase, DelvilkaarDao())
    private val ukjentBeroertDao = UkjentBeroertDao(autoClosingDatabase)

    // Klient
    private val leaderElectionKlient = LeaderElection(env[ELECTOR_PATH], leaderElectionHttpClient)

    private val klageKlient = KlageKlientImpl(klageHttpClient, url = env.requireEnvValue(ETTERLATTE_KLAGE_API_URL))
    private val deodshendelserProducer = DoedshendelserKafkaServiceImpl(rapid)
    val kodeverkService = KodeverkService(kodeverkKlient)

    val behandlingsHendelser = BehandlingsHendelserKafkaProducerImpl(rapid)

    // Service
    private val klageHendelser = KlageHendelserServiceImpl(rapid)
    private val tilbakekrevingHendelserService = TilbakekrevingHendelserServiceImpl(rapid)
    val saksbehandlerService: SaksbehandlerService =
        SaksbehandlerServiceImpl(saksbehandlerInfoDao, navAnsattKlient, entraProxyKlient)
    val oppgaveService =
        OppgaveService(oppgaveDaoEndringer, sakLesDao, hendelseDao, behandlingsHendelser, saksbehandlerService)
    val oppgaveKommentarService = OppgaveKommentarService(oppgaveKommentarDao, oppgaveService, sakLesDao)

    private val aldersovergangDao = AldersovergangDao(dataSource)

    val opplysningDao = OpplysningDao(dataSource)

    val nyAldersovergangService =
        no.nav.etterlatte.grunnlag.aldersovergang
            .AldersovergangService(aldersovergangDao)

    val sakTilgang: SakTilgang = SakTilgangImpl(sakSkrivDao, sakLesDao)
    val oppdaterTilgangService =
        OppdaterTilgangService(
            skjermingKlient = skjermingKlient,
            pdltjenesterKlient = pdlTjenesterKlient,
            brukerService = brukerService,
            oppgaveService = oppgaveService,
            sakSkrivDao = sakSkrivDao,
            sakTilgang = sakTilgang,
            sakLesDao = sakLesDao,
            featureToggleService = featureToggleService,
        )

    val grunnlagService: GrunnlagService =
        grunnlagServiceOverride ?: GrunnlagServiceImpl(
            pdlTjenesterKlient,
            opplysningDao,
            GrunnlagHenter(pdlTjenesterKlient),
            oppdaterTilgangService,
        )

    private val etteroppgjoerHendelseService = EtteroppgjoerHendelseService(rapid, hendelseDao, etteroppgjoerForbehandlingDao)
    private val etteroppgjoerOppgaveService = EtteroppgjoerOppgaveService(oppgaveService)

    val etteroppgjoerTempService =
        EtteroppgjoerTempService(etteroppgjoerDao, etteroppgjoerForbehandlingDao, etteroppgjoerHendelseService)

    val behandlingService =
        BehandlingServiceImpl(
            behandlingDao = behandlingDao,
            behandlingHendelser = behandlingsHendelser,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
            hendelseDao = hendelseDao,
            kommerBarnetTilGodeDao = kommerBarnetTilGodeDao,
            oppgaveService = oppgaveService,
            grunnlagService = grunnlagService,
            beregningKlient = beregningKlient,
            etteroppgjoerTempService = etteroppgjoerTempService,
            etteroppgjoerOppgaveService = etteroppgjoerOppgaveService,
        )
    val generellBehandlingService =
        GenerellBehandlingService(
            generellbehandlingDao,
            oppgaveService,
            behandlingService,
            grunnlagService,
            hendelseDao,
            saksbehandlerInfoDao,
        )
    val behandlingMedBrevService =
        BehandlingMedBrevService(
            behandlingMedBrevDao,
        )
    val kommerBarnetTilGodeService =
        KommerBarnetTilGodeService(kommerBarnetTilGodeDao, behandlingDao)
    val sjekklisteService = SjekklisteService(sjekklisteDao, behandlingService, oppgaveService)

    private val klageBrevService = KlageBrevService(brevApiKlient)
    val klageService =
        KlageServiceImpl(
            klageDao = klageDao,
            sakDao = sakLesDao,
            behandlingService = behandlingService,
            hendelseDao = hendelseDao,
            oppgaveService = oppgaveService,
            klageKlient = klageKlient,
            klageHendelser = klageHendelser,
            vedtakKlient = vedtakKlient,
            featureToggleService = featureToggleService,
            klageBrevService = klageBrevService,
        )

    val aktivitetspliktKopierService =
        AktivitetspliktKopierService(aktivitetspliktAktivitetsgradDao, aktivitetspliktUnntakDao)

    val revurderingService =
        RevurderingService(
            oppgaveService = oppgaveService,
            grunnlagService = grunnlagService,
            behandlingHendelser = behandlingsHendelser,
            behandlingDao = behandlingDao,
            hendelseDao = hendelseDao,
            kommerBarnetTilGodeService = kommerBarnetTilGodeService,
            revurderingDao = revurderingDao,
            aktivitetspliktDao = aktivitetspliktDao,
            aktivitetspliktKopierService = aktivitetspliktKopierService,
        )
    val automatiskRevurderingService =
        AutomatiskRevurderingService(
            revurderingService,
            behandlingService,
            grunnlagService,
            vedtakKlient,
            beregningKlient,
        )
    val manuellRevurderingService =
        ManuellRevurderingService(
            revurderingService = revurderingService,
            behandlingService = behandlingService,
            grunnlagService = grunnlagService,
            oppgaveService = oppgaveService,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
        )
    val omgjoeringKlageRevurderingService =
        OmgjoeringKlageRevurderingService(
            revurderingService = revurderingService,
            oppgaveService = oppgaveService,
            klageService = klageService,
            behandlingDao = behandlingDao,
            grunnlagService = grunnlagService,
        )

    val aktivitetspliktService =
        AktivitetspliktService(
            aktivitetspliktDao = aktivitetspliktDao,
            aktivitetspliktAktivitetsgradDao = aktivitetspliktAktivitetsgradDao,
            aktivitetspliktUnntakDao = aktivitetspliktUnntakDao,
            behandlingService = behandlingService,
            grunnlagService = grunnlagService,
            revurderingService = revurderingService,
            statistikkKafkaProducer = behandlingsHendelser,
            oppgaveService = oppgaveService,
            aktivitetspliktKopierService = aktivitetspliktKopierService,
            featureToggleService = featureToggleService,
        )
    val gyldighetsproevingService =
        GyldighetsproevingServiceImpl(
            behandlingDao = behandlingDao,
        )

    val omregningService =
        OmregningService(
            behandlingService = behandlingService,
            omregningDao = omregningDao,
            oppgaveService = oppgaveService,
        )

    val tilgangService = TilgangServiceSjekkerImpl(sakTilgangDao)

    private val externalServices: List<Pingable> =
        listOf(
            entraProxyKlient,
            navAnsattKlient,
            skjermingKlient,
            pdlTjenesterKlient,
            klageKlient,
            tilbakekrevingKlient,
        )
    val selfTestService = SelfTestService(externalServices)

    val sakService =
        SakServiceImpl(
            sakSkrivDao,
            sakLesDao,
            sakendringerDao,
            skjermingKlient,
            brukerService,
            grunnlagService,
            krrKlient,
            pdlTjenesterKlient,
            featureToggleService,
            oppdaterTilgangService,
            sakTilgang,
            nyAldersovergangService,
        )

    val etteroppgjoerService =
        EtteroppgjoerService(
            dao = etteroppgjoerDao,
            vedtakKlient = vedtakKlient,
            behandlingService = behandlingService,
            beregningKlient = beregningKlient,
            sigrunKlient = sigrunKlient,
            etteroppgjoerOppgaveService = etteroppgjoerOppgaveService,
        )

    val etteroppgjoerDataService = EtteroppgjoerDataService(behandlingService, featureToggleService, vedtakKlient, beregningKlient)

    val doedshendelseService =
        DoedshendelseService(doedshendelseDao, pdlTjenesterKlient, gosysOppgaveKlient, ukjentBeroertDao)

    val inntektsjusteringSelvbetjeningService =
        InntektsjusteringSelvbetjeningService(
            oppgaveService = oppgaveService,
            behandlingService = behandlingService,
            vedtakKlient = vedtakKlient,
            rapid = rapid,
            featureToggleService = featureToggleService,
            beregningKlient = beregningKlient,
        )

    private val inntektskomponentService =
        InntektskomponentService(
            klient = inntektskomponentKlient,
            featureToggleService = featureToggleService,
        )

    private val pensjonsgivendeInntektService: PensjonsgivendeInntektService =
        PensjonsgivendeInntektService(
            sigrunKlient = sigrunKlient,
        )

    val etteroppgjoerForbehandlingService =
        EtteroppgjoerForbehandlingService(
            dao = etteroppgjoerForbehandlingDao,
            etteroppgjoerService = etteroppgjoerService,
            sakDao = sakLesDao,
            oppgaveService = oppgaveService,
            inntektskomponentService = inntektskomponentService,
            pensjonsgivendeInntektService = pensjonsgivendeInntektService,
            hendelserService = etteroppgjoerHendelseService,
            beregningKlient = beregningKlient,
            behandlingService = behandlingService,
            vedtakKlient = vedtakKlient,
            etteroppgjoerOppgaveService = etteroppgjoerOppgaveService,
            etteroppgjoerDataService = etteroppgjoerDataService,
        )

    val aarligInntektsjusteringJobbService =
        AarligInntektsjusteringJobbService(
            omregningService = omregningService,
            sakService = sakService,
            behandlingService = behandlingService,
            revurderingService = revurderingService,
            vedtakKlient = vedtakKlient,
            grunnlagService = grunnlagService,
            beregningKlient = beregningKlient,
            pdlTjenesterKlient = pdlTjenesterKlient,
            oppgaveService = oppgaveService,
            rapid = rapid,
            featureToggleService = featureToggleService,
            aldersovergangService = nyAldersovergangService,
            etteroppgjoerForbehandlingService = etteroppgjoerForbehandlingService,
        )

    private val grunnlagsendringsHendelseFilter = GrunnlagsendringsHendelseFilter(vedtakKlient, behandlingService)
    val grunnlagsendringshendelseService =
        GrunnlagsendringshendelseService(
            oppgaveService = oppgaveService,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
            behandlingService = behandlingService,
            pdltjenesterKlient = pdlTjenesterKlient,
            grunnlagService = grunnlagService,
            sakService = sakService,
            doedshendelseService = doedshendelseService,
            grunnlagsendringsHendelseFilter = grunnlagsendringsHendelseFilter,
            tilgangsService = oppdaterTilgangService,
        )

    private val doedshendelseReminderJob =
        DoedshendelseReminderService(doedshendelseDao, behandlingService, oppgaveService, sakLesDao)
    private val doedshendelseJobService =
        DoedshendelseJobService(
            doedshendelseDao = doedshendelseDao,
            doedshendelseKontrollpunktService =
                DoedshendelseKontrollpunktService(
                    pdlTjenesterKlient = pdlTjenesterKlient,
                    grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
                    oppgaveService = oppgaveService,
                    sakService = sakService,
                    pesysKlient = pesysKlient,
                    behandlingService = behandlingService,
                ),
            grunnlagsendringshendelseService = grunnlagsendringshendelseService,
            sakService = sakService,
            dagerGamleHendelserSomSkalKjoeres = if (isProd()) 5 else 0,
            deodshendelserProducer = deodshendelserProducer,
            grunnlagService = grunnlagService,
            pdlTjenesterKlient = pdlTjenesterKlient,
            krrKlient = krrKlient,
        )

    val behandlingsStatusService =
        BehandlingStatusServiceImpl(
            behandlingDao,
            behandlingService,
            behandlingInfoDao,
            oppgaveService,
            grunnlagsendringshendelseService,
            generellBehandlingService,
            aktivitetspliktService,
            saksbehandlerService,
            etteroppgjoerService,
            etteroppgjoerForbehandlingService,
            grunnlagService,
        )

    val behandlingInfoService = BehandlingInfoService(behandlingInfoDao, behandlingService, behandlingsStatusService)

    val bosattUtlandService = BosattUtlandService(bosattUtlandDao = bosattUtlandDao)

    val tilbakekrevingBrevService =
        TilbakekrevingBrevService(
            sakService,
            brevKlient,
            brevApiKlient,
            vedtakKlient,
            grunnlagService,
        )

    val skatteoppgjoerHendelserService =
        SkatteoppgjoerHendelserService(
            dao = skatteoppgjoerHendelserDao,
            sigrunKlient = sigrunKlient,
            etteroppgjoerService = etteroppgjoerService,
            sakService = sakService,
            vedtakKlient = vedtakKlient,
        )

    private val etteroppgjoerRevurderingBrevService =
        EtteroppgjoerRevurderingBrevService(
            grunnlagService = grunnlagService,
            vedtakKlient = vedtakKlient,
            brevKlient = brevKlient,
            behandlingService = behandlingService,
            etteroppgjoerForbehandlingService = etteroppgjoerForbehandlingService,
            beregningKlient = beregningKlient,
            brevApiKlient = brevApiKlient,
            etteroppgjoerService = etteroppgjoerService,
        )

    private val uttrekkLoependeYtelseEtter67JobService =
        UttrekkLoependeYtelseEtter67JobService(
            vedtakKlient,
            sakService,
            nyAldersovergangService,
            featureToggleService,
        )

    val etteroppgjoerForbehandlingBrevService =
        EtteroppgjoerForbehandlingBrevService(
            brevKlient = brevKlient,
            grunnlagService = grunnlagService,
            etteroppgjoerForbehandlingService = etteroppgjoerForbehandlingService,
            behandlingService = behandlingService,
            etteroppgjoerOppgaveService = etteroppgjoerOppgaveService,
        )

    val vilkaarsvurderingService =
        VilkaarsvurderingService(
            vilkaarsvurderingDao,
            behandlingService,
            grunnlagService,
            behandlingsStatusService,
        )

    private val vedtaksbrevService =
        VedtaksbrevService(
            grunnlagService = grunnlagService,
            vedtakKlient = vedtakKlient,
            brevKlient = brevKlient,
            behandlingService = behandlingService,
            beregningKlient = beregningKlient,
            behandlingInfoService = behandlingInfoService,
            trygdetidKlient = trygdetidKlient,
            vilkaarsvurderingService = vilkaarsvurderingService,
            sakService = sakService,
            klageService = klageService,
            kodeverkService = kodeverkService,
        )

    val brevService =
        BrevService(
            behandlingMedBrevService = behandlingMedBrevService,
            behandlingService = behandlingService,
            brevApiKlient = brevApiKlient,
            vedtakKlient = vedtakKlient,
            tilbakekrevingBrevService = tilbakekrevingBrevService,
            etteroppgjoerForbehandlingBrevService = etteroppgjoerForbehandlingBrevService,
            etteroppgjoerRevurderingBrevService = etteroppgjoerRevurderingBrevService,
            vedtaksbrevService = vedtaksbrevService,
        )

    val tilbakekrevingService =
        TilbakekrevingService(
            tilbakekrevingDao = tilbakekrevingDao,
            sakDao = sakLesDao,
            hendelseDao = hendelseDao,
            behandlingService = behandlingService,
            oppgaveService = oppgaveService,
            vedtakKlient = vedtakKlient,
            brevApiKlient = brevApiKlient,
            brevService = brevService,
            tilbakekrevingKlient = tilbakekrevingKlient,
            tilbakekrevinghendelser = tilbakekrevingHendelserService,
        )

    private val saksbehandlerJobService =
        SaksbehandlerJobService(saksbehandlerInfoDao, navAnsattKlient, entraProxyKlient)

    val oppdaterSkatteoppgjoerIkkeMottattJobService =
        OppdaterSkatteoppgjoerIkkeMottattJobService(
            featureToggleService,
            etteroppgjoerOppgaveService,
            etteroppgjoerService,
        )

    val etteroppgjoerSvarfristUtloeptJobService =
        EtteroppgjoerSvarfristUtloeptJobService(
            etteroppgjoerService,
            oppgaveService,
            featureToggleService,
        )

    private val aktivitetspliktOppgaveUnntakUtloeperJobService =
        AktivitetspliktOppgaveUnntakUtloeperJobService(
            aktivitetspliktDao,
            aktivitetspliktService,
            oppgaveService,
            vedtakKlient,
            featureToggleService,
        )

    val gosysOppgaveService =
        GosysOppgaveServiceImpl(
            gosysOppgaveKlient,
            oppgaveService,
            saksbehandlerService,
            saksbehandlerInfoDao,
            pdlTjenesterKlient,
        )
    val aldersovergangService = AldersovergangService(vilkaarsvurderingService)

    val behandlingFactory =
        BehandlingFactory(
            oppgaveService = oppgaveService,
            grunnlagService = grunnlagService,
            revurderingService = revurderingService,
            gyldighetsproevingService = gyldighetsproevingService,
            sakService = sakService,
            behandlingDao = behandlingDao,
            hendelseDao = hendelseDao,
            behandlingHendelser = behandlingsHendelser,
            kommerBarnetTilGodeService = kommerBarnetTilGodeService,
            vilkaarsvurderingService = vilkaarsvurderingService,
            behandlingInfoService = behandlingInfoService,
            tilgangsService = oppdaterTilgangService,
        )

    val etteroppgjoerRevurderingService =
        EtteroppgjoerRevurderingService(
            behandlingService,
            etteroppgjoerService,
            etteroppgjoerForbehandlingService,
            grunnlagService,
            revurderingService,
            vilkaarsvurderingService,
            trygdetidKlient,
            beregningKlient,
            etteroppgjoerDataService,
        )

    val migreringService =
        MigreringService(
            behandlingService = behandlingService,
        )

    val aktivitetspliktOppgaveService =
        AktivitetspliktOppgaveService(
            aktivitetspliktService = aktivitetspliktService,
            oppgaveService = oppgaveService,
            sakService = sakService,
            aktivitetspliktBrevDao = aktivitetspliktBrevDao,
            brevApiKlient = brevApiKlient,
            behandlingService = behandlingService,
            beregningKlient = beregningKlient,
        )

    // Jobs
    val metrikkerJob: MetrikkerJob by lazy {
        MetrikkerJob(
            BehandlingMetrics(oppgaveMetrikkerDao, behandlingMetrikkerDao, gjenopprettingMetrikkerDao),
            { leaderElectionKlient.isLeader() },
            Duration.of(6, ChronoUnit.MINUTES).toMillis(),
            periode = Duration.of(10, ChronoUnit.MINUTES),
            openingHours = env.requireEnvValue(JOBB_METRIKKER_OPENING_HOURS).let { OpeningHours.of(it) },
        )
    }

    val uttrekkLoependeYtelseEtter67Job: UttrekkLoependeYtelseEtter67Job by lazy {
        UttrekkLoependeYtelseEtter67Job(
            service = uttrekkLoependeYtelseEtter67JobService,
            dataSource = dataSource,
            sakTilgangDao = sakTilgangDao,
            erLeader = { leaderElectionKlient.isLeader() },
            initialDelay = Duration.of(8, ChronoUnit.MINUTES).toMillis(),
            interval = Duration.of(1, ChronoUnit.HOURS),
        )
    }

    val aktivitetspliktOppgaveUnntakUtloeperJob: AktivitetspliktOppgaveUnntakUtloeperJob by lazy {
        AktivitetspliktOppgaveUnntakUtloeperJob(
            aktivitetspliktOppgaveUnntakUtloeperJobService,
            { leaderElectionKlient.isLeader() },
            initialDelay = Duration.of(5, ChronoUnit.MINUTES).toMillis(),
            interval = Duration.of(1, ChronoUnit.HOURS),
        )
    }

    val doedsmeldingerJob: DoedsmeldingJob by lazy {
        DoedsmeldingJob(
            doedshendelseJobService,
            { leaderElectionKlient.isLeader() },
            if (isProd()) {
                Duration.of(3, ChronoUnit.MINUTES).toMillis()
            } else {
                Duration
                    .of(20, ChronoUnit.MINUTES)
                    .toMillis()
            },
            interval = if (isProd()) Duration.of(1, ChronoUnit.HOURS) else Duration.of(10, ChronoUnit.HOURS),
            dataSource = dataSource,
            sakTilgangDao = sakTilgangDao,
        )
    }

    val doedsmeldingerReminderJob: DoedsmeldingReminderJob by lazy {
        DoedsmeldingReminderJob(
            doedshendelseReminderJob,
            { leaderElectionKlient.isLeader() },
            Duration.of(4, ChronoUnit.MINUTES).toMillis(),
            interval = if (isProd()) Duration.of(1, ChronoUnit.DAYS) else Duration.of(1, ChronoUnit.HOURS),
            dataSource = dataSource,
            sakTilgangDao = sakTilgangDao,
            openingHours = env.requireEnvValue(JOBB_DOEDSMELDINGER_REMINDER_OPENING_HOURS).let { OpeningHours.of(it) },
        )
    }

    val saksbehandlerJob: SaksbehandlerJob by lazy {
        SaksbehandlerJob(
            saksbehandlerJobService = saksbehandlerJobService,
            { leaderElectionKlient.isLeader() },
            initialDelay = Duration.of(2, ChronoUnit.MINUTES).toMillis(),
            interval = Duration.of(20, ChronoUnit.MINUTES),
            openingHours = env.requireEnvValue(JOBB_SAKSBEHANDLER_OPENING_HOURS).let { OpeningHours.of(it) },
        )
    }

    val oppdaterSkatteoppgjoerIkkeMottattJob: OppdaterSkatteoppgjoerIkkeMottattJob by lazy {
        OppdaterSkatteoppgjoerIkkeMottattJob(
            oppdaterSkatteoppgjoerIkkeMottattJobService = oppdaterSkatteoppgjoerIkkeMottattJobService,
            { leaderElectionKlient.isLeader() },
            initialDelay = Duration.of(1, ChronoUnit.MINUTES).toMillis(),
            interval = Duration.of(5, ChronoUnit.MINUTES),
            dataSource = dataSource,
            sakTilgangDao = sakTilgangDao,
        )
    }

    val etteroppgjoerSvarfristUtloeptJob: EtteroppgjoerSvarfristUtloeptJob by lazy {
        EtteroppgjoerSvarfristUtloeptJob(
            etteroppgjoerSvarfristUtloeptJobService,
            { leaderElectionKlient.isLeader() },
            initialDelay = Duration.of(5, ChronoUnit.MINUTES).toMillis(),
            interval = if (isProd()) Duration.of(1, ChronoUnit.DAYS) else Duration.of(6, ChronoUnit.MINUTES),
            dataSource = dataSource,
            sakTilgangDao = sakTilgangDao,
        )
    }

    val sjekkAdressebeskyttelseJobDao: SjekkAdressebeskyttelseJobDao by lazy {
        SjekkAdressebeskyttelseJobDao(autoClosingDatabase)
    }

    val sjekkAdressebeskyttelseJobService: SjekkAdressebeskyttelseJobService by lazy {
        SjekkAdressebeskyttelseJobService(
            sjekkAdressebeskyttelseJobDao = sjekkAdressebeskyttelseJobDao,
            pdlTjenesterKlient = pdlTjenesterKlient,
            tilgangService = oppdaterTilgangService,
            grunnlagService = grunnlagService,
            featureToggleService = featureToggleService,
            sakLesDao = sakLesDao,
        )
    }

    val sjekkAdressebeskyttelseJob: SjekkAdressebeskyttelseJob by lazy {
        SjekkAdressebeskyttelseJob(
            service = sjekkAdressebeskyttelseJobService,
            sakTilgangDao = sakTilgangDao,
            dataSource = dataSource,
            erLeader = { leaderElectionKlient.isLeader() },
            initialDelay = Duration.of(5, ChronoUnit.MINUTES).toMillis(),
            interval = Duration.of(5, ChronoUnit.MINUTES),
        )
    }

    val lesSkatteoppgjoerHendelserJob: LesSkatteoppgjoerHendelserJob by lazy {
        LesSkatteoppgjoerHendelserJob(
            skatteoppgjoerHendelserService = skatteoppgjoerHendelserService,
            erLeader = { leaderElectionKlient.isLeader() },
            initialDelay = Duration.of(3, ChronoUnit.MINUTES).toMillis(),
            interval = Duration.of(1, ChronoUnit.HOURS),
            hendelserBatchSize = 1000,
            dataSource = dataSource,
            sakTilgangDao = sakTilgangDao,
            featureToggleService = featureToggleService,
        )
    }

    fun close() {
        (dataSource as HikariDataSource).close()
    }
}

enum class JobbKeys : EnvEnum {
    JOBB_DOEDSMELDINGER_REMINDER_OPENING_HOURS,
    JOBB_METRIKKER_OPENING_HOURS,
    JOBB_SAKSBEHANDLER_OPENING_HOURS,
    ;

    override fun key() = name
}
