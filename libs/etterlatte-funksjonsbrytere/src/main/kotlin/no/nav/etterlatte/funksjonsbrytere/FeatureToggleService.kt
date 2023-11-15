package no.nav.etterlatte.funksjonsbrytere

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.UnleashContextProvider
import io.getunleash.strategy.GradualRolloutRandomStrategy
import io.getunleash.strategy.GradualRolloutUserIdStrategy
import io.getunleash.util.UnleashConfig
import org.slf4j.LoggerFactory

interface FeatureToggleService {
    fun isEnabled(
        toggleId: FeatureToggle,
        defaultValue: Boolean,
        context: UnleashContext? = null,
    ): Boolean

    companion object {
        fun initialiser(properties: FeatureToggleProperties) = UnleashFeatureToggleService(properties)
    }
}

class UnleashFeatureToggleService(private val properties: FeatureToggleProperties) : FeatureToggleService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val defaultUnleash =
        DefaultUnleash(
            UnleashConfig.builder()
                .appName(properties.applicationName)
                .unleashAPI(properties.uri())
                .unleashContextProvider(lagUnleashContextProvider())
                .apiKey(properties.apiKey)
                .build(),
            GradualRolloutRandomStrategy(),
            GradualRolloutUserIdStrategy(),
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
        context: UnleashContext?,
    ) = try {
        context?.let { defaultUnleash.isEnabled(toggleId.key(), merge(it), defaultValue) }
            ?: defaultUnleash.isEnabled(toggleId.key(), defaultValue)
    } catch (e: Exception) {
        logger.warn("Fikk feilmelding fra Unleash for toggle $toggleId, bruker defaultverdi $defaultValue", e)
        defaultValue
    }

    private fun merge(other: UnleashContext): UnleashContext {
        return UnleashContext.builder()
            .appName(other.appName.orElse(properties.applicationName))
            .userId(other.userId.orElse(null))
            .sessionId(other.sessionId.orElse(null))
            .build()
    }
}
