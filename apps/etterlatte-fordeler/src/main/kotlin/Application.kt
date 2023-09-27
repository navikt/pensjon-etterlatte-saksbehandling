package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.fordeler.Fordeler
import no.nav.etterlatte.fordeler.FordelerKriterier
import no.nav.etterlatte.fordeler.FordelerRepository
import no.nav.etterlatte.fordeler.FordelerService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

val sikkerLogg = sikkerlogger()

fun main() {
    val rapidEnv = getRapidEnv()
    val featureToggleService = FeatureToggleService.initialiser(featureToggleProperties(ConfigFactory.load()))

    RapidApplication.create(rapidEnv).also { rapidsConnection ->
        val ab = AppBuilder(Miljoevariabler(rapidEnv))
        Fordeler(
            rapidsConnection = rapidsConnection,
            fordelerService =
                FordelerService(
                    fordelerKriterier = FordelerKriterier(featureToggleService),
                    pdlTjenesterKlient = ab.pdlTjenesterKlient(),
                    fordelerRepository = FordelerRepository(ab.createDataSource()),
                    behandlingKlient = ab.behandlingKlient(),
                    maxFordelingTilGjenny = ab.longFeature("FEATURE_MAX_FORDELING_TIL_GJENNY"),
                ),
        )
    }.start()
}

private fun featureToggleProperties(config: Config) =
    FeatureToggleProperties(
        applicationName = config.getString("funksjonsbrytere.unleash.applicationName"),
        host = config.getString("funksjonsbrytere.unleash.host"),
        apiKey = config.getString("funksjonsbrytere.unleash.token"),
    )
