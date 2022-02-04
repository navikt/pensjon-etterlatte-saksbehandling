package no.nav.etterlatte.vilkaar.barnepensjon

import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato as DoedsDatoModell
import no.nav.etterlatte.vilkaar.model.*
import java.time.LocalDate

val BrukerErUnder18 = BrukersAlder.enkelVurdering("Bruker er under 18 år") {
    Tidslinje(foedselsdato to VilkaarVurderingsResultat.OPPFYLT, foedselsdato.plusYears(18) to VilkaarVurderingsResultat.IKKE_OPPFYLT)
}

val BrukerErUnder20 = BrukersAlder.enkelVurdering("Bruker er under 20 år",) {
    Tidslinje(foedselsdato to VilkaarVurderingsResultat.OPPFYLT, foedselsdato.plusYears(20) to VilkaarVurderingsResultat.IKKE_OPPFYLT)
}

val BrukerErIUtdanning = Utdanningsgrunnlag.enkelVurdering("Bruker er i utdanning") {
    val vurdering = if(status) VilkaarVurderingsResultat.OPPFYLT else VilkaarVurderingsResultat.IKKE_OPPFYLT
    Tidslinje(LocalDate.MIN to vurdering)
}
val BrukerErForeldreloes = ForeldreloesGrunnlag.enkelVurdering("Bruker er foreldreløs") {
    val vurdering = status
        .takeIf { it }?.let { VilkaarVurderingsResultat.OPPFYLT }
        ?: VilkaarVurderingsResultat.IKKE_OPPFYLT

    Tidslinje(LocalDate.MIN to vurdering)
}

val brukerErUnder20aarIUtdanningOgForeldreloes = BrukerErUnder20 og BrukerErIUtdanning og BrukerErForeldreloes

val brukerErUngNok = BrukerErUnder18 eller brukerErUnder20aarIUtdanningOgForeldreloes

val sammenligningAvOpplysninger = Doedsdato.enkelSammenligning("sammenlign dødsdato"){
    Tidslinje(LocalDate.MIN to (groupBy{it.doedsdato}.size.takeIf { it < 2 }?.let { VilkaarVurderingsResultat.OPPFYLT } ?: VilkaarVurderingsResultat.IKKE_OPPFYLT))
}


inline fun <reified T>OpplysningType<T>.enkelVurdering(vilkarsNavn:String, crossinline test: T.() -> Tidslinje<VilkaarVurderingsResultat>) = enkelVurderingAvOpplysning(vilkarsNavn, opplysningsNavn, test)
inline fun <reified T>OpplysningType<T>.enkelSammenligning(vilkarsNavn:String, crossinline test: List<T>.() -> Tidslinje<VilkaarVurderingsResultat>) = enkelSammenligningAvOpplysninger(vilkarsNavn, opplysningsNavn, test)

data class Utdanningsgrunnlag(val status: Boolean){
    companion object: OpplysningType<Utdanningsgrunnlag> {
        override val opplysningsNavn: String
            get() = "utdanningsstatus"
    }
}

data class ForeldreloesGrunnlag(val status: Boolean){
    companion object: OpplysningType<ForeldreloesGrunnlag> {
        override val opplysningsNavn: String
            get() = "foreldreloesstatus"
    }
}

data class BrukersAlder(
    val foedselsdato: LocalDate
    ){
    companion object:OpplysningType<BrukersAlder>{
        override val opplysningsNavn: String
            get() = "brukers_alder"

    }
}

object Doedsdato:OpplysningType<DoedsDatoModell>{
    override val opplysningsNavn: String
        get() = "doedsfall:v1"

}