package no.nav.etterlatte.vilkaar.barnepensjon

import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foedselsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foreldre
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.vilkaar.model.*
import java.time.LocalDate


fun brukerErUnder20(opplysninger: List<VilkaarOpplysning<Foedselsdato>>): VurdertVilkaar {
    val opplysningPdl = opplysninger.find { it.kilde.type == "pdl" }
        ?: return VurdertVilkaar(Opplysningstyper.SOEKER_FOEDSELSDATO_V1.value, VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, opplysninger)

    return if (opplysningPdl.opplysning.foedselsdato.plusYears(20) < LocalDate.now()) {
        VurdertVilkaar(opplysningPdl.opplysingType, VilkaarVurderingsResultat.OPPFYLT, opplysninger )
    } else {
        VurdertVilkaar(opplysningPdl.opplysingType, VilkaarVurderingsResultat.IKKE_OPPFYLT, opplysninger, )
    }

}


fun doedsfallErRegistrert(doedsdato: List<VilkaarOpplysning<Doedsdato>>, foreldre: List<VilkaarOpplysning<Foreldre>> ): VurdertVilkaar {
    val doedsdatoPdl = doedsdato.find {it.kilde.type == "pdl"}
        ?: return VurdertVilkaar(Opplysningstyper.SOEKER_RELASJON_FORELDRE_V1.value, VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, List(doedsdato, foreldre))

}
