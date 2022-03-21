package no.nav.etterlatte.barnepensjon

import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarVurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Foreldre


fun vilkaarDoedsfallErRegistrert(
    vilkaartype: Vilkaartyper,
    avdoed: VilkaarOpplysning<Person>?,
    soeker: VilkaarOpplysning<Person>?,
): VurdertVilkaar {
    val doedsdatoRegistrertIPdl = kriterieDoedsdatoRegistrertIPdl(avdoed)
    val avdoedErForeldre = kriterieAvdoedErForelder(soeker, avdoed)

    return VurdertVilkaar(
        vilkaartype,
        setVikaarVurderingsResultat(listOf(doedsdatoRegistrertIPdl, avdoedErForeldre)),
        listOf(doedsdatoRegistrertIPdl, avdoedErForeldre)
    )
}

fun kriterieDoedsdatoRegistrertIPdl(avdoed: VilkaarOpplysning<Person>?): Kriterie {
    return avdoed?.let {
        val resultat = try {
            hentDoedsdato(avdoed)
            VilkaarVurderingsResultat.OPPFYLT
        } catch (ex: OpplysningKanIkkeHentesUt) {
            VilkaarVurderingsResultat.IKKE_OPPFYLT
        }
        Kriterie(
            Kriterietyper.DOEDSFALL_ER_REGISTRERT_I_PDL,
            resultat,
            listOf(Kriteriegrunnlag(avdoed.kilde, Doedsdato(avdoed.opplysning.doedsdato, avdoed.opplysning.foedselsnummer)))
        )
    } ?: Kriterie(
        Kriterietyper.DOEDSFALL_ER_REGISTRERT_I_PDL,
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
        emptyList())

}

fun kriterieAvdoedErForelder(
    soeker: VilkaarOpplysning<Person>?,
    avdoed: VilkaarOpplysning<Person>?,
): Kriterie {
    if(soeker == null || avdoed == null)
        return Kriterie(Kriterietyper.AVDOED_ER_FORELDER, VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, emptyList())
    val opplsyningsGrunnlag = listOf(
        Kriteriegrunnlag(soeker.kilde, Foreldre(soeker.opplysning.familieRelasjon?.foreldre)),
        Kriteriegrunnlag(avdoed.kilde, Doedsdato(avdoed.opplysning.doedsdato, avdoed.opplysning.foedselsnummer))
    )

    val resultat = vurderOpplysning { hentFnrForeldre(soeker).contains(avdoed.opplysning.foedselsnummer) }

    return Kriterie(Kriterietyper.AVDOED_ER_FORELDER, resultat, opplsyningsGrunnlag)
}




