package no.nav.etterlatte.testdata

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.EnvKey.BRUK_INTERN_BEREGNING
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.testdata.automatisk.AvkortingService
import no.nav.etterlatte.testdata.automatisk.BehandlingService
import no.nav.etterlatte.testdata.automatisk.BeregningService
import no.nav.etterlatte.testdata.automatisk.BrevService
import no.nav.etterlatte.testdata.automatisk.GrunnlagService
import no.nav.etterlatte.testdata.automatisk.TrygdetidService
import no.nav.etterlatte.testdata.automatisk.VedtaksvurderingService
import no.nav.etterlatte.testdata.automatisk.VilkaarsvurderingService

class AppBuilder(
    private val config: Config = ConfigFactory.load(),
    env: Miljoevariabler = Miljoevariabler.systemEnv(),
) {
    private val beregningApp =
        if (env.getOrDefault(BRUK_INTERN_BEREGNING, "false").toBooleanStrict()) "behandling" else "beregning"

    private fun settOppHttpClient(appnavn: String) =
        DownstreamResourceClient(
            httpClient =
                httpClientClientCredentials(
                    azureAppClientId = config.getString("azure.app.client.id"),
                    azureAppJwk = config.getString("azure.app.jwk"),
                    azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
                    azureAppScope = config.getString("$appnavn.azure.scope"),
                ),
            azureAdClient = AzureAdClient(config),
        )

    fun lagBehandler(): Behandler {
        val behandlingVilkaarsvurderingService =
            VilkaarsvurderingService(
                settOppHttpClient("behandling"),
                "http://etterlatte-behandling",
                config.getString("behandling.client.id"),
            )
        val grunnlagService =
            GrunnlagService(
                settOppHttpClient("behandling"),
                "http://etterlatte-behandling",
                config.getString("behandling.client.id"),
            )
        val behandlingService =
            BehandlingService(
                settOppHttpClient("behandling"),
                "http://etterlatte-behandling",
                config.getString("behandling.client.id"),
            )
        val trygdetidService =
            TrygdetidService(
                settOppHttpClient("behandling"),
                "http://etterlatte-behandling",
                config.getString("behandling.client.id"),
            )
        val httpClientBeregning = settOppHttpClient(beregningApp)
        val avkortingService = lagAvkortingService(httpClientBeregning)
        val beregningService = lagBeregningService(httpClientBeregning)
        val vedtaksvurderingService =
            VedtaksvurderingService(
                settOppHttpClient("behandling"),
                "http://etterlatte-behandling",
                config.getString("behandling.client.id"),
            )
        val brevService =
            BrevService(
                settOppHttpClient("behandling"),
                "http://etterlatte-behandling",
                config.getString("behandling.client.id"),
            )

        return Behandler(
            avkortingService = avkortingService,
            beregningService = beregningService,
            brevService = brevService,
            grunnlagService = grunnlagService,
            behandlingService = behandlingService,
            trygdetidService = trygdetidService,
            vedtaksvurderingService = vedtaksvurderingService,
            vilkaarsvurderingService = behandlingVilkaarsvurderingService,
        )
    }

    internal fun lagAvkortingService(httpClient: DownstreamResourceClient): AvkortingService =
        AvkortingService(
            httpClient,
            config.getString("$beregningApp.resource.url"),
            config.getString("$beregningApp.client.id"),
        )

    internal fun lagBeregningService(httpClient: DownstreamResourceClient): BeregningService =
        BeregningService(
            httpClient,
            config.getString("$beregningApp.resource.url"),
            config.getString("$beregningApp.client.id"),
        )
}

const val BEGRUNNELSE = "Automatisk behandla testsak"
