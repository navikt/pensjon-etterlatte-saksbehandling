package no.nav.etterlatte.libs.common.vedtak

enum class VedtakType(
    val vanligBehandling: Boolean,
) {
    INNVILGELSE(true),
    OPPHOER(true),
    AVSLAG(true),
    ENDRING(true),
    TILBAKEKREVING(false),
    AVVIST_KLAGE(false),
    INGEN_ENDRING(true),
    ;

    fun tilLesbarString() =
        when (this) {
            INNVILGELSE -> "Innvilgelse"
            OPPHOER -> "Opphør"
            AVSLAG -> "Avslag"
            ENDRING -> "Endring"
            TILBAKEKREVING -> "Tilbakekreving"
            AVVIST_KLAGE -> "Avvist klage"
            INGEN_ENDRING -> "Ingen endring"
        }
}
