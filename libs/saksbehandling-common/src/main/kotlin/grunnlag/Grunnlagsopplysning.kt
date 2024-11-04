package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import java.time.YearMonth
import java.util.UUID

open class Grunnlagsopplysning<T>(
    val id: UUID,
    val kilde: Kilde,
    val opplysningType: Opplysningstype,
    val meta: ObjectNode,
    val opplysning: T,
    val attestering: Kilde? = null,
    val fnr: Folkeregisteridentifikator? = null,
    val periode: Periode? = null,
) {
    companion object {
        fun empty(
            opplysningType: Opplysningstype,
            kilde: Kilde,
            fnr: Folkeregisteridentifikator,
            fom: YearMonth?,
            tom: YearMonth? = null,
        ): Grunnlagsopplysning<out Any?> =
            Grunnlagsopplysning(
                id = UUID.randomUUID(),
                kilde = kilde,
                opplysningType = opplysningType,
                meta = objectMapper.createObjectNode(),
                opplysning = null,
                attestering = null,
                fnr = fnr,
                periode = fom?.let { Periode(it, tom) },
            )

        val automatiskSaksbehandler = Saksbehandler.create(ident = "Gjenny")
    }

    override fun toString(): String = "Opplysning om ${opplysningType.name}: oppgitt av $kilde til å være: $opplysning. Id: $id"

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = Saksbehandler::class, name = "saksbehandler"),
        JsonSubTypes.Type(value = Privatperson::class, name = "privatperson"),
        JsonSubTypes.Type(value = Pdl::class, name = "pdl"),
        JsonSubTypes.Type(value = Persondata::class, name = "persondata"),
        JsonSubTypes.Type(value = RegelKilde::class, name = "regel"),
        JsonSubTypes.Type(value = Gjenoppretting::class, name = "gjenoppretting"),
        JsonSubTypes.Type(value = Pesys::class, name = "pesys"),
        JsonSubTypes.Type(value = UkjentInnsender::class, name = "ukjentinnsender"),
        JsonSubTypes.Type(value = Gjenny::class, name = "gjenny"),
    )
    sealed class Kilde(
        val type: String,
    ) {
        fun toJson() = objectMapperKilde.writeValueAsString(this)
    }

    data class Saksbehandler(
        val ident: String,
        val tidspunkt: Tidspunkt,
    ) : Kilde("saksbehandler") {
        companion object {
            fun create(ident: String) = Saksbehandler(ident, Tidspunkt.now())
        }

        override fun toString(): String = "saksbehandler $ident"
    }

    data class Gjenny(
        val ident: String,
        val tidspunkt: Tidspunkt,
    ) : Kilde("gjenny") {
        companion object {
            fun create(ident: String) = Saksbehandler(ident, Tidspunkt.now())
        }

        override fun toString(): String = "gjenny $ident"
    }

    data class Pesys(
        val tidspunkt: Tidspunkt,
    ) : Kilde("pesys") {
        companion object {
            fun create() = Pesys(Tidspunkt.now())
        }

        override fun toString(): String = "pesys"
    }

    data class Gjenoppretting(
        val tidspunkt: Tidspunkt,
    ) : Kilde("gjenoppretting") {
        companion object {
            fun create() = Gjenoppretting(Tidspunkt.now())
        }

        override fun toString(): String = "gjenoppretting"
    }

    data class Privatperson(
        val fnr: String,
        val mottatDato: Tidspunkt,
    ) : Kilde("privatperson")

    data class Pdl(
        val tidspunktForInnhenting: Tidspunkt,
        val registersReferanse: String?,
        val opplysningId: String?,
    ) : Kilde("pdl") {
        val navn = "pdl"

        override fun toString(): String = navn
    }

    data class Persondata(
        val tidspunktForInnhenting: Tidspunkt,
        val registersReferanse: String?,
        val opplysningId: String?,
    ) : Kilde("persondata") {
        val navn = "persondata"

        override fun toString(): String = navn
    }

    data class RegelKilde(
        val navn: String,
        val ts: Tidspunkt,
        val versjon: String,
    ) : Kilde("regel") {
        override fun toString(): String = "beregningsregel  $navn"
    }

    data class UkjentInnsender(
        val tidspunkt: Tidspunkt,
    ) : Kilde("ukjentinnsender") {
        companion object {
            fun create() = UkjentInnsender(Tidspunkt.now())
        }

        override fun toString(): String = this.type
    }
}

val objectMapperKilde =
    jacksonObjectMapper().registerModule(JavaTimeModule()).disable(
        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
    )

fun <T : Any> lagOpplysning(
    opplysningsType: Opplysningstype,
    kilde: Grunnlagsopplysning.Kilde,
    opplysning: T,
    periode: Periode? = null,
): Grunnlagsopplysning<T> =
    Grunnlagsopplysning(
        id = UUID.randomUUID(),
        kilde = kilde,
        opplysningType = opplysningsType,
        meta = objectMapper.createObjectNode(),
        opplysning = opplysning,
        periode = periode,
    )
