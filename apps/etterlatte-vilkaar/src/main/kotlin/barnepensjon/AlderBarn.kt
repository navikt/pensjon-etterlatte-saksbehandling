package no.nav.etterlatte.barnepensjon

import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Foedselsdato


fun vilkaarBrukerErUnder20(
    vilkaartype: Vilkaartyper,
    soekerPdl: VilkaarOpplysning<Person>,
    avdoedPdl: VilkaarOpplysning<Person>,
): VurdertVilkaar {
    val soekerErUnder20 = kriterieSoekerErUnder20(soekerPdl, avdoedPdl)

    return VurdertVilkaar(
        vilkaartype,
        setVikaarVurderingsResultat(listOf(soekerErUnder20)),
        listOf(soekerErUnder20)
    )
}

fun kriterieSoekerErUnder20(
    soekerPdl: VilkaarOpplysning<Person>,
    avdoedPdl: VilkaarOpplysning<Person>
): Kriterie {
    val opplysningsGrunnlag = listOf(
        Kriteriegrunnlag(avdoedPdl.kilde, Doedsdato(avdoedPdl.opplysning.doedsdato, avdoedPdl.opplysning.foedselsnummer)),
        Kriteriegrunnlag(soekerPdl.kilde, Foedselsdato(soekerPdl.opplysning.foedselsdato, soekerPdl.opplysning.foedselsnummer))
    )

    val resultat =
        vurderOpplysning { hentFoedselsdato(soekerPdl).plusYears(20) > hentVirkningsdato(avdoedPdl) }
    return Kriterie(Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO, resultat, opplysningsGrunnlag)
}

