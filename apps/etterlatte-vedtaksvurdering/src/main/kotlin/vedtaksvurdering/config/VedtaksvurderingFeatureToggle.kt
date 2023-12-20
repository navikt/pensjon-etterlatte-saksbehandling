package no.nav.etterlatte.vedtaksvurdering.config

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle

enum class VedtaksvurderingFeatureToggle(private val key: String) : FeatureToggle {
    ValiderGrunnlagsversjon("pensjon-etterlatte.valider-grunnlagsversjon-ved-fatt-vedtak"),
    ;

    override fun key() = key
}
