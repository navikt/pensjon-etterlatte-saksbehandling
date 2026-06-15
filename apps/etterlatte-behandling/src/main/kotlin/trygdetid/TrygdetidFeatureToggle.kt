package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle

enum class TrygdetidFeatureToggle(
    private val key: String,
) : FeatureToggle {
    BrukInternTrygdetid("pensjon-etterlatte.bruk-intern-trygdetid"),
    ;

    override fun key() = key
}
