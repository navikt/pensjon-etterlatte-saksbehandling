package no.nav.etterlatte.libs.common.vikaar

data class Kriterie(
    val navn: Kriterietyper,
    val resultat: VurderingsResultat,
    val basertPaaOpplysninger: List<Kriteriegrunnlag<out Any>>
)

data class Metakriterie(
    val navn: Metakriterietyper,
    val resultat: VurderingsResultat,
    val utfall: Utfall?,
    val kriterie: List<Kriterie>
)