package no.nav.etterlatte.config.modules

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.EnvKey.ETTERLATTE_KLAGE_API_URL
import no.nav.etterlatte.EnvKey.ETTERLATTE_TILBAKEKREVING_URL
import no.nav.etterlatte.EnvKey.NAVANSATT_URL
import no.nav.etterlatte.EnvKey.NORG2_URL
import no.nav.etterlatte.EnvKey.SKJERMING_URL
import no.nav.etterlatte.arbeidOgInntekt.ArbeidOgInntektKlient
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentKlient
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentKlientImpl
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlientImpl
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
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.BrevKlientImpl
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.PesysKlientImpl
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.common.klienter.SkjermingKlientImpl
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.kodeverk.KodeverkKlient
import no.nav.etterlatte.kodeverk.KodeverkKlientImpl
import no.nav.etterlatte.krr.KrrKlient
import no.nav.etterlatte.krr.KrrKlientImpl
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveKlient
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveKlientImpl

class KlientModule(
    private val config: Config,
    private val env: Miljoevariabler,
    private val httpClientFactory: HttpClientFactory,
    private val featureToggleService: FeatureToggleService,
    // Test overrides
    navAnsattKlientOverride: NavAnsattKlient? = null,
    norg2KlientOverride: Norg2Klient? = null,
    leaderElectionHttpClientOverride: HttpClient? = null,
    beregningKlientOverride: BeregningKlient? = null,
    trygdetidKlientOverride: TrygdetidKlient? = null,
    gosysOppgaveKlientOverride: GosysOppgaveKlient? = null,
    vedtakKlientOverride: VedtakKlient? = null,
    brevApiKlientOverride: BrevApiKlient? = null,
    brevKlientOverride: BrevKlient? = null,
    klageHttpClientOverride: HttpClient? = null,
    tilbakekrevingKlientOverride: TilbakekrevingKlient? = null,
    pesysKlientOverride: PesysKlient? = null,
    krrKlientOverride: KrrKlient? = null,
    entraProxyKlientOverride: EntraProxyKlient? = null,
    pdlTjenesterKlientOverride: PdlTjenesterKlient? = null,
    kodeverkKlientOverride: KodeverkKlient? = null,
    skjermingKlientOverride: SkjermingKlient? = null,
    inntektskomponentKlientOverride: InntektskomponentKlient? = null,
    sigrunKlientOverride: SigrunKlient? = null,
    arbeidOgInntektKlientOverride: ArbeidOgInntektKlient? = null,
) {
    private val standardHttpClient: HttpClient by lazy { httpClient() }
    private val httpClientForventSuksess: HttpClient by lazy { httpClient(forventSuksess = true) }

    val navAnsattKlient: NavAnsattKlient by lazy {
        navAnsattKlientOverride ?: NavAnsattKlientImpl(
            httpClientFactory.navAnsattKlient(),
            env.requireEnvValue(NAVANSATT_URL),
        ).also { it.asyncPing() }
    }

    val norg2Klient: Norg2Klient by lazy {
        norg2KlientOverride ?: Norg2KlientImpl(client = standardHttpClient, url = env.requireEnvValue(NORG2_URL))
    }

    val beregningKlient: BeregningKlient by lazy {
        beregningKlientOverride ?: BeregningKlientImpl(config = config, httpClient = standardHttpClient)
    }

    val trygdetidKlient: TrygdetidKlient by lazy {
        trygdetidKlientOverride ?: TrygdetidKlientImpl(config = config, httpClient = standardHttpClient)
    }

    val gosysOppgaveKlient: GosysOppgaveKlient by lazy {
        gosysOppgaveKlientOverride ?: GosysOppgaveKlientImpl(config = config, httpClient = standardHttpClient)
    }

    val vedtakKlient: VedtakKlient by lazy {
        vedtakKlientOverride ?: VedtakKlientImpl(config = config, httpClient = standardHttpClient)
    }

    val brevApiKlient: BrevApiKlient by lazy {
        brevApiKlientOverride ?: BrevApiKlientObo(config = config, client = httpClientForventSuksess)
    }

    val brevKlient: BrevKlient by lazy {
        brevKlientOverride ?: BrevKlientImpl(config = config, client = httpClientForventSuksess)
    }

    val klageHttpClient: HttpClient by lazy {
        klageHttpClientOverride ?: httpClientFactory.klageKlient()
    }

    val klageKlient: KlageKlientImpl by lazy {
        KlageKlientImpl(client = klageHttpClient, url = env.requireEnvValue(ETTERLATTE_KLAGE_API_URL))
    }

    val tilbakekrevingKlient: TilbakekrevingKlient by lazy {
        tilbakekrevingKlientOverride ?: TilbakekrevingKlientImpl(
            client = httpClientFactory.tilbakekrevingKlient(),
            url = env.requireEnvValue(ETTERLATTE_TILBAKEKREVING_URL),
        )
    }

    val pesysKlient: PesysKlient by lazy {
        pesysKlientOverride ?: PesysKlientImpl(config = config, httpClient = standardHttpClient)
    }

    val krrKlient: KrrKlient by lazy {
        krrKlientOverride ?: KrrKlientImpl(client = httpClientFactory.krrKlient(), url = config.getString("krr.url"))
    }

    val entraProxyKlient: EntraProxyKlient by lazy {
        entraProxyKlientOverride ?: EntraProxyKlientImpl(
            client = httpClientFactory.entraProxyKlient(),
            url = config.getString("entraProxy.url"),
        )
    }

    val pdlTjenesterKlient: PdlTjenesterKlient by lazy {
        pdlTjenesterKlientOverride ?: PdlTjenesterKlientImpl(config = config, client = httpClientFactory.pdlKlient())
    }

    val kodeverkKlient: KodeverkKlient by lazy {
        kodeverkKlientOverride ?: KodeverkKlientImpl(config = config, httpKlient = standardHttpClient)
    }

    val skjermingKlient: SkjermingKlient by lazy {
        skjermingKlientOverride ?: SkjermingKlientImpl(
            httpClient = httpClientFactory.skjermingKlient(),
            url = env.requireEnvValue(SKJERMING_URL),
        )
    }

    val inntektskomponentKlient: InntektskomponentKlient by lazy {
        inntektskomponentKlientOverride ?: InntektskomponentKlientImpl(
            httpClient = httpClientFactory.inntektskomponentKlient(),
            url = config.getString("inntektskomponenten.url"),
        )
    }

    val sigrunKlient: SigrunKlient by lazy {
        sigrunKlientOverride ?: SigrunKlientImpl(
            httpClient = httpClientFactory.sigrunKlient(),
            url = config.getString("sigrun.url"),
            featureToggleService = featureToggleService,
        )
    }

    val arbeidOgInntektKlient: ArbeidOgInntektKlient by lazy {
        arbeidOgInntektKlientOverride ?: ArbeidOgInntektKlient(
            client = standardHttpClient,
            url = config.getString("arbeidOgInntekt.url"),
        )
    }

    val leaderElectionHttpClient: HttpClient by lazy {
        leaderElectionHttpClientOverride ?: httpClient()
    }

    val pingableKlienter: List<Pingable> by lazy {
        listOf(
            entraProxyKlient,
            navAnsattKlient,
            skjermingKlient,
            pdlTjenesterKlient,
            klageKlient,
            tilbakekrevingKlient,
        )
    }
}
