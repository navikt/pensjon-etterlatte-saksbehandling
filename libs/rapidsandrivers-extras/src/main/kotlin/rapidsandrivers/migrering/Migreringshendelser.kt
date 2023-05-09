package no.nav.etterlatte.rapidsandrivers.migrering

object Migreringshendelser {

    private const val PREFIX = "MIGRERING:"

    const val START_MIGRERING = "${PREFIX}START_MIGRERING"
    const val MIGRER_SAK = "${PREFIX}MIGRER_SAK"
    const val GRUNNLAG = "${PREFIX}GRUNNLAG"
    const val VILKAARSVURDER = "${PREFIX}VILKAARSVURDER"
    const val BEREGN = "${PREFIX}BEREGN"
    const val TRYGDETID = "${PREFIX}TRYGDETID"
}