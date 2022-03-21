package no.nav.etterlatte.barnepensjon

import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.*
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Foedselsdato


fun vilkaarBrukerErUnder20(
    vilkaartype: Vilkaartyper,
    soekerPdl: VilkaarOpplysning<Person>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
): VurdertVilkaar {
    val soekerErUnder20 = kriterieSoekerErUnder20(soekerPdl, avdoedPdl)

    return VurdertVilkaar(
        vilkaartype,
        setVikaarVurderingsResultat(listOf(soekerErUnder20)),
        listOf(soekerErUnder20)
    )
}

fun kriterieSoekerErUnder20(
    soekerPdl: VilkaarOpplysning<Person>?,
    avdoedPdl: VilkaarOpplysning<Person>?
): Kriterie {
    val opplysningsGrunnlag = listOfNotNull(
        avdoedPdl?.let {
            Kriteriegrunnlag(
                avdoedPdl.kilde,
                Doedsdato(avdoedPdl.opplysning.doedsdato, avdoedPdl.opplysning.foedselsnummer)
            )
        },
        soekerPdl?.let {
            Kriteriegrunnlag(
                soekerPdl.kilde,
                Foedselsdato(soekerPdl.opplysning.foedselsdato, soekerPdl.opplysning.foedselsnummer)
            )
        }
    )
    return if(opplysningsGrunnlag.size < 2) {
        Kriterie(Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO, VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, emptyList())
    } else {
        val resultat =
            vurderOpplysning { hentFoedselsdato(soekerPdl!!).plusYears(20) > hentVirkningsdato(avdoedPdl) }
        Kriterie(Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO, resultat, opplysningsGrunnlag)
    }


}

