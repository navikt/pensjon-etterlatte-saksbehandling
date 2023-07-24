package no.nav.etterlatte.rapidsandrivers.migrering

object Migreringshendelser {

    private const val PREFIX = "MIGRERING:"

    const val START_MIGRERING = "${PREFIX}START_MIGRERING"
    const val MIGRER_SAK = "${PREFIX}MIGRER_SAK"
    const val BEHANDLING_OPPRETTET = "${PREFIX}BEHANDLING_OPPRETTET"
    const val VILKAARSVURDER = "${PREFIX}VILKAARSVURDER"
    const val BEREGN = "${PREFIX}BEREGN"
    const val TRYGDETID = "${PREFIX}TRYGDETID"
    const val TRYGDETID_GRUNNLAG = "${PREFIX}TRYGDETID_GRUNNLAG"
    const val VEDTAK = "${PREFIX}VEDTAK"
}