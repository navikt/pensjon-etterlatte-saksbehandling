package no.nav.etterlatte.barnepensjon

import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foedselsdato
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar


fun vilkaarBrukerErUnder20(
    vilkaartype: Vilkaartyper,
    foedselsdato: List<VilkaarOpplysning<Foedselsdato>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>,
): VurdertVilkaar {
    val soekerErUnder20 = kriterieSoekerErUnder20(foedselsdato, doedsdato)

    return VurdertVilkaar(
        vilkaartype,
        setVikaarVurderingsResultat(listOf(soekerErUnder20)),
        listOf(soekerErUnder20)
    )
}

fun kriterieSoekerErUnder20(
    foedselsdato: List<VilkaarOpplysning<Foedselsdato>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>
): Kriterie {
    val opplysningsGrunnlag = listOf(foedselsdato, doedsdato).flatten().filter { it.kilde.type == "pdl" }
    val resultat =
        vurderOpplysning { hentSoekerFoedselsdato(foedselsdato).plusYears(20) > hentVirkningsdato(doedsdato) }
    return Kriterie(Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO, resultat, opplysningsGrunnlag)
}

