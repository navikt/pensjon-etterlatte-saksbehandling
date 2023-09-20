package no.nav.etterlatte.funksjonsbrytere

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.UnleashContextProvider
import io.getunleash.strategy.GradualRolloutRandomStrategy
import io.getunleash.util.UnleashConfig

interface FeatureToggleService {
    fun isEnabled(
        toggleId: FeatureToggle,
        defaultValue: Boolean,
    ): Boolean

    companion object {
        fun initialiser(properties: FeatureToggleProperties) = UnleashFeatureToggleService(properties)
    }
}

class UnleashFeatureToggleService(private val properties: FeatureToggleProperties) : FeatureToggleService {
    private val defaultUnleash =
        DefaultUnleash(
            UnleashConfig.builder()
                .appName(properties.applicationName)
                .unleashAPI(properties.uri())
                .unleashContextProvider(lagUnleashContextProvider())
                .apiKey(properties.apiKey)
                .build(),
            GradualRolloutRandomStrategy(),
        )

    private fun lagUnleashContextProvider() =
        UnleashContextProvider {
            UnleashContext.builder()
                .appName(properties.applicationName)
                .build()
        }

    override fun isEnabled(
        toggleId: FeatureToggle,
        defaultValue: Boolean,
    ) = defaultUnleash.isEnabled(toggleId.key(), defaultValue)
}
