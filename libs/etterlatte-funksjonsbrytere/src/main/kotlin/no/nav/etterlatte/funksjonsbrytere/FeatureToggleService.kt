package no.nav.etterlatte.funksjonsbrytere

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.UnleashContextProvider
import io.getunleash.strategy.GradualRolloutRandomStrategy
import io.getunleash.util.UnleashConfig
import org.slf4j.LoggerFactory

interface FeatureToggleService {
    fun isEnabled(toggleId: FeatureToggle, defaultValue: Boolean): Boolean

    companion object {
        fun initialiser(properties: FeatureToggleProperties) = UnleashFeatureToggleService(properties)
    }
}

class UnleashFeatureToggleService(private val properties: FeatureToggleProperties) : FeatureToggleService {

    private val defaultUnleash = DefaultUnleash(
        UnleashConfig.builder()
            .appName(properties.applicationName)
            .unleashAPI(properties.uri)
            .unleashContextProvider(lagUnleashContextProvider())
            .build(),
        ByClusterStrategy(properties.cluster),
        GradualRolloutRandomStrategy()
    )

    private fun lagUnleashContextProvider() = UnleashContextProvider {
        UnleashContext.builder()
            .appName(properties.applicationName)
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

    private val overstyrteBrytere = mutableMapOf<FeatureToggle, Boolean>(
        FellesFeatureToggle.NoOperationToggle to true
    )

    fun settBryter(bryter: FeatureToggle, verdi: Boolean) = overstyrteBrytere.put(bryter, verdi)

    override fun isEnabled(toggleId: FeatureToggle, defaultValue: Boolean) =
        overstyrteBrytere.getOrDefault(toggleId, true)

    companion object {
        private val logger = LoggerFactory.getLogger(FeatureToggleService::class.java)
    }
}