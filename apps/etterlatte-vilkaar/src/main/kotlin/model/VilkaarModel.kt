package no.nav.etterlatte.vilkaar.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import java.time.LocalDate

val objectMapper: ObjectMapper = JsonMapper.builder()
    .addModule(JavaTimeModule())
    .addModule(KotlinModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
    .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
    .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
    .build()

infix fun Vilkaar.og(other: Vilkaar) = AlleVilkaarOppfylt(listOf(this, other))
infix fun Vilkaar.eller(other: Vilkaar) = MinstEttVilkaarOppfylt(listOf(this, other))
infix fun AlleVilkaarOppfylt.og(other:Vilkaar) = AlleVilkaarOppfylt(vilkaar + other)

class AlleVilkaarOppfylt(val vilkaar: List<Vilkaar>) : Vilkaar {
    override fun accept(visitor: VilkaarVisitor) {
        visitor.visit(this)
        vilkaar.forEach { it.accept(visitor) }
    }

    override fun opplysningsbehov(): List<String> = emptyList()

    override val navn: String
        get() = vilkaar.joinToString(separator = " og ") { it.navn }
    override fun vurder(opplysninger: List<Opplysning>): VurdertVilkaar {
        return vilkaar
            .map { it.vurder(opplysninger)}
            .let {
                val ny_tidslinje = it.map { it.resultat }
                    .reduce{ acc, cur ->
                        (acc + cur).map {_, it ->
                            (it.first?: VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING) * (it.second?: VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING)
                        }
                    }.normaliser()
                VurdertVilkaar(ny_tidslinje  , it.map { objectMapper.valueToTree(it.serialize()) }, navn) }
    }
}

class MinstEttVilkaarOppfylt(val vilkaar: List<Vilkaar>) : Vilkaar {
    override fun accept(visitor: VilkaarVisitor) {
        visitor.visit(this)
        vilkaar.forEach { it.accept(visitor) }
    }
    override fun opplysningsbehov(): List<String> = emptyList()

    override val navn: String
        get() = vilkaar.joinToString(separator = " eller ") { it.navn }
    override fun vurder(opplysninger: List<VilkaarOpplysning<out Any>>): VurdertVilkaar {
        return vilkaar
            .map { it.vurder(opplysninger)}
            .let {
                val ny_tidslinje = it.map { it.resultat }
                    .reduce{ acc, cur ->
                        (acc + cur).map {_, it ->
                            (it.first?: VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING) + (it.second?: VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING)
                        }
                    }.normaliser()

                VurdertVilkaar(
                ny_tidslinje
                , it.map { objectMapper.valueToTree(it.serialize()) }, navn) }
    }
}

class EnkelSjekkAvOpplysning(vilkaarsnavn: String, val opplysningNavn: String, @get:JsonIgnore val test: VilkaarOpplysning<out Any>.() -> Tidslinje<VilkaarVurderingsResultat>) :
    Vilkaar {
    override fun opplysningsbehov(): List<String> {
        return listOf(opplysningNavn)
    }
    override val navn = vilkaarsnavn
    override fun vurder(opplysninger: List<VilkaarOpplysning<out Any>>) = opplysninger.find { it.opplysingType == opplysningNavn }?.let {
        VurdertVilkaar(it.test(), listOf(it), navn) }?: VurdertVilkaar(Tidslinje(LocalDate.MIN to  VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING), emptyList(), navn)
}

class EnkelSammenligningAvOpplysninger(vilkaarsnavn: String, val opplysningNavn: List<String>, @get:JsonIgnore val test: List<VilkaarOpplysning<out Any>>.() -> Tidslinje<VilkaarVurderingsResultat>) :
    Vilkaar {
    override fun opplysningsbehov(): List<String> {
        return opplysningNavn
    }
    override val navn = vilkaarsnavn
    override fun vurder(opplysninger: List<VilkaarOpplysning<out Any>>) = opplysninger.filter { it.opplysingType in opplysningNavn }?.let {
        VurdertVilkaar(it.test(), it, navn) }?: VurdertVilkaar(Tidslinje(LocalDate.MIN to  VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING), emptyList(), navn)
}

inline fun <reified T> enkelVurderingAvOpplysning(vilkarsNavn:String, opplysningsNavn: String, crossinline test: T.() -> Tidslinje<VilkaarVurderingsResultat>): Vilkaar = EnkelSjekkAvOpplysning(vilkarsNavn, opplysningsNavn){
    val grunnlag = objectMapper.treeToValue<T>(this)!!
    grunnlag.test()
}

inline fun <reified T> enkelSammenligningAvOpplysninger(vilkarsNavn:String, opplysningsNavn: String, crossinline test: List<T>.() -> Tidslinje<VilkaarVurderingsResultat>): Vilkaar = EnkelSammenligningAvOpplysninger(vilkarsNavn, listOf(opplysningsNavn)){
    val grunnlag = map{objectMapper.treeToValue<T>(it)!!}
    grunnlag.test()
}

interface OpplysningType<T>{
    val opplysningsNavn: String
}