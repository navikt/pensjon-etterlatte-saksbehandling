package no.nav.etterlatte.behandling

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode

import com.fasterxml.jackson.module.kotlin.treeToValue
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.*


open class Opplysning(
    val id: UUID,
    val kilde: Kilde,
    val opplysningType: String,
    val meta: ObjectNode,
    val opplysning: ObjectNode,
    val attestering: Kilde? = null
) {
    override fun toString(): String {
        return "Opplysning om $opplysningType: oppgitt av $kilde til å være: $opplysning"
    }

    open fun opplysningerSomMåAttesteres():List<String>{
        return if(kilde is Saksbehandler && attestering == null) listOf("!" + opplysningType) else emptyList()
    }

    class KildeDeserializer(c:Class<Any>? = null): StdDeserializer<Kilde>(c) {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Kilde {
            val node: ObjectNode = p.codec.readTree(p)
            return when (node["type"].asText()) {
                "saksbehandler" -> Saksbehandler(node["ident"].textValue())
                "privatperson" -> Privatperson(node["fnr"].textValue(), objectMapper.treeToValue(node["mottatDato"])!!)
                else -> throw IllegalArgumentException()
            }


        }
    }

    @JsonDeserialize(using = KildeDeserializer::class)
    sealed class Kilde(val type: String) {
        open fun attributes(): Map<String, String?> = emptyMap()
        fun toJson() = objectMapper.writeValueAsString(mapOf("type" to type) + attributes())
    }
    class Saksbehandler(val ident: String) : Kilde("saksbehandler"){
        override fun attributes() = mapOf("ident" to ident)

        override fun toString(): String {
            return "saksbehandler $ident"
        }
    }
    class Privatperson(val fnr: String, val mottatDato: Instant) : Kilde("privatperson") {
        override fun attributes() = mapOf("fnr" to fnr, "mottatDato" to mottatDato.toString())
    }

    class Register(val navn: String, val tidspunktForInnhenting: Instant, val registersReferanse: String?) : Kilde("register") {
        override fun attributes() = mapOf("navn" to navn, "tidspunktForInnhenting" to tidspunktForInnhenting.toString(), "registersReferanse" to registersReferanse)
        override fun toString(): String {
            return navn
        }
    }

    class RegelKilde(val navn: String, val ts: Instant, val versjon: String) : Kilde("regel"){
        override fun attributes() = mapOf("navn" to navn, "ts" to ts.toString(), "versjon" to versjon)
        override fun toString(): String {
            return "beregningsregel  $navn"
        }
    }
}