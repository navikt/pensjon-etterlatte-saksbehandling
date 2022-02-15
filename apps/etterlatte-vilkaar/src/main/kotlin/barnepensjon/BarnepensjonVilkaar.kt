package no.nav.etterlatte.vilkaar.barnepensjon

import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foedselsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foreldre
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.vilkaar.model.*


fun brukerErUnder20(
    foedselsdato: List<VilkaarOpplysning<Foedselsdato>>,
    doedsdato: List<VilkaarOpplysning<Doedsdato>>
): VurdertVilkaar {
    val foedselsdatoPdl = foedselsdato.find { it.kilde.type == "pdl" }
    val doedsdatoPdl = doedsdato.find { it.kilde.type == "pdl" }

    val vilkaarResultat = if (foedselsdatoPdl == null || doedsdatoPdl == null) {
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    } else {
        val foersteIMndEtterDoedsdato =
            doedsdatoPdl.opplysning.doedsdato.minusDays(doedsdatoPdl.opplysning.doedsdato.dayOfMonth.toLong() - 1)
                .plusMonths(1)
        if (foedselsdatoPdl.opplysning.foedselsdato.plusYears(20) > foersteIMndEtterDoedsdato) {
            VilkaarVurderingsResultat.IKKE_OPPFYLT
        } else {
            VilkaarVurderingsResultat.OPPFYLT
        }
    }

    return VurdertVilkaar("brukerErUnder20", vilkaarResultat, listOf(foedselsdato, doedsdato).flatten())
}


fun doedsfallErRegistrert(
    doedsdato: List<VilkaarOpplysning<Doedsdato>>,
    foreldre: List<VilkaarOpplysning<Foreldre>>
): VurdertVilkaar {
    val doedsdatoPdl = doedsdato.find { it.kilde.type == "pdl" }
    val foreldrePdl = foreldre.find { it.kilde.type == "pdl" }

    val vilkaarResultat = if (doedsdatoPdl == null) {
        VilkaarVurderingsResultat.IKKE_OPPFYLT
    } else if (foreldrePdl == null) {
        VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    } else {
        val foreldreFnr = foreldrePdl.opplysning.foreldre?.map { it.foedselsnummer.value }
        val avdoedErForelder = foreldreFnr?.contains(doedsdatoPdl.opplysning.foedselsnummer)

        if (avdoedErForelder!!) {
            VilkaarVurderingsResultat.OPPFYLT
        } else {
            VilkaarVurderingsResultat.IKKE_OPPFYLT
        }
    }

    return VurdertVilkaar("doedsdatoErRegistrert", vilkaarResultat, listOf(doedsdato, foreldre).flatten())

}


