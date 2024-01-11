package no.nav.etterlatte.vedtaksvurdering.config

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle

enum class VedtaksvurderingFeatureToggle(private val key: String) : FeatureToggle {
    BrukTrygdetidIValiderGrunnlagsversjon("pensjon-etterlatte.valider-grunnlagsversjon-bruk-trygdetid"),
    ;

    override fun key() = key
}
