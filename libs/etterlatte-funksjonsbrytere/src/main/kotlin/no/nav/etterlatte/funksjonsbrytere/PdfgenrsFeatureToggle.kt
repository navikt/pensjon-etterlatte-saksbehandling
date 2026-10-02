package no.nav.etterlatte.funksjonsbrytere

enum class PdfgenrsFeatureToggle(
    val mal: String,
) : FeatureToggle {
    BARNEPENSJON("barnepensjon_v2"),
    OMSTILLINGSSTOENAD("omstillingsstoenad_v1"),
    OMS_ENDRINGER("oms_meldt_inn_endring_v1"),
    NOTAT("tom_mal"),
    KLAGE_BLANKETT("klage_oversendelse_blankett"),
    ;

    override fun key() = "pdfgenrs-$mal"

    companion object {
        fun brukPdfgenrs(
            mal: String,
            featureToggleService: FeatureToggleService,
        ): Boolean = entries.firstOrNull { it.mal == mal }?.let { featureToggleService.isEnabled(it, false) } ?: false
    }
}
