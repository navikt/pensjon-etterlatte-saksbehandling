package fordeler

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle

enum class FordelerFeatureToggle(private val key: String) : FeatureToggle {
    TillatAlleScenarier("fordeler.tillat-alle-scenarier"),
    ;

    override fun key() = key
}
