package no.nav.etterlatte

import no.nav.etterlatte.database.DataSourceBuilder
import no.nav.etterlatte.statistikk.StatistikkRepository
import no.nav.etterlatte.statistikk.StatistikkRiver
import no.nav.etterlatte.statistikk.StatistikkService
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.vedlikehold.registrerVedlikeholdsriver

fun main() {
    System.getenv()
        .apply {
            put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
        }
        .also { env ->
            DataSourceBuilder(env).apply {
                migrate()
            }.dataSource
                .also { dataSource ->
                    val statistikkService = StatistikkService(StatistikkRepository.using(dataSource))
                    RapidApplication.create(env).apply {
                        StatistikkRiver(this, statistikkService)
                        registrerVedlikeholdsriver(statistikkService)
                    }.start()
                }
        }
}