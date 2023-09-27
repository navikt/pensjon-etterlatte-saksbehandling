package no.nav.etterlatte

import no.nav.etterlatte.fordeler.Fordeler
import no.nav.etterlatte.fordeler.FordelerKriterier
import no.nav.etterlatte.fordeler.FordelerRepository
import no.nav.etterlatte.fordeler.FordelerService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleProperties
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.requireEnvValue
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

val sikkerLogg = sikkerlogger()

fun main() {
    val rapidEnv = getRapidEnv()
    val featureToggleService = FeatureToggleService.initialiser(featureToggleProperties(rapidEnv))

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

private fun featureToggleProperties(env: Map<String, String>) =
    FeatureToggleProperties(
        applicationName = env.requireEnvValue("NAIS_APP_NAME"),
        host = env.requireEnvValue("UNLEASH_SERVER_API_URL"),
        apiKey = env.requireEnvValue("UNLEASH_SERVER_API_TOKEN"),
    )
