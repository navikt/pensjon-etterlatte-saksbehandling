package no.nav.etterlatte.vilkaar

import no.nav.etterlatte.vilkaar.barnepensjon.brukerErUngNok
import no.nav.etterlatte.vilkaar.model.VilkaarSomBrukerOpplysningVisitor
import no.nav.etterlatte.vilkaar.model.VilkaarVurderingsResultat
import no.nav.etterlatte.vilkaar.model.objectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class VilkaarModelTest {


    @Test
    fun testBrukerSomEr15ErUngNok() {
        val vilkaar = brukerErUngNok
        val opplysning = objectMapper.createObjectNode().put("_navn", "brukers_alder").put("foedselsdato", LocalDate.now().minusYears(15).toString())

        Assertions.assertEquals(VilkaarVurderingsResultat.OPPFYLT, vilkaar.vurder(listOf(opplysning)).resultat[LocalDate.now()])
    }

    @Test
    fun testBrukerSomEr25ErIkkeUngNok() {
        val vilkaar = brukerErUngNok
        val opplysning = objectMapper.createObjectNode().put("_navn", "brukers_alder").put("foedselsdato", LocalDate.now().minusYears(25).toString())

        Assertions.assertEquals(VilkaarVurderingsResultat.IKKE_OPPFYLT, vilkaar.vurder(listOf(opplysning)).resultat[LocalDate.now()])
    }

    @Test
    fun testBrukerSomEr19ogForeldreloesOgIUtdanningErUngNok() {
        val vilkaar = brukerErUngNok
        val opplysning = objectMapper.createObjectNode().put("_navn", "brukers_alder").put("foedselsdato", LocalDate.now().minusYears(19).toString())
        val opplysning2 = objectMapper.createObjectNode().put("_navn", "utdanningsstatus").put("status", true)
        val opplysning3 = objectMapper.createObjectNode().put("_navn", "foreldreloesstatus").put("status", true)

        Assertions.assertEquals(VilkaarVurderingsResultat.OPPFYLT, vilkaar.vurder(listOf(opplysning, opplysning2,opplysning3)).resultat[LocalDate.now()])
    }

    @Test
    fun testBehovvisitor(){
        val visitor = VilkaarSomBrukerOpplysningVisitor("brukers_alder")
        brukerErUngNok.accept(visitor)
        println(visitor.vilkaarSomBrukerOpplysning)
        Assertions.assertEquals(2, visitor.vilkaarSomBrukerOpplysning.size)
    }

}