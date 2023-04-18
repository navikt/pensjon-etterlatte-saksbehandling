package no.nav.etterlatte

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.fordeler.Fordeler
import no.nav.etterlatte.fordeler.FordelerKriterier
import no.nav.etterlatte.fordeler.FordelerRepository
import no.nav.etterlatte.fordeler.FordelerService
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.skjerming.ServiceStatus
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory
import rapidsandrivers.getRapidEnv

val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    val logger = LoggerFactory.getLogger("FordelerMain")

    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val ab = AppBuilder(Miljoevariabler(rapidEnv))
        val skjermingKlient = ab.skjermingKlient()
        runBlocking {
            val ping = ab.skjermingKlient().ping()
            when (ping.getStatus()) {
                ServiceStatus.UP -> logger.info("Skjermingstjeneste svarer OK")
                ServiceStatus.DOWN -> logger.warn("Skjermingstjeneste svarer IKKE ok. ${ping.toStringServiceDown()}")
            }
        }
        Fordeler(
            rapidsConnection = rapidsConnection,
            fordelerService = FordelerService(
                fordelerKriterier = FordelerKriterier(),
                pdlTjenesterKlient = ab.pdlTjenesterKlient(),
                fordelerRepository = FordelerRepository(ab.createDataSource()),
                behandlingKlient = ab.behandlingKlient(),
                skjermingKlient = skjermingKlient,
                skalBrukeSkjermingsklient = ab.skalBrukeSkjermingsklient(),
                maxFordelingTilDoffen = ab.longFeature("FEATURE_MAX_FORDELING_TIL_DOFFEN")
            )
        )
    }.start()
}