package no.nav.etterlatte.libs.common.vedtak

enum class VedtakType(
    val vanligBehandling: Boolean,
    val tilgjengeligEksternt: Boolean,
) {
    INNVILGELSE(true, true),
    OPPHOER(true, true),
    AVSLAG(true, true),
    ENDRING(true, true),
    TILBAKEKREVING(false, false),
    AVVIST_KLAGE(false, false),
    INGEN_ENDRING(true, false),
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
