package no.nav.etterlatte

import no.nav.etterlatte.fordeler.Fordeler
import no.nav.etterlatte.fordeler.FordelerKriterier
import no.nav.etterlatte.fordeler.FordelerRepository
import no.nav.etterlatte.fordeler.FordelerService
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory
import rapidsandrivers.getRapidEnv

val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val ab = AppBuilder(Miljoevariabler(rapidEnv))
        Fordeler(
            rapidsConnection = rapidsConnection,
            fordelerService = FordelerService(
                fordelerKriterier = FordelerKriterier(),
                pdlTjenesterKlient = ab.pdlTjenesterKlient(),
                fordelerRepository = FordelerRepository(ab.createDataSource()),
                behandlingKlient = ab.behandlingKlient(),
                maxFordelingTilDoffen = ab.longFeature("FEATURE_MAX_FORDELING_TIL_DOFFEN")
            )
        )
    }.start()
}