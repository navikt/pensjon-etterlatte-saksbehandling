package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.Instant
import java.time.YearMonth
import java.util.*

open class Grunnlagsopplysning<T>(
    val id: UUID,
    val kilde: Kilde,
    val opplysningType: Opplysningstype,
    val meta: ObjectNode,
    val opplysning: T,
    val attestering: Kilde? = null,
    val fnr: Foedselsnummer? = null,
    val periode: Periode? = null
) {
    companion object {
        fun empty(
            opplysningType: Opplysningstype,
            kilde: Kilde,
            fnr: Foedselsnummer,
            fom: YearMonth?,
            tom: YearMonth? = null
        ): Grunnlagsopplysning<out Any?> {
            return Grunnlagsopplysning(
                id = UUID.randomUUID(),
                kilde = kilde,
                opplysningType = opplysningType,
                meta = objectMapper.createObjectNode(),
                opplysning = null,
                attestering = null,
                fnr = fnr,
                periode = fom?.let { Periode(it, tom) }
            )
        }

        val automatiskSaksbehandler = Saksbehandler.create(ident = "Doffen")
    }

    override fun toString(): String {
        return "Opplysning om ${opplysningType.name}: oppgitt av $kilde til å være: $opplysning. Id: $id"
    }

    fun erPeriodisert() = periode != null

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type"
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = Saksbehandler::class, name = "saksbehandler"),
        JsonSubTypes.Type(value = Privatperson::class, name = "privatperson"),
        JsonSubTypes.Type(value = Pdl::class, name = "pdl"),
        JsonSubTypes.Type(value = RegelKilde::class, name = "regel")
    )
    sealed class Kilde(val type: String) {
        fun toJson() = objectMapperKilde.writeValueAsString(this)
    }

    data class Saksbehandler(val ident: String, val tidspunkt: Instant) : Kilde("saksbehandler") {
        companion object {
            fun create(ident: String) = Saksbehandler(ident, Tidspunkt.now().instant)
        }

        override fun toString(): String {
            return "saksbehandler $ident"
        }
    }

    data class Privatperson(val fnr: String, val mottatDato: Instant) : Kilde("privatperson")

    data class Pdl(
        val navn: String,
        val tidspunktForInnhenting: Instant,
        val registersReferanse: String?,
        val opplysningId: String?
    ) : Kilde("pdl") {
        override fun toString(): String {
            return navn
        }
    }

    data class RegelKilde(val navn: String, val ts: Instant, val versjon: String) : Kilde("regel") {
        override fun toString(): String {
            return "beregningsregel  $navn"
        }
    }
}

val objectMapperKilde = jacksonObjectMapper().registerModule(JavaTimeModule()).disable(
    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
)