package vilkaar.barnepensjon

import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraVilkaar
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

internal class UtilsKtTest {

    companion object{
        val oppfyltVilkarDoedsfall = VurdertVilkaar(Vilkaartyper.DOEDSFALL_ER_REGISTRERT, VurderingsResultat.OPPFYLT, emptyList(), LocalDateTime.now())
        val oppfyltVilkarAlder = VurdertVilkaar(Vilkaartyper.SOEKER_ER_UNDER_20, VurderingsResultat.OPPFYLT, emptyList(), LocalDateTime.now())
        val oppfyltVilkarBarnetsMedlemskap = VurdertVilkaar(Vilkaartyper.BARNETS_MEDLEMSKAP, VurderingsResultat.OPPFYLT, emptyList(), LocalDateTime.now())
        val ikkeOppfyltVilkaarBarnetsMedlemskap = VurdertVilkaar(Vilkaartyper.BARNETS_MEDLEMSKAP, VurderingsResultat.IKKE_OPPFYLT, emptyList(), LocalDateTime.now())
        val manglerInfoVilkaarBarnetsMedlemskap = VurdertVilkaar(Vilkaartyper.BARNETS_MEDLEMSKAP, VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, emptyList(), LocalDateTime.now())
        val ikkeOppfyltVilkaarAvdodesMedlemskap = VurdertVilkaar(Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP, VurderingsResultat.IKKE_OPPFYLT, emptyList(), LocalDateTime.now())
        val manglerInfoVilkaarAvdodesMedlemskap = VurdertVilkaar(Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP, VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, emptyList(), LocalDateTime.now())
    }


    @Test
    fun vilkarsvurderingOppfyltOmAlleVilkårOppfylt() {
        assertEquals(VurderingsResultat.OPPFYLT, setVilkaarVurderingFraVilkaar(listOf(oppfyltVilkarAlder, oppfyltVilkarDoedsfall, oppfyltVilkarBarnetsMedlemskap)))
    }

    @Test
    fun vilkarsvurderingIkkeOppfyltOmMinstEtVilkårIkkeOppfylt() {
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, setVilkaarVurderingFraVilkaar(listOf(oppfyltVilkarAlder, oppfyltVilkarDoedsfall, ikkeOppfyltVilkaarBarnetsMedlemskap)))
    }

    @Test
    fun vilkarsvurderingKanIkkeVurderesOmMinstEtVilkårIkkeKanVurderes() {
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, setVilkaarVurderingFraVilkaar(listOf(oppfyltVilkarAlder, oppfyltVilkarDoedsfall, manglerInfoVilkaarBarnetsMedlemskap)))
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, setVilkaarVurderingFraVilkaar(listOf(oppfyltVilkarAlder, oppfyltVilkarDoedsfall, ikkeOppfyltVilkaarAvdodesMedlemskap, manglerInfoVilkaarBarnetsMedlemskap)))
    }

    @Test
    fun vilkaarOmAvdodesMedlemskapSkalIkkeEndreTotalvurdering() {
        assertEquals(VurderingsResultat.OPPFYLT, setVilkaarVurderingFraVilkaar(listOf(oppfyltVilkarAlder, oppfyltVilkarDoedsfall, oppfyltVilkarBarnetsMedlemskap, manglerInfoVilkaarAvdodesMedlemskap)))
        assertEquals(VurderingsResultat.OPPFYLT, setVilkaarVurderingFraVilkaar(listOf(oppfyltVilkarAlder, oppfyltVilkarDoedsfall, oppfyltVilkarBarnetsMedlemskap, ikkeOppfyltVilkaarAvdodesMedlemskap)))
    }
}