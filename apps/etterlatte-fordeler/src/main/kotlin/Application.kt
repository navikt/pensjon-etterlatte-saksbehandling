package no.nav.etterlatte

import no.nav.etterlatte.fordeler.Fordeler
import no.nav.etterlatte.fordeler.FordelerKriterier
import no.nav.etterlatte.fordeler.FordelerRepository
import no.nav.etterlatte.fordeler.FordelerService
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory

val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    sikkerLogg.info("SikkerLogg: etterlatte-fordeler oppstart")

    val env = System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", requireNotNull(get("NAIS_APP_NAME")).replace("-", ""))
    }
    val dataSource = DataSourceBuilder.createDataSource(env).apply { migrate() }

    val appBuilder = AppBuilder(env)
    RapidApplication.create(env)
        .also {
            Fordeler(
                rapidsConnection = it,
                fordelerService = FordelerService(
                    FordelerKriterier(),
                    appBuilder.pdlTjenesterKlient(),
                    FordelerRepository(dataSource),
                    maxFordelingTilDoffen = env.longFeature("FEATURE_MAX_FORDELING_TIL_DOFFEN")
                )
            )
        }.start()
}

fun Map<String, String>.longFeature(featureName: String, default: Long = 0): Long {
    return (this[featureName]?.toLong() ?: default).takeIf { it > -1 } ?: Long.MAX_VALUE
}