package no.nav.etterlatte.regulering

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle

enum class ReguleringFeatureToggle(
    private val key: String,
) : FeatureToggle {
    SkalStoppeEtterFattetVedtak("omregning-skal-stoppe-etter-fattet-vedtak"),
    ;

    override fun key(): String = key
}
