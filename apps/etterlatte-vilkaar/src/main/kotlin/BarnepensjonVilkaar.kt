package no.nav.etterlatte.vilkaar

val BrukerErUnder18 = EnkelSjekkAvOpplysning("Bruker er under 18 år","brukers_alder") {
    get("alder")?.intValue()
        ?.takeIf { it < 18 }?.let { VilkaarVurderingsResultat.OPPFYLT }
        ?: VilkaarVurderingsResultat.IKKE_OPPFYLT
}

val BrukerErUnder20 = EnkelSjekkAvOpplysning("Bruker er under 20 år","brukers_alder") {
    get("alder")?.intValue()
        ?.takeIf { it < 20 }?.let { VilkaarVurderingsResultat.OPPFYLT }
        ?: VilkaarVurderingsResultat.IKKE_OPPFYLT
}

val BrukerErIUtdanning = EnkelSjekkAvOpplysning("Bruker er i utdanning","utdanningsstatus") {
    get("status")?.booleanValue()
        ?.takeIf { it }?.let { VilkaarVurderingsResultat.OPPFYLT }
        ?: VilkaarVurderingsResultat.IKKE_OPPFYLT
}
val BrukerErForeldreloes = EnkelSjekkAvOpplysning("Bruker er foreldreløs","foreldreloesstatus") {
    get("status")?.booleanValue()
        ?.takeIf { it }?.let { VilkaarVurderingsResultat.OPPFYLT }
        ?: VilkaarVurderingsResultat.IKKE_OPPFYLT
}


val brukerErUnder20aarIUtdanningOgForeldreloes = BrukerErUnder20 og BrukerErIUtdanning og BrukerErForeldreloes

val brukerErUngNok = BrukerErUnder18 eller brukerErUnder20aarIUtdanningOgForeldreloes