package no.nav.etterlatte.libs.common.vedtak

enum class VedtakAldersovergangStepEvents {
    /**
     * Skal ageres på
     */
    IDENTIFISERT_SAK,

    /**
     *  Etter at løpende ytelser er vurdert for sakId fra #SAK_IDENTIFISERT
     */
    VURDERT_LOEPENDE_YTELSE,
}
