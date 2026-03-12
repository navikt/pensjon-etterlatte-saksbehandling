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
import no.nav.etterlatte.behandling.etteroppgjoer.brev.EtteroppgjoerForbehandlingBrevService
import no.nav.etterlatte.behandling.etteroppgjoer.brev.EtteroppgjoerRevurderingBrevService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingDao
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingHendelseService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
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
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.LesSkatteoppgjoerHendelserJobService
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.OppdaterSkatteoppgjoerIkkeMottattJob
import no.nav.etterlatte.behandling.jobs.etteroppgjoer.OppdaterSkatteoppgjoerIkkeMottattJobService
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
import no.nav.etterlatte.behandling.klienter.VedtakInternalService
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
import no.nav.etterlatte.behandling.vedtaksvurdering.VedtaksvurderingRepository
import no.nav.etterlatte.behandling.vedtaksvurdering.klienter.SamordningsKlient
import no.nav.etterlatte.behandling.vedtaksvurdering.klienter.SamordningsKlientImpl
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakBehandlingService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakEtteroppgjoerService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakKlageService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakSamordningService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakTilbakekrevingService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtaksvurderingRapidService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtaksvurderingService
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
    val kontekst by lazy { Kontekst.get() }
    return when (kontekst) {
        null -> Fagsaksystem.EY.navn
        else -> Kontekst.get().AppUser.name()
    }
}

private fun samKlient(config: Config) =
    httpClientClientCredentials(
        azureAppClientId = config.getString("azure.app.client.id"),
        azureAppJwk = config.getString("azure.app.jwk"),
        azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
        azureAppScope = config.getString("samordnevedtak.azure.scope"),
    )

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
    // TODO: Denne trenger egentlig ikke å hete "default" men vi fikser senere.
    val vedtakKlientDefault: VedtakKlient = VedtakKlientImpl(config, httpClient()),
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
    val entraProxyKlient: EntraProxyKlient =
        EntraProxyKlientImpl(
            entraProxyKlient(config),
            url = config.getString("entraProxy.url"),
        ),
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
    val arbeidOgInntektKlient: ArbeidOgInntektKlient =
        ArbeidOgInntektKlient(
            httpClient(),
            config.getString("arbeidOgInntekt.url"),
        ),
    val brukerService: BrukerService = BrukerServiceImpl(pdlTjenesterKlient, norg2Klient),
    val samordningKlient: SamordningsKlient = SamordningsKlientImpl(config, samKlient(config)),
    grunnlagServiceOverride: GrunnlagService? = null,
) {
    // TODO: Dette skal komme fra yaml, wip.
    private val brukNyVedtakKlientInternal: Boolean = false

    val httpPort by lazy { env.getOrDefault(HTTP_PORT, "8080").toInt() }
    val saksbehandlerGroupIdsByKey = AzureGroup.entries.associateWith { env.requireEnvValue(it.envKey) }
    private val sporingslogg by lazy { Sporingslogg() }
    val behandlingRequestLogger by lazy { BehandlingRequestLogger(sporingslogg) }
    val dataSource by lazy { DataSourceBuilder.createDataSource(env) }

    private val autoClosingDatabase by lazy { ConnectionAutoclosingImpl(dataSource) }

    private val vedtaksvurderingRepository by lazy { VedtaksvurderingRepository(dataSource) }

    val hendelseDao by lazy { HendelseDao(autoClosingDatabase) }
    val kommerBarnetTilGodeDao by lazy { KommerBarnetTilGodeDao(autoClosingDatabase) }
    val aktivitetspliktDao by lazy { AktivitetspliktDao(autoClosingDatabase) }
    val aktivitetspliktAktivitetsgradDao by lazy { AktivitetspliktAktivitetsgradDao(autoClosingDatabase) }
    private val aktivitetspliktUnntakDao by lazy { AktivitetspliktUnntakDao(autoClosingDatabase) }
    private val sjekklisteDao by lazy { SjekklisteDao(autoClosingDatabase) }
    val revurderingDao by lazy { RevurderingDao(autoClosingDatabase) }
    val behandlingDao by lazy { BehandlingDao(kommerBarnetTilGodeDao, revurderingDao, autoClosingDatabase) }
    private val generellbehandlingDao by lazy { GenerellBehandlingDao(autoClosingDatabase) }
    private val behandlingMedBrevDao by lazy { BehandlingMedBrevDao(autoClosingDatabase) }
    private val oppgaveDaoNy by lazy { OppgaveDaoImpl(autoClosingDatabase) }
    private val oppgaveDaoEndringer by lazy { OppgaveDaoMedEndringssporingImpl(oppgaveDaoNy, autoClosingDatabase) }
    private val oppgaveKommentarDao by lazy { OppgaveKommentarDaoImpl(autoClosingDatabase) }
    val sakLesDao by lazy { SakLesDao(autoClosingDatabase) }
    private val sakendringerDao by lazy { SakendringerDao(autoClosingDatabase) }
    val sakSkrivDao by lazy { SakSkrivDao(sakendringerDao) }
    val grunnlagsendringshendelseDao by lazy {
        GrunnlagsendringshendelseDao(
            autoClosingDatabase,
        )
    }
    val institusjonsoppholdDao by lazy { InstitusjonsoppholdDao(autoClosingDatabase) }
    private val oppgaveMetrikkerDao by lazy { OppgaveMetrikkerDao(dataSource) }
    private val behandlingMetrikkerDao by lazy { BehandlingMetrikkerDao(dataSource) }
    private val gjenopprettingMetrikkerDao by lazy { GjenopprettingMetrikkerDao(dataSource) }
    val klageDao by lazy { KlageDaoImpl(autoClosingDatabase) }
    val tilbakekrevingDao by lazy { TilbakekrevingDao(autoClosingDatabase) }
    private val etteroppgjoerForbehandlingDao by lazy { EtteroppgjoerForbehandlingDao(autoClosingDatabase) }
    private val skatteoppgjoerHendelserDao by lazy { SkatteoppgjoerHendelserDao(autoClosingDatabase) }
    private val etteroppgjoerDao by lazy { EtteroppgjoerDao(autoClosingDatabase) }
    val behandlingInfoDao by lazy { BehandlingInfoDao(autoClosingDatabase) }
    private val bosattUtlandDao by lazy { BosattUtlandDao(autoClosingDatabase) }
    private val saksbehandlerInfoDao by lazy { SaksbehandlerInfoDao(autoClosingDatabase) }
    private val aktivitetspliktBrevDao by lazy { AktivitetspliktBrevDao(autoClosingDatabase) }
    private val doedshendelseDao by lazy { DoedshendelseDao(autoClosingDatabase) }
    private val omregningDao by lazy { OmregningDao(autoClosingDatabase) }
    val sakTilgangDao by lazy { SakTilgangDao(dataSource) }
    private val vilkaarsvurderingDao by lazy { VilkaarsvurderingDao(autoClosingDatabase, DelvilkaarDao()) }
    private val ukjentBeroertDao by lazy { UkjentBeroertDao(autoClosingDatabase) }

    private val leaderElectionKlient by lazy { LeaderElection(env[ELECTOR_PATH], leaderElectionHttpClient) }

    private val klageKlient by lazy {
        KlageKlientImpl(
            klageHttpClient,
            url = env.requireEnvValue(ETTERLATTE_KLAGE_API_URL),
        )
    }
    private val deodshendelserProducer by lazy { DoedshendelserKafkaServiceImpl(rapid) }
    val kodeverkService by lazy { KodeverkService(kodeverkKlient) }
    val behandlingsHendelser by lazy { BehandlingsHendelserKafkaProducerImpl(rapid) }

    private val klageHendelser by lazy { KlageHendelserServiceImpl(rapid) }
    private val tilbakekrevingHendelserService by lazy { TilbakekrevingHendelserServiceImpl(rapid) }
    val saksbehandlerService: SaksbehandlerService by lazy {
        SaksbehandlerServiceImpl(
            saksbehandlerInfoDao,
            navAnsattKlient,
            entraProxyKlient,
        )
    }
    val oppgaveService by lazy {
        OppgaveService(
            oppgaveDaoEndringer,
            sakLesDao,
            hendelseDao,
            behandlingsHendelser,
            saksbehandlerService,
        )
    }
    val oppgaveKommentarService by lazy { OppgaveKommentarService(oppgaveKommentarDao, oppgaveService, sakLesDao) }

    private val aldersovergangDao by lazy { AldersovergangDao(dataSource) }

    val opplysningDao by lazy { OpplysningDao(dataSource) }

    val nyAldersovergangService by lazy {
        no.nav.etterlatte.grunnlag.aldersovergang
            .AldersovergangService(aldersovergangDao)
    }

    val sakTilgang: SakTilgang by lazy { SakTilgangImpl(sakSkrivDao, sakLesDao) }
    val oppdaterTilgangService by lazy {
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
    }

    val grunnlagService: GrunnlagService by lazy {
        grunnlagServiceOverride ?: GrunnlagServiceImpl(
            pdlTjenesterKlient,
            opplysningDao,
            GrunnlagHenter(pdlTjenesterKlient),
            oppdaterTilgangService,
        )
    }

    private val etteroppgjoerHendelseService by lazy {
        EtteroppgjoerForbehandlingHendelseService(
            rapid,
            hendelseDao,
            etteroppgjoerForbehandlingDao,
        )
    }
    private val etteroppgjoerOppgaveService by lazy { EtteroppgjoerOppgaveService(oppgaveService) }

    val behandlingService by lazy {
        BehandlingServiceImpl(
            behandlingDao = behandlingDao,
            behandlingHendelser = behandlingsHendelser,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
            hendelseDao = hendelseDao,
            kommerBarnetTilGodeDao = kommerBarnetTilGodeDao,
            oppgaveService = oppgaveService,
            grunnlagService = grunnlagService,
            beregningKlient = beregningKlient,
            etteroppgjoerDao = etteroppgjoerDao,
            etteroppgjoerForbehandlingDao = etteroppgjoerForbehandlingDao,
            etteroppgjoerForbehandlingHendelseService = etteroppgjoerHendelseService,
            etteroppgjoerOppgaveService = etteroppgjoerOppgaveService,
        )
    }
    val generellBehandlingService by lazy {
        GenerellBehandlingService(
            generellbehandlingDao,
            oppgaveService,
            behandlingService,
            grunnlagService,
            hendelseDao,
            saksbehandlerInfoDao,
        )
    }
    val behandlingMedBrevService by lazy {
        BehandlingMedBrevService(
            behandlingMedBrevDao,
        )
    }
    val kommerBarnetTilGodeService by lazy { KommerBarnetTilGodeService(kommerBarnetTilGodeDao, behandlingDao) }
    val sjekklisteService by lazy { SjekklisteService(sjekklisteDao, behandlingService, oppgaveService) }

    private val klageBrevService by lazy { KlageBrevService(brevApiKlient) }
    val klageService by lazy {
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
    }

    val aktivitetspliktKopierService by lazy {
        AktivitetspliktKopierService(
            aktivitetspliktAktivitetsgradDao,
            aktivitetspliktUnntakDao,
        )
    }

    val revurderingService by lazy {
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
    }
    val automatiskRevurderingService by lazy {
        AutomatiskRevurderingService(
            revurderingService,
            behandlingService,
            grunnlagService,
            vedtakKlient,
            beregningKlient,
        )
    }
    val manuellRevurderingService by lazy {
        ManuellRevurderingService(
            revurderingService = revurderingService,
            behandlingService = behandlingService,
            grunnlagService = grunnlagService,
            oppgaveService = oppgaveService,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
        )
    }
    val omgjoeringKlageRevurderingService by lazy {
        OmgjoeringKlageRevurderingService(
            revurderingService = revurderingService,
            oppgaveService = oppgaveService,
            klageService = klageService,
            behandlingDao = behandlingDao,
            grunnlagService = grunnlagService,
        )
    }

    val aktivitetspliktService by lazy {
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
    }
    val gyldighetsproevingService by lazy {
        GyldighetsproevingServiceImpl(
            behandlingDao = behandlingDao,
        )
    }

    val omregningService by lazy {
        OmregningService(
            behandlingService = behandlingService,
            omregningDao = omregningDao,
            oppgaveService = oppgaveService,
        )
    }

    val tilgangService by lazy { TilgangServiceSjekkerImpl(sakTilgangDao) }

    private val externalServices: List<Pingable> by lazy {
        listOf(
            entraProxyKlient,
            navAnsattKlient,
            skjermingKlient,
            pdlTjenesterKlient,
            klageKlient,
            tilbakekrevingKlient,
        )
    }
    val selfTestService by lazy { SelfTestService(externalServices) }

    val sakService by lazy {
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
    }

    val etteroppgjoerService by lazy {
        EtteroppgjoerService(
            dao = etteroppgjoerDao,
            vedtakKlient = vedtakKlient,
            behandlingService = behandlingService,
            beregningKlient = beregningKlient,
            sigrunKlient = sigrunKlient,
            etteroppgjoerOppgaveService = etteroppgjoerOppgaveService,
            hendelseDao = hendelseDao,
        )
    }

    val etteroppgjoerDataService by lazy {
        EtteroppgjoerDataService(
            behandlingService,
            featureToggleService,
            vedtakKlient,
            beregningKlient,
        )
    }

    val doedshendelseService by lazy {
        DoedshendelseService(
            doedshendelseDao,
            pdlTjenesterKlient,
            gosysOppgaveKlient,
            ukjentBeroertDao,
        )
    }

    val inntektsjusteringSelvbetjeningService by lazy {
        InntektsjusteringSelvbetjeningService(
            oppgaveService = oppgaveService,
            behandlingService = behandlingService,
            vedtakKlient = vedtakKlient,
            rapid = rapid,
            featureToggleService = featureToggleService,
            beregningKlient = beregningKlient,
        )
    }

    private val inntektskomponentService by lazy {
        InntektskomponentService(
            klient = inntektskomponentKlient,
            featureToggleService = featureToggleService,
        )
    }

    private val pensjonsgivendeInntektService: PensjonsgivendeInntektService by lazy {
        PensjonsgivendeInntektService(
            sigrunKlient = sigrunKlient,
        )
    }

    val etteroppgjoerForbehandlingService by lazy {
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
    }

    val aarligInntektsjusteringJobbService by lazy {
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
    }

    private val grunnlagsendringsHendelseFilter by lazy {
        GrunnlagsendringsHendelseFilter(
            vedtakKlient,
            behandlingService,
        )
    }
    val grunnlagsendringshendelseService by lazy {
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
    }

    private val doedshendelseReminderJob by lazy {
        DoedshendelseReminderService(
            doedshendelseDao,
            behandlingService,
            oppgaveService,
            sakLesDao,
        )
    }
    private val doedshendelseJobService by lazy {
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
    }

    val behandlingsStatusService by lazy {
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
    }

    val behandlingInfoService by lazy {
        BehandlingInfoService(
            behandlingInfoDao,
            behandlingService,
            behandlingsStatusService,
        )
    }

    val bosattUtlandService by lazy { BosattUtlandService(bosattUtlandDao = bosattUtlandDao) }

    val tilbakekrevingBrevService by lazy {
        TilbakekrevingBrevService(
            sakService,
            brevKlient,
            brevApiKlient,
            vedtakKlient,
            grunnlagService,
        )
    }

    val lesSkatteoppgjoerHendelserJobService by lazy {
        LesSkatteoppgjoerHendelserJobService(
            dao = skatteoppgjoerHendelserDao,
            sigrunKlient = sigrunKlient,
            etteroppgjoerService = etteroppgjoerService,
            sakService = sakService,
        )
    }

    private val etteroppgjoerRevurderingBrevService by lazy {
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
    }

    private val uttrekkLoependeYtelseEtter67JobService by lazy {
        UttrekkLoependeYtelseEtter67JobService(
            vedtakKlient,
            sakService,
            nyAldersovergangService,
            featureToggleService,
        )
    }

    val etteroppgjoerForbehandlingBrevService by lazy {
        EtteroppgjoerForbehandlingBrevService(
            brevKlient = brevKlient,
            grunnlagService = grunnlagService,
            etteroppgjoerForbehandlingService = etteroppgjoerForbehandlingService,
            behandlingService = behandlingService,
            etteroppgjoerOppgaveService = etteroppgjoerOppgaveService,
        )
    }

    val vilkaarsvurderingService by lazy {
        VilkaarsvurderingService(
            vilkaarsvurderingDao,
            behandlingService,
            grunnlagService,
            behandlingsStatusService,
        )
    }

    private val vedtaksbrevService by lazy {
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
    }

    val brevService by lazy {
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
    }

    val tilbakekrevingService by lazy {
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
    }

    private val saksbehandlerJobService by lazy {
        SaksbehandlerJobService(
            saksbehandlerInfoDao,
            navAnsattKlient,
            entraProxyKlient,
        )
    }

    val oppdaterSkatteoppgjoerIkkeMottattJobService by lazy {
        OppdaterSkatteoppgjoerIkkeMottattJobService(
            featureToggleService,
            etteroppgjoerOppgaveService,
            etteroppgjoerService,
            vedtakKlient,
        )
    }

    val etteroppgjoerSvarfristUtloeptJobService by lazy {
        EtteroppgjoerSvarfristUtloeptJobService(
            etteroppgjoerService,
            oppgaveService,
            featureToggleService,
        )
    }

    private val aktivitetspliktOppgaveUnntakUtloeperJobService by lazy {
        AktivitetspliktOppgaveUnntakUtloeperJobService(
            aktivitetspliktDao,
            aktivitetspliktService,
            oppgaveService,
            vedtakKlient,
            featureToggleService,
        )
    }

    val gosysOppgaveService by lazy {
        GosysOppgaveServiceImpl(
            gosysOppgaveKlient,
            oppgaveService,
            saksbehandlerService,
            saksbehandlerInfoDao,
            pdlTjenesterKlient,
        )
    }
    val aldersovergangService by lazy { AldersovergangService(vilkaarsvurderingService) }

    val behandlingFactory by lazy {
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
    }

    val etteroppgjoerRevurderingService by lazy {
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
    }

    val migreringService by lazy {
        MigreringService(
            behandlingService = behandlingService,
        )
    }

    val aktivitetspliktOppgaveService by lazy {
        AktivitetspliktOppgaveService(
            aktivitetspliktService = aktivitetspliktService,
            oppgaveService = oppgaveService,
            sakService = sakService,
            aktivitetspliktBrevDao = aktivitetspliktBrevDao,
            brevApiKlient = brevApiKlient,
            behandlingService = behandlingService,
            beregningKlient = beregningKlient,
        )
    }

    val vedtaksvurderingService by lazy { VedtaksvurderingService(vedtaksvurderingRepository) }
    val vedtakBehandlingService by lazy {
        VedtakBehandlingService(
            vedtaksvurderingRepository = vedtaksvurderingRepository,
            beregningKlient = beregningKlient,
            vilkaarsvurderingService = vilkaarsvurderingService,
            behandlingStatusService = behandlingsStatusService,
            behandlingService = behandlingService,
            samordningsKlient = samordningKlient,
            trygdetidKlient = trygdetidKlient,
            etteroppgjorRevurderingService = etteroppgjoerRevurderingService,
            sakLesDao = sakLesDao,
        )
    }

    val vedtaksvurderingRapidService by lazy {
        VedtaksvurderingRapidService(
            publiser = { key, melding -> rapid.publiser(key.toString(), verdi = melding) },
        )
    }
    val vedtakKlageService: VedtakKlageService by lazy {
        VedtakKlageService(
            vedtaksvurderingRepository = vedtaksvurderingRepository,
            vedtaksvurderingRapidService = vedtaksvurderingRapidService,
        )
    }
    val vedtakSamordningService by lazy { VedtakSamordningService(vedtaksvurderingRepository) }
    val vedtakEtteroppgjoerService by lazy {
        VedtakEtteroppgjoerService(
            repository = vedtaksvurderingRepository,
            vedtakSamordningService = vedtakSamordningService,
        )
    }
    val vedtakTilbakekrevingService: VedtakTilbakekrevingService by lazy {
        VedtakTilbakekrevingService(
            repository = vedtaksvurderingRepository,
            featureToggleService = featureToggleService,
        )
    }

    val vedtakKlient: VedtakKlient by lazy {
        if (brukNyVedtakKlientInternal) {
            VedtakInternalService(
                vedtakTilbakekrevingService = vedtakTilbakekrevingService,
                vedtakKlageService = vedtakKlageService,
                vedtakBehandlingService = vedtakBehandlingService,
                vedtaksvurderingService = vedtaksvurderingService,
            )
        } else {
            vedtakKlientDefault
        }
    }

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
            lesSkatteoppgjoerHendelserJobService = lesSkatteoppgjoerHendelserJobService,
            erLeader = { leaderElectionKlient.isLeader() },
            initialDelay = Duration.of(3, ChronoUnit.MINUTES).toMillis(),
            interval =
                if (isProd()) {
                    Duration.of(1, ChronoUnit.HOURS)
                } else {
                    Duration.of(5, ChronoUnit.MINUTES)
                },
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
