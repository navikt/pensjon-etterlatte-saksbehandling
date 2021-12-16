package no.nav.etterlatte.vilkaar

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.etterlatte.libs.common.objectMapper

val objectMapper: ObjectMapper = JsonMapper.builder()
    .addModule(JavaTimeModule())
    .addModule(KotlinModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
    .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
    .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
    .build()
typealias Opplysning = ObjectNode
/*
interface Opplysning<T> {
    val navn: String
    val verdi: T
}
*/

enum class VilkaarVurderingsResultat { OPPFYLT, IKKE_OPPFYLT, KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING }

class VurdertVilkaar(
    val resultat: VilkaarVurderingsResultat,
    val basertPaaOpplysninger: List<Opplysning>
)

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
    fun vurder(opplysninger: List<Opplysning>): VurdertVilkaar
    fun accept(visitor: VilkaarVisitor) = visitor.visit(this)
    fun opplysningsbehov():List<String>
}


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
            .let { VurdertVilkaar(
                if(it.all { it.resultat == VilkaarVurderingsResultat.OPPFYLT}) VilkaarVurderingsResultat.OPPFYLT
                else VilkaarVurderingsResultat.IKKE_OPPFYLT
                , it.map { objectMapper.valueToTree(it) }) }
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
    override fun vurder(opplysninger: List<Opplysning>): VurdertVilkaar {
        return vilkaar
            .map { it.vurder(opplysninger)}
            .let { VurdertVilkaar(
                if(it.any { it.resultat == VilkaarVurderingsResultat.OPPFYLT}) VilkaarVurderingsResultat.OPPFYLT
                else VilkaarVurderingsResultat.IKKE_OPPFYLT
                , it.map { objectMapper.valueToTree(it) }) }
    }
}

class EnkelSjekkAvOpplysning(vilkaarsnavn: String, val opplysningNavn: String, val test: Opplysning.() -> VilkaarVurderingsResultat) :
    Vilkaar {
    override fun opplysningsbehov(): List<String> {
        return listOf(opplysningNavn)
    }
    override val navn = vilkaarsnavn
    override fun vurder(opplysninger: List<Opplysning>) =opplysninger.find { it["navn"].textValue() == opplysningNavn }?.let {
        VurdertVilkaar(it.test(), listOf(it)) }?: VurdertVilkaar(VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, emptyList())
}










