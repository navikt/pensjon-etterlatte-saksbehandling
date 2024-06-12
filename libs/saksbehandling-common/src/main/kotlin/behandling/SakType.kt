package no.nav.etterlatte.libs.common.behandling

enum class SakType(
    val tema: String,
    val behandlingsnummer: String,
) {
    BARNEPENSJON("EYB", "B359"),
    OMSTILLINGSSTOENAD("EYO", "B373"),
}
