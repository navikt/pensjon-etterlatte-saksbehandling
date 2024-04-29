package no.nav.etterlatte.testdata

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.testdata.automatisk.AvkortingService
import no.nav.etterlatte.testdata.automatisk.BeregningService
import no.nav.etterlatte.testdata.automatisk.BrevService
import no.nav.etterlatte.testdata.automatisk.SakService
import no.nav.etterlatte.testdata.automatisk.TrygdetidService
import no.nav.etterlatte.testdata.automatisk.VedtaksvurderingService
import no.nav.etterlatte.testdata.automatisk.VilkaarsvurderingService

class AppBuilder {
    private val config: Config = ConfigFactory.load()

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
        val vilkaarsvurderingService =
            VilkaarsvurderingService(
                settOppHttpClient("vilkaarsvurdering"),
                "http://etterlatte-vilkaarsvurdering",
                config.getString("vilkaarsvurdering.client.id"),
            )
        val httpClientBehandling = settOppHttpClient("behandling")
        val sakService =
            SakService(
                httpClientBehandling,
                "http://etterlatte-behandling",
                config.getString("behandling.client.id"),
            )
        val trygdetidService =
            TrygdetidService(
                settOppHttpClient("trygdetid"),
                "http://etterlatte-trygdetid",
                config.getString("trygdetid.client.id"),
            )
        val avkortingService =
            AvkortingService(
                httpClientBehandling,
                "http://etterlatte-behandling",
                config.getString("behandling.client.id"),
            )
        val beregningService =
            BeregningService(
                settOppHttpClient("beregning"),
                "http://etterlatte-beregning",
                config.getString("beregning.client.id"),
            )
        val vedtaksvurderingService =
            VedtaksvurderingService(
                settOppHttpClient("vedtaksvurdering"),
                "http://etterlatte-vedtaksvurdering",
                config.getString("vedtaksvurdering.client.id"),
            )
        val brevService =
            BrevService(
                settOppHttpClient("brev"),
                "http://etterlatte-brev-api",
                config.getString("brev.client.id"),
            )

        return Behandler(
            avkortingService = avkortingService,
            beregningService = beregningService,
            brevService = brevService,
            sakService = sakService,
            trygdetidService = trygdetidService,
            vedtaksvurderingService = vedtaksvurderingService,
            vilkaarsvurderingService = vilkaarsvurderingService,
        )
    }
}

const val BEGRUNNELSE = "Automatisk behandla testsak"
