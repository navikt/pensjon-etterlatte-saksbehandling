package no.nav.etterlatte.samordning

/**
 * Definisjon ekstern hendelse (fra SAM)
 */
class SamordningVedtakHendelse {
    var fagomrade: String? = null
    var artTypeKode: String? = null
    var vedtakId: Long? = null

    override fun toString(): String = "SamordningVedtakHendelse[fagomrade=$fagomrade, artTypeKode=$artTypeKode, vedtakId=$vedtakId]"
}

const val FAGOMRADE_OMS = "EYO"
const val SAKSTYPE_OMS = "OMS"
