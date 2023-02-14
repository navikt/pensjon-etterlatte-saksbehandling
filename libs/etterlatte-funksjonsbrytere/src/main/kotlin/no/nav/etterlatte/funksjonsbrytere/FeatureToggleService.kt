package no.nav.etterlatte.funksjonsbrytere

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.UnleashContextProvider
import io.getunleash.strategy.GradualRolloutRandomStrategy
import io.getunleash.util.UnleashConfig
import org.slf4j.LoggerFactory
import java.net.URI

fun initialiser(env: Map<String, String>): FeatureToggleService {
    val enabled = env.getOrDefault(Properties.ENABLED.navn, false) as Boolean
    return if (enabled) {
        UnleashFeatureToggleService(
            Unleash(
                enabled = true,
                uri = URI(env.getOrDefault(Properties.URI.navn, "")),
                cluster = env.getOrDefault(Properties.CLUSTER.navn, ""),
                applicationName = env.getOrDefault(Properties.APPLICATIONNAME.navn, "")
            )
        )
    } else {
        DummyFeatureToggleService()
    }
}

interface FeatureToggleService {
    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean
}

class UnleashFeatureToggleService(private val unleash: Unleash) : FeatureToggleService {

    private val defaultUnleash = DefaultUnleash(
        UnleashConfig.builder()
            .appName(unleash.applicationName)
            .unleashAPI(unleash.uri)
            .unleashContextProvider(lagUnleashContextProvider())
            .build(),
        ByClusterStrategy(unleash.cluster),
        GradualRolloutRandomStrategy()
    )

    private fun lagUnleashContextProvider() = UnleashContextProvider {
        UnleashContext.builder()
            .appName(unleash.applicationName)
            .build()
    }

    override fun isEnabled(toggleId: String, defaultValue: Boolean) = defaultUnleash.isEnabled(toggleId, defaultValue)
}

class DummyFeatureToggleService : FeatureToggleService {

    init {
        logger.warn(
            "Funksjonsbryter-funksjonalitet er skrudd AV. " +
                "Gir standardoppførsel for alle funksjonsbrytere, dvs 'false'"
        )
    }

    private val overstyrteBrytere = mapOf(
        Pair(FeatureToggleConfig.TREKK_I_LØPENDE_UTBETALING, true)
    )

    override fun isEnabled(toggleId: String, defaultValue: Boolean) = overstyrteBrytere.getOrDefault(toggleId, true)

    companion object {
        private val logger = LoggerFactory.getLogger(FeatureToggleService::class.java)
    }
}