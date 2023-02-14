package no.nav.etterlatte.featureToggle

import java.net.URI

data class Unleash(
    val enabled: Boolean,
    val uri: URI,
    val cluster: String,
    val applicationName: String
)