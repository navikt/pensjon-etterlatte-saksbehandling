package no.nav.etterlatte.rapidsandrivers.migrering

object Migreringshendelser {
    private const val PREFIX = "MIGRERING:"

    const val START_MIGRERING = "${PREFIX}START_MIGRERING"
    const val MIGRER_SPESIFIKK_SAK = "${PREFIX}MIGRER_SPESIFIKK_SAK"

    const val MIGRER_SAK = "${PREFIX}MIGRER_SAK"
    const val VERIFISER = "${PREFIX}VERIFISERING"
    const val LAGRE_KOPLING = "${PREFIX}LAGRE_KOPLING"
    const val LAGRE_GRUNNLAG = "${PREFIX}LAGRE_GRUNNLAG"
    const val VILKAARSVURDER = "${PREFIX}VILKAARSVURDER"
    const val BEREGN = "${PREFIX}BEREGN"
    const val TRYGDETID = "${PREFIX}TRYGDETID"
    const val VEDTAK = "${PREFIX}VEDTAK"
    const val PAUSE = "${PREFIX}PAUSE"
    const val IVERKSATT = "${PREFIX}IVERKSATT"
    const val AVBRYT_BEHANDLING = "${PREFIX}AVBRYT_BEHANDLING"
    const val FIKS_ENKELTBREV = "${PREFIX}FIKS_ENKELTBREV"
}
