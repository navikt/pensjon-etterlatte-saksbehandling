package no.nav.etterlatte

import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.migrering.ApplicationContext
import no.nav.etterlatte.migrering.MigrerSpesifikkSak
import no.nav.etterlatte.migrering.Migrering
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rapidsandrivers.getRapidEnv
import java.net.URI

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

fun main() = ApplicationContext().let { Server(it).run() }

internal class Server(private val context: ApplicationContext) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-migrering")
    }

    fun run() = with(context) {
        dataSource.migrate()
        val rapidEnv = getRapidEnv()
        RapidApplication.create(rapidEnv).also { rapidsConnection ->
            Migrering(rapidsConnection, pesysRepository, sakmigrerer)
            MigrerSpesifikkSak(rapidsConnection, penklient, pesysRepository, sakmigrerer)
        }.start()
    }

    private fun featureToggleProperties(config: Config) = FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        uri = URI(config.getString("funksjonsbrytere.unleash.uri")),
        cluster = config.getString("funksjonsbrytere.unleash.cluster")
    )
}