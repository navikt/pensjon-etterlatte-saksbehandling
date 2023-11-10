package no.nav.etterlatte.samordning

/**
 * Definisjon ekstern hendelse (fra SAM)
 */
data class SamordningVedtakHendelse(
    val fagomrade: String,
    val artTypeKode: String,
    val vedtakId: Long,
)

const val FAGOMRADE_OMS = "EYO"
const val SAKSTYPE_OMS = "OMS"
