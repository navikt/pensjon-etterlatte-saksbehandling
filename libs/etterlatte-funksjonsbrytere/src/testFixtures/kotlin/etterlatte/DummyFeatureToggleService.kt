package no.nav.etterlatte.funksjonsbrytere

import io.getunleash.UnleashContext

class DummyFeatureToggleService : FeatureToggleService {
    private val overstyrteBrytere =
        mutableMapOf<FeatureToggle, Boolean>(
            FellesFeatureToggle.NoOperationToggle to true,
        )

    fun settBryter(
        bryter: FeatureToggle,
        verdi: Boolean,
    ) = overstyrteBrytere.put(bryter, verdi)

    override fun isEnabled(
        toggleId: FeatureToggle,
        defaultValue: Boolean,
        context: UnleashContext?,
    ) = overstyrteBrytere.getOrDefault(toggleId, defaultValue)
}
