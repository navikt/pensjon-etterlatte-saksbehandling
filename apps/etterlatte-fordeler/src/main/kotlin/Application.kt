package no.nav.etterlatte

import no.nav.etterlatte.fordeler.Fordeler
import no.nav.etterlatte.fordeler.FordelerKriterier
import no.nav.etterlatte.fordeler.FordelerRepository
import no.nav.etterlatte.fordeler.FordelerService
import no.nav.etterlatte.rapidsandrivers.init
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    sikkerLogg.info("SikkerLogg: etterlatte-fordeler oppstart")
    init(
        { AppBuilder(it) },
        { rc: RapidsConnection, ab: AppBuilder ->
            Fordeler(
                rapidsConnection = rc,
                fordelerService = FordelerService(
                    FordelerKriterier(),
                    ab.pdlTjenesterKlient(),
                    FordelerRepository(ab.createDataSource()),
                    maxFordelingTilDoffen = ab.longFeature("FEATURE_MAX_FORDELING_TIL_DOFFEN")
                )
            )
        }
    )
}