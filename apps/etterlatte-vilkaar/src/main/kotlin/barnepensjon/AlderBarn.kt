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
                KriterieOpplysningsType.DOEDSDATO,
                avdoedPdl.kilde,
                Doedsdato(avdoedPdl.opplysning.doedsdato, avdoedPdl.opplysning.foedselsnummer)
            )
        },
        soekerPdl?.let {
            Kriteriegrunnlag(
                KriterieOpplysningsType.FOEDSELSDATO,
                soekerPdl.kilde,
                Foedselsdato(soekerPdl.opplysning.foedselsdato, soekerPdl.opplysning.foedselsnummer)
            )
        }
    )

    val resultat = if (soekerPdl == null || avdoedPdl == null) {
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    } else {
        vurderOpplysning { hentFoedselsdato(soekerPdl).plusYears(18) > hentVirkningsdato(avdoedPdl) }
    }

    return Kriterie(
            Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
            resultat,
            opplysningsGrunnlag
        )
}

