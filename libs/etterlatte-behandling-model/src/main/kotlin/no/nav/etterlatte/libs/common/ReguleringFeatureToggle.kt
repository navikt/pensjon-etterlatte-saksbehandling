package no.nav.etterlatte.libs.common

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle

enum class ReguleringFeatureToggle(private val key: String) : FeatureToggle {
    START_REGULERING("start-regulering"),
    ;

    override fun key() = key
}
