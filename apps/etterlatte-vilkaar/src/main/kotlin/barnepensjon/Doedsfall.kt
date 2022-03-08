package no.nav.etterlatte.barnepensjon

import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foreldre
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarVurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar



fun vilkaarDoedsfallErRegistrert(
    vilkaartype: Vilkaartyper,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>,
    foreldre: List<VilkaarOpplysning<Foreldre>>,
): VurdertVilkaar {
    val doedsdatoRegistrertIPdl = kriterieDoedsdatoRegistrertIPdl(doedsdato)
    val avdoedErForeldre = kriterieAvdoedErForelder(foreldre, doedsdato)

    return VurdertVilkaar(
        vilkaartype,
        setVikaarVurderingsResultat(listOf(doedsdatoRegistrertIPdl, avdoedErForeldre)),
        listOf(doedsdatoRegistrertIPdl, avdoedErForeldre)
    )
}

fun kriterieDoedsdatoRegistrertIPdl(doedsdato: List<VilkaarOpplysning<Doedsdato>>): Kriterie {
    val resultat = try {
        hentDoedsdato(doedsdato)
        VilkaarVurderingsResultat.OPPFYLT
    } catch (ex: OpplysningKanIkkeHentesUt) {
        VilkaarVurderingsResultat.IKKE_OPPFYLT
    }

    return Kriterie(
        Kriterietyper.DOEDSFALL_ER_REGISTRERT_I_PDL,
        resultat,
        listOf(doedsdato).flatten().filter { it.kilde.type == "pdl" })
}

fun kriterieAvdoedErForelder(
    foreldre: List<VilkaarOpplysning<Foreldre>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>
): Kriterie {
    val opplsyningsGrunnlag = listOf(foreldre, doedsdato).flatten().filter { it.kilde.type == "pdl" }
    val resultat = vurderOpplysning { hentFnrForeldre(foreldre).contains(hentDoedsdato(doedsdato).foedselsnummer) }

    return Kriterie(Kriterietyper.AVDOED_ER_FORELDER, resultat, opplsyningsGrunnlag)
}




