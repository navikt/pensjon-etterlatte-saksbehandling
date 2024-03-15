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

    /**
     * SKal fatte (opphørs-)vedtak etter dette
     */
    VILKAARSVURDERT,

    /**
     * Vedtak er fattet og attestert (og sendt videre for iverksetting)
     */
    VEDTAK_ATTESTERT,
}
