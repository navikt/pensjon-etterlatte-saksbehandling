package no.nav.etterlatte.libs.common.vikaar

data class Kriterie(
    val navn: Kriterietyper,
    val resultat: VurderingsResultat,
    val basertPaaOpplysninger: List<Kriteriegrunnlag<out Any>>
)