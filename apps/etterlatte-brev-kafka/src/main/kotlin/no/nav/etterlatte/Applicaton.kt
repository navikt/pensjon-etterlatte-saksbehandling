package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.rapidsandrivers.configFromEnvironment
import no.nav.etterlatte.rivers.DistribuerBrevRiver
import no.nav.etterlatte.rivers.JournalfoerVedtaksbrevRiver
import no.nav.etterlatte.rivers.OpprettJournalfoerOgDistribuerRiver
import no.nav.etterlatte.rivers.SamordningsnotatRiver
import no.nav.etterlatte.rivers.VedtaksbrevUnderkjentRiver
import no.nav.helse.rapids_rivers.RapidsConnection
import rapidsandrivers.initRogR

fun main() {
    ApplicationBuilder()
}

class ApplicationBuilder {
    private val config = ConfigFactory.load()
    private val brevapiKlient = BrevapiKlient(config, httpClient())
    private val connection =
        initRogR(
            applikasjonsnavn = "brev-kafka",
            configFromEnvironment = { configFromEnvironment(it) },
        ) { rapidsConnection, _ ->

            rapidsConnection.register(
                object : RapidsConnection.StatusListener {
                    override fun onStartup(rapidsConnection: RapidsConnection) {
                        // TODO: empty?
                    }
                },
            )

            OpprettJournalfoerOgDistribuerRiver(
                brevapiKlient,
                rapidsConnection,
            )
            JournalfoerVedtaksbrevRiver(rapidsConnection, brevapiKlient)
            VedtaksbrevUnderkjentRiver(rapidsConnection, brevapiKlient)
            DistribuerBrevRiver(rapidsConnection, brevapiKlient)
            SamordningsnotatRiver(rapidsConnection, brevapiKlient)
        }
}
