package no.nav.etterlatte

import no.nav.etterlatte.fordeler.Fordeler
import no.nav.etterlatte.fordeler.FordelerKriterier
import no.nav.etterlatte.fordeler.FordelerRepository
import no.nav.etterlatte.fordeler.FordelerService
import no.nav.etterlatte.rapidsandrivers.startRapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    sikkerLogg.info("SikkerLogg: etterlatte-fordeler oppstart")
    startRapidApplication(
        { AppBuilder(it) },
        { rc: RapidsConnection, ab: AppBuilder ->
            Fordeler(
                rapidsConnection = rc,
                fordelerService = FordelerService(
                    fordelerKriterier = FordelerKriterier(),
                    pdlTjenesterKlient = ab.pdlTjenesterKlient(),
                    fordelerRepository = FordelerRepository(ab.createDataSource()),
                    behandlingClient = ab.behandlingClient(),
                    maxFordelingTilDoffen = ab.longFeature("FEATURE_MAX_FORDELING_TIL_DOFFEN")
                )
            )
        }
    )
}