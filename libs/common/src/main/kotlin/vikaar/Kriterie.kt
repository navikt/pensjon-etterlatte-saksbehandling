package no.nav.etterlatte.libs.common.vikaar

import no.nav.etterlatte.libs.common.inntekt.PensjonUforeOpplysning

data class Kriterie(
    val navn: Kriterietyper,
    val resultat: VurderingsResultat,
    val basertPaaOpplysninger: List<Kriteriegrunnlag<out Any>>
)