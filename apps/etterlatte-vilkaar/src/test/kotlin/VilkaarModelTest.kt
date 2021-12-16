package no.nav.etterlatte.vilkaar

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class VilkaarModelTest {


    @Test
    fun testBrukerSomEr15ErUngNok() {
        val vilkaar = brukerErUngNok
        val opplysning = objectMapper.createObjectNode().put("navn", "brukers_alder").put("alder", 15)

        Assertions.assertEquals(VilkaarVurderingsResultat.OPPFYLT, vilkaar.vurder(listOf(opplysning)).resultat)
    }

    @Test
    fun testBrukerSomEr25ErIkkeUngNok() {
        val vilkaar = brukerErUngNok
        val opplysning = objectMapper.createObjectNode().put("navn", "brukers_alder").put("alder", 25)

        Assertions.assertEquals(VilkaarVurderingsResultat.IKKE_OPPFYLT, vilkaar.vurder(listOf(opplysning)).resultat)
    }

    @Test
    fun testBrukerSomEr19ogForeldreloesOgIUtdanningErUngNok() {
        val vilkaar = brukerErUngNok
        val opplysning = objectMapper.createObjectNode().put("navn", "brukers_alder").put("alder", 19)
        val opplysning2 = objectMapper.createObjectNode().put("navn", "utdanningsstatus").put("status", true)
        val opplysning3 = objectMapper.createObjectNode().put("navn", "foreldreloesstatus").put("status", true)

        Assertions.assertEquals(VilkaarVurderingsResultat.OPPFYLT, vilkaar.vurder(listOf(opplysning, opplysning2,opplysning3)).resultat)
    }

    @Test
    fun testBehovvisitor(){
        val visitor = VilkaarSomBrukerOpplysningVisitor("brukers_alder")
        brukerErUngNok.accept(visitor)
        println(visitor.vilkaarSomBrukerOpplysning)
        Assertions.assertEquals(2, visitor.vilkaarSomBrukerOpplysning.size)
    }

}