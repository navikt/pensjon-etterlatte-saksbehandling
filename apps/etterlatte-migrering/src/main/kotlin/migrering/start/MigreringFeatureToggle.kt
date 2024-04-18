package no.nav.etterlatte.migrering.start

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle

enum class MigreringFeatureToggle(private val key: String) : FeatureToggle {
    OpphoerSakIPesys("opphoer-sak-i-pesys"),
    ;

    override fun key() = key
}
