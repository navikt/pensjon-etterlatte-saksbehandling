package no.nav.etterlatte.libs.common.vedtak

enum class VedtakType {
    INNVILGELSE,
    OPPHOER,
    AVSLAG,
    ENDRING,
    TILBAKEKREVING,
    AVVIST_KLAGE,
    ;

    fun tilLesbarString() =
        when (this) {
            INNVILGELSE -> "Innvilgelse"
            OPPHOER -> "Opphør"
            AVSLAG -> "Avslag"
            ENDRING -> "Endring"
            TILBAKEKREVING -> "Tilbakekreving"
            AVVIST_KLAGE -> "Avvist klage"
        }
}
