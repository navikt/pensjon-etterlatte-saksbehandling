package vilkaarsvurdering.config

enum class VilkaarsvurderingFeatureToggle(val key: String) : no.nav.etterlatte.funksjonsbrytere.FeatureToggle {
    OppdaterGrunnlagsversjon("pensjon-etterlatte.oppdater-grunnlagsversjon-vilkaarsvurdering") {
        override fun key(): String {
            return key
        }
    },
}
