package no.nav.etterlatte.vilkaar.barnepensjon

import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foedselsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foreldre
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarVurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters


fun brukerErUnder20(
    vilkaartype: String,
    foedselsdato: List<VilkaarOpplysning<Foedselsdato>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>,
): VurdertVilkaar {
    return VurdertVilkaar(
        vilkaartype,
        vurderOpplysning { hentSoekerFoedselsdato(foedselsdato).plusYears(20) > hentVirkningsdato(doedsdato) },
        listOf(foedselsdato, doedsdato).flatten()
    )
}

fun doedsfallErRegistrert(
    vilkaartype: String,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>,
    foreldre: List<VilkaarOpplysning<Foreldre>>,
): VurdertVilkaar {
    try {
        hentDoedsdato(doedsdato)
    } catch (ex: OpplysningKanIkkeHentesUt) {
        return VurdertVilkaar(
            vilkaartype,
            VilkaarVurderingsResultat.IKKE_OPPFYLT,
            listOf(doedsdato, foreldre).flatten()
        )
    }

    return VurdertVilkaar(vilkaartype,
        vurderOpplysning { hentFnrForeldre(foreldre).contains(hentDoedsdato(doedsdato).foedselsnummer) },
        listOf(doedsdato, foreldre).flatten())
}


fun vurderOpplysning(vurdering: () -> Boolean): VilkaarVurderingsResultat = try {
    if (vurdering()) VilkaarVurderingsResultat.OPPFYLT else VilkaarVurderingsResultat.IKKE_OPPFYLT
} catch (ex: OpplysningKanIkkeHentesUt) {
    VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
}

fun hentFnrForeldre(foreldre: List<VilkaarOpplysning<Foreldre>>): List<String> {
    return foreldre.find { it.kilde.type == "pdl" }?.opplysning?.foreldre?.map { it.foedselsnummer.value }
        ?: throw OpplysningKanIkkeHentesUt()
}

fun hentSoekerFoedselsdato(foedselsdato: List<VilkaarOpplysning<Foedselsdato>>): LocalDate {
    return foedselsdato.find { it.kilde.type == "pdl" }?.opplysning?.foedselsdato
        ?: throw OpplysningKanIkkeHentesUt()
}

fun hentDoedsdato(doedsdato: List<VilkaarOpplysning<Doedsdato>>): Doedsdato {
    return doedsdato.find { it.kilde.type == "pdl" }?.opplysning
        ?: throw OpplysningKanIkkeHentesUt()
}

fun hentVirkningsdato(doedsdato: List<VilkaarOpplysning<Doedsdato>>): LocalDate {
    val doedsdato = doedsdato.find { it.kilde.type == "pdl" }?.opplysning?.doedsdato
    return doedsdato?.with(TemporalAdjusters.firstDayOfNextMonth()) ?: throw OpplysningKanIkkeHentesUt()
}

class OpplysningKanIkkeHentesUt : IllegalStateException()




