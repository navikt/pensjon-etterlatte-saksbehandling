package no.nav.etterlatte.testdata

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
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
        httpClientClientCredentials(
            azureAppClientId = config.getString("azure.app.client.id"),
            azureAppJwk = config.getString("azure.app.jwk"),
            azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
            azureAppScope = config.getString("$appnavn.azure.scope"),
        )

    fun lagBehandler(): Behandler {
        val vilkaarsvurderingService =
            VilkaarsvurderingService(
                settOppHttpClient("vilkaarsvurdering"),
                "http://etterlatte-vilkaarsvurdering",
            )
        val httpClientBehandling = settOppHttpClient("behandling")
        val sakService =
            SakService(
                httpClientBehandling,
                "http://etterlatte-behandling",
            )
        val trygdetidService =
            TrygdetidService(
                settOppHttpClient("trygdetid"),
                "http://etterlatte-trygdetid",
            )
        val avkortingService =
            AvkortingService(
                httpClientBehandling,
                "http://etterlatte-behandling",
            )
        val beregningService =
            BeregningService(
                settOppHttpClient("beregning"),
                "http://etterlatte-beregning",
            )
        val vedtaksvurderingService =
            VedtaksvurderingService(
                settOppHttpClient("vedtaksvurdering"),
                "http://etterlatte-vilkaarsvurdering",
            )
        val brevService =
            BrevService(
                settOppHttpClient("brev"),
                "http://etterlatte-brev-api",
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
