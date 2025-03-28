package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import no.nav.etterlatte.klienter.BrevapiKlient
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.libs.ktor.httpClientClientCredentials
import no.nav.etterlatte.rapidsandrivers.configFromEnvironment
import no.nav.etterlatte.rivers.DistribuerBrevRiver
import no.nav.etterlatte.rivers.JournalfoerVedtaksbrevRiver
import no.nav.etterlatte.rivers.OpprettJournalfoerOgDistribuerRiver
import no.nav.etterlatte.rivers.SamordningsnotatRiver
import no.nav.helse.rapids_rivers.RapidsConnection
import rapidsandrivers.initRogR

fun main() {
    ApplicationBuilder()
}

class ApplicationBuilder {
    private val config = ConfigFactory.load()
    private val brevhttpKlient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = config.getString("azure.app.client.id"),
            azureAppJwk = config.getString("azure.app.jwk"),
            azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
            azureAppScope = config.getString("brevapi.azure.scope"),
        ).config {
            install(HttpTimeout)
        }
    }

    private val grunnlagHttpKlient: HttpClient by lazy {
        httpClientClientCredentials(
            azureAppClientId = config.getString("azure.app.client.id"),
            azureAppJwk = config.getString("azure.app.jwk"),
            azureAppWellKnownUrl = config.getString("azure.app.well.known.url"),
            azureAppScope = config.getString("behandling.azure.scope"),
        )
    }
    private val brevapiKlient = BrevapiKlient(config, brevhttpKlient)
    private val grunnlagKlient = GrunnlagKlient(config, grunnlagHttpKlient)
    private val connection =
        initRogR(
            applikasjonsnavn = "brev-kafka",
            configFromEnvironment = { configFromEnvironment(it) },
        ) { rapidsConnection, _ ->

            rapidsConnection.register(
                object : RapidsConnection.StatusListener {
                    override fun onStartup(rapidsConnection: RapidsConnection) {
                    }
                },
            )

            OpprettJournalfoerOgDistribuerRiver(
                brevapiKlient,
                grunnlagKlient,
                rapidsConnection,
            )
            JournalfoerVedtaksbrevRiver(rapidsConnection, brevapiKlient)
            DistribuerBrevRiver(rapidsConnection, brevapiKlient)
            SamordningsnotatRiver(rapidsConnection, brevapiKlient)
        }
}
