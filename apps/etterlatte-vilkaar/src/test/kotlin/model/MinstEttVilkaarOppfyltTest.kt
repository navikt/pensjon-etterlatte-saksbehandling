package model

import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.vilkaar.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class MinstEttVilkaarOppfyltTest {



    @Test
    fun minstEttOppfylt(){
        val enkeltVilkaar = object: Vilkaar {
            override val navn: String
                get() = "aldri oppfyllt"

            override fun vurder(opplysninger: List<VilkaarOpplysning<out Any>>): VurdertVilkaar {
                return VurdertVilkaar(Tidslinje(LocalDate.MIN to VilkaarVurderingsResultat.OPPFYLT), emptyList(), navn)
            }

            override fun opplysningsbehov(): List<String> {
                return emptyList()
            }

        }

        val komplekstVilkaar = object: Vilkaar {
            override val navn: String
                get() = "ikke alltid oppfyllt"

            override fun vurder(opplysninger: List<VilkaarOpplysning<out Any>>): VurdertVilkaar {
                return VurdertVilkaar(
                    Tidslinje(
                        LocalDate.MIN to VilkaarVurderingsResultat.OPPFYLT,
                        LocalDate.of(2010,1,5) to VilkaarVurderingsResultat.IKKE_OPPFYLT,
                        LocalDate.of(2012,5,6) to VilkaarVurderingsResultat.OPPFYLT,
                        LocalDate.of(2012,5,8) to VilkaarVurderingsResultat.IKKE_OPPFYLT,
                        ), emptyList(), navn)
            }

            override fun opplysningsbehov(): List<String> {
                return emptyList()
            }

        }


        val vurdering = (enkeltVilkaar og komplekstVilkaar).vurder(emptyList())
        assertEquals(VilkaarVurderingsResultat.OPPFYLT, vurdering.resultat[LocalDate.MIN])
        assertEquals(VilkaarVurderingsResultat.IKKE_OPPFYLT, vurdering.resultat[LocalDate.of(2010,1,5)])
        assertEquals(VilkaarVurderingsResultat.OPPFYLT, vurdering.resultat[LocalDate.of(2012,5,6)])
        assertEquals(VilkaarVurderingsResultat.IKKE_OPPFYLT, vurdering.resultat[LocalDate.of(2012,5,8)])


    }

}