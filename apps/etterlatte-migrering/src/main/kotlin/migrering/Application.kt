package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.migrering.ApplicationContext
import no.nav.etterlatte.migrering.MigrerSpesifikkSak
import no.nav.etterlatte.migrering.Migrering
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

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
}