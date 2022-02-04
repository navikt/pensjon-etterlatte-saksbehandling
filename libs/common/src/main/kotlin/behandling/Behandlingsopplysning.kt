package no.nav.etterlatte.libs.common.behandling

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

import java.time.Instant
import java.util.*


open class Behandlingsopplysning<T>(
    val id: UUID,
    val kilde: Kilde,
    val opplysningType: String,
    val meta: ObjectNode,
    val opplysning: T,
    val attestering: Kilde? = null
) {
    override fun toString(): String {
        return "Opplysning om $opplysningType: oppgitt av $kilde til å være: $opplysning"
    }

    open fun opplysningerSomMåAttesteres():List<String>{
        return if(kilde is Saksbehandler && attestering == null) listOf("!" + opplysningType) else emptyList()
    }


    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type"
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = Saksbehandler::class, name = "saksbehandler"),
        JsonSubTypes.Type(value = Privatperson::class, name = "privatperson"),
        JsonSubTypes.Type(value = Register::class, name = "register"),
        JsonSubTypes.Type(value = RegelKilde::class, name = "regel"),
    )
    sealed class Kilde(val type: String) {
        fun toJson() = objectMapperKilde.writeValueAsString(this)
    }
    class Saksbehandler(val ident: String) : Kilde("saksbehandler"){
        override fun toString(): String {
            return "saksbehandler $ident"
        }
    }
    class Privatperson(val fnr: String, val mottatDato: Instant) : Kilde("privatperson") {
    }

    class Register(val navn: String, val tidspunktForInnhenting: Instant, val registersReferanse: String?) : Kilde("register") {
        override fun toString(): String {
            return navn
        }
    }

    class RegelKilde(val navn: String, val ts: Instant, val versjon: String) : Kilde("regel"){
        override fun toString(): String {
            return "beregningsregel  $navn"
        }
    }
}

val objectMapperKilde = jacksonObjectMapper().registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

