package no.nav.etterlatte.vilkaar.model

import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning

interface VilkaarVisitor{
    fun visit(vilkaar: Vilkaar)
}

class VilkaarSomBrukerOpplysningVisitor(val opplysningstype: String): VilkaarVisitor{
    val vilkaarSomBrukerOpplysning = mutableListOf<String>()

    override fun visit(vilkaar: Vilkaar) {
        if(vilkaar.opplysningsbehov().contains(opplysningstype)) vilkaarSomBrukerOpplysning.add(vilkaar.navn)
    }
}

interface Vilkaar {
    val navn: String
    fun vurder(opplysninger: List<VilkaarOpplysning<out Any>>): VurdertVilkaar
    fun accept(visitor: VilkaarVisitor) = visitor.visit(this)
    fun opplysningsbehov():List<String>
}