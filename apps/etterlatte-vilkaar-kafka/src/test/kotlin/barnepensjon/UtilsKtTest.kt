package barnepensjon

import no.nav.etterlatte.barnepensjon.setVikaarVurderingFraKriterier
import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraVilkaar
import no.nav.etterlatte.barnepensjon.setVurderingFraKommerBarnetTilGode
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
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

        val oppfyltGjenlevendeBarnSammeAdresse = VurdertVilkaar(Vilkaartyper.GJENLEVENDE_OG_BARN_SAMME_BOSTEDADRESSE, VurderingsResultat.OPPFYLT, emptyList(), LocalDateTime.now())
        val ikkeOppfyltGjenlevendeBarnSammeAdresse = VurdertVilkaar(Vilkaartyper.GJENLEVENDE_OG_BARN_SAMME_BOSTEDADRESSE, VurderingsResultat.IKKE_OPPFYLT, emptyList(), LocalDateTime.now())
        val manglerInfoGjenlevendeBarnSammeAdresse = VurdertVilkaar(Vilkaartyper.GJENLEVENDE_OG_BARN_SAMME_BOSTEDADRESSE, VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, emptyList(), LocalDateTime.now())
        val oppfyltBarnIngenUtlandsadresse = VurdertVilkaar(Vilkaartyper.BARN_INGEN_OPPGITT_UTLANDSADRESSE, VurderingsResultat.OPPFYLT, emptyList(), LocalDateTime.now())
        val ikkeOppfyltBarnIngenUtlandsadresse = VurdertVilkaar(Vilkaartyper.BARN_INGEN_OPPGITT_UTLANDSADRESSE, VurderingsResultat.IKKE_OPPFYLT, emptyList(), LocalDateTime.now())
        val oppfyltBarnAvdoedSammeAdresse = VurdertVilkaar(Vilkaartyper.BARN_BOR_PAA_AVDOEDES_ADRESSE, VurderingsResultat.OPPFYLT, emptyList(), LocalDateTime.now())
        val ikkeOppfyltBarnAvdoedSammeAdresse = VurdertVilkaar(Vilkaartyper.BARN_BOR_PAA_AVDOEDES_ADRESSE, VurderingsResultat.IKKE_OPPFYLT, emptyList(), LocalDateTime.now())
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
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, setVilkaarVurderingFraVilkaar(listOf(
            oppfyltVilkarAlder, oppfyltVilkarDoedsfall, manglerInfoVilkaarBarnetsMedlemskap
        )))
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, setVilkaarVurderingFraVilkaar(listOf(
            oppfyltVilkarAlder, oppfyltVilkarDoedsfall, ikkeOppfyltVilkaarAvdodesMedlemskap, manglerInfoVilkaarBarnetsMedlemskap
        )))
    }

    @Test
    fun vilkaarOmAvdodesMedlemskapSkalIkkeEndreTotalvurdering() {
        assertEquals(VurderingsResultat.OPPFYLT, setVilkaarVurderingFraVilkaar(listOf(oppfyltVilkarAlder, oppfyltVilkarDoedsfall, oppfyltVilkarBarnetsMedlemskap, manglerInfoVilkaarAvdodesMedlemskap)))
        assertEquals(VurderingsResultat.OPPFYLT, setVilkaarVurderingFraVilkaar(listOf(oppfyltVilkarAlder, oppfyltVilkarDoedsfall, oppfyltVilkarBarnetsMedlemskap, ikkeOppfyltVilkaarAvdodesMedlemskap)))
    }

    @Test
    fun kommerBarnetTilGodeVurderingOppfylt() {
        assertEquals(VurderingsResultat.OPPFYLT, setVurderingFraKommerBarnetTilGode(listOf(
            oppfyltGjenlevendeBarnSammeAdresse, oppfyltBarnIngenUtlandsadresse, ikkeOppfyltBarnAvdoedSammeAdresse
        )))
    }

    @Test
    fun kommerBarnetTilGodeVurderingIkkeOppfylt() {
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, setVurderingFraKommerBarnetTilGode(listOf(
            ikkeOppfyltGjenlevendeBarnSammeAdresse, oppfyltBarnIngenUtlandsadresse, ikkeOppfyltBarnAvdoedSammeAdresse
        )))
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, setVurderingFraKommerBarnetTilGode(listOf(
            ikkeOppfyltGjenlevendeBarnSammeAdresse, oppfyltBarnIngenUtlandsadresse, oppfyltBarnAvdoedSammeAdresse
        )))
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, setVurderingFraKommerBarnetTilGode(listOf(
            oppfyltGjenlevendeBarnSammeAdresse, ikkeOppfyltBarnIngenUtlandsadresse, ikkeOppfyltBarnAvdoedSammeAdresse
        )))
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, setVurderingFraKommerBarnetTilGode(listOf(
            ikkeOppfyltBarnAvdoedSammeAdresse, ikkeOppfyltBarnIngenUtlandsadresse, oppfyltBarnAvdoedSammeAdresse
        )))
    }

    @Test
    fun kommerBarnetTilGodeVurderingManglerInfo() {
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, setVurderingFraKommerBarnetTilGode(listOf(
            manglerInfoGjenlevendeBarnSammeAdresse, oppfyltBarnIngenUtlandsadresse, ikkeOppfyltBarnAvdoedSammeAdresse
        )))
    }

    @Test
    fun vurderVilkaarsVurdering() {
        val kriterieOppfylt =
            Kriterie(Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO, VurderingsResultat.OPPFYLT, listOf())
        val kriterieIkkeOppfylt =
            Kriterie(
                Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
                VurderingsResultat.IKKE_OPPFYLT,
                listOf()
            )
        val kriterieKanIkkeVurdere = Kriterie(
            Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            listOf()
        )

        val vilkaarKriterierOppfylt = setVikaarVurderingFraKriterier(listOf(kriterieOppfylt, kriterieOppfylt))
        val vilkaarEtKriterieIkkeOppfylt =
            setVikaarVurderingFraKriterier(listOf(kriterieOppfylt, kriterieIkkeOppfylt, kriterieKanIkkeVurdere))
        val vilkaarKriterierOppfyltOgKanIkkeHentesUt =
            setVikaarVurderingFraKriterier(listOf(kriterieOppfylt, kriterieKanIkkeVurdere, kriterieOppfylt))

        assertEquals(VurderingsResultat.OPPFYLT, vilkaarKriterierOppfylt)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, vilkaarEtKriterieIkkeOppfylt)
        assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            vilkaarKriterierOppfyltOgKanIkkeHentesUt
        )
    }

}