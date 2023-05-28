package no.nav.etterlatte

import no.nav.etterlatte.libs.database.KotliqueryRepository
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.migrering.ApplicationContext
import no.nav.etterlatte.migrering.Migrering
import no.nav.etterlatte.migrering.PesysRepository
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rapidsandrivers.getRapidEnv

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

fun main() = ApplicationContext().let { Server(it).run() }

// Test3

class Server(private val context: ApplicationContext) {
    init {
        sikkerLogg.info("SikkerLogg: etterlatte-migrering oppstart")
    }

    fun run() = with(context) {
        dataSource.migrate()
        val rapidEnv = getRapidEnv()
        RapidApplication.create(rapidEnv).also { rapidsConnection ->
            Migrering(rapidsConnection, PesysRepository(KotliqueryRepository(dataSource)))
        }.start()
    }
}