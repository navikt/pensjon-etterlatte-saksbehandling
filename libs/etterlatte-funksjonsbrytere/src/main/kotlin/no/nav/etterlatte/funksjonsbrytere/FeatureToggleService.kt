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
                uri = URI(env[Properties.URI.navn] ?: throw IllegalArgumentException("Unleash-URI er ikke definert")),
                cluster = env[Properties.CLUSTER.navn]
                    ?: throw IllegalArgumentException("Unleash-cluster er ikke definert"),
                applicationName = env[Properties.APPLICATIONNAME.navn]
                    ?: throw IllegalArgumentException("Unleash-applikasjonsnavn er ikke definert")
            )
        )
    } else {
        DummyFeatureToggleService()
    }
}

interface FeatureToggleService {
    fun isEnabled(toggleId: FeatureToggle, defaultValue: Boolean): Boolean
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

    override fun isEnabled(toggleId: FeatureToggle, defaultValue: Boolean) =
        defaultUnleash.isEnabled(toggleId.key(), defaultValue)
}

class DummyFeatureToggleService : FeatureToggleService {

    init {
        logger.warn(
            "Funksjonsbryter-funksjonalitet er skrudd AV. " +
                "Gir standardoppførsel for alle funksjonsbrytere, dvs 'false'"
        )
    }

    private val overstyrteBrytere = mapOf(
        Pair(FellesFeatureToggle.NoOperationToggle, true)
    )

    override fun isEnabled(toggleId: FeatureToggle, defaultValue: Boolean) =
        overstyrteBrytere.getOrDefault(toggleId, true)

    companion object {
        private val logger = LoggerFactory.getLogger(FeatureToggleService::class.java)
    }
}