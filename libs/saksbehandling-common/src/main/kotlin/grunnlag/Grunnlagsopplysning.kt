package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning.Kilde
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
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
        val automatiskSaksbehandler = Saksbehandler.create(ident = "Gjenny")
        const val ALDERSPENSJONNAME = "alderspensjon"
        const val UFOERETRYGDNAME = "ufoeretrygd"
        const val SAKSBEHANDLERNAME = "saksbehandler"
        const val PRIVATPERSONNAME = "privatperson"
        const val GJENNYNAME = "gjenny"
        const val PESYSNAME = "pesys"
        const val GJENOPPRETTINGNAME = "gjenoppretting"
        const val PDLNAME = "pdl"
        const val PERSONDATANAME = "persondata"
        const val REGELNAME = "regel"
        const val UKJENTINNSENDERNAME = "ukjentinnsender"
    }

    override fun toString(): String = "Opplysning om ${opplysningType.name}: oppgitt av $kilde til å være: $opplysning. Id: $id"

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = Saksbehandler::class, name = SAKSBEHANDLERNAME),
        JsonSubTypes.Type(value = Privatperson::class, name = PRIVATPERSONNAME),
        JsonSubTypes.Type(value = Pdl::class, name = PDLNAME),
        JsonSubTypes.Type(value = Persondata::class, name = PERSONDATANAME),
        JsonSubTypes.Type(value = RegelKilde::class, name = REGELNAME),
        JsonSubTypes.Type(value = Gjenoppretting::class, name = GJENOPPRETTINGNAME),
        JsonSubTypes.Type(value = Pesys::class, name = PESYSNAME),
        JsonSubTypes.Type(value = UkjentInnsender::class, name = UKJENTINNSENDERNAME),
        JsonSubTypes.Type(value = Gjenny::class, name = GJENNYNAME),
        JsonSubTypes.Type(value = Alderspensjon::class, name = ALDERSPENSJONNAME),
        JsonSubTypes.Type(value = Ufoeretrygd::class, name = UFOERETRYGDNAME),
    )
    sealed class Kilde(
        val type: String,
    ) {
        fun toJson() = objectMapperKilde.writeValueAsString(this)
    }

    data class Saksbehandler(
        val ident: String,
        val tidspunkt: Tidspunkt,
    ) : Kilde(SAKSBEHANDLERNAME) {
        companion object {
            fun create(ident: String) = Saksbehandler(ident, Tidspunkt.now())
        }

        override fun toString(): String = "$SAKSBEHANDLERNAME $ident"
    }

    data class Gjenny(
        val ident: String,
        val tidspunkt: Tidspunkt,
    ) : Kilde(GJENNYNAME) {
        companion object {
            fun create(ident: String) = Saksbehandler(ident, Tidspunkt.now())
        }

        override fun toString(): String = "$GJENNYNAME $ident"
    }

    data class Pesys(
        val tidspunkt: Tidspunkt,
    ) : Kilde(PESYSNAME) {
        companion object {
            fun create() = Pesys(Tidspunkt.now())
        }

        override fun toString(): String = PESYSNAME
    }

    sealed class PesysYtelseKilde(
        type: String,
    ) : Kilde(type)

    data class Alderspensjon(
        val tidspunkt: Tidspunkt,
    ) : PesysYtelseKilde(ALDERSPENSJONNAME) {
        companion object {
            fun create() = Alderspensjon(Tidspunkt.now())
        }

        override fun toString(): String = ALDERSPENSJONNAME
    }

    data class Ufoeretrygd(
        val tidspunkt: Tidspunkt,
    ) : PesysYtelseKilde(UFOERETRYGDNAME) {
        companion object {
            fun create() = Ufoeretrygd(Tidspunkt.now())
        }

        override fun toString(): String = UFOERETRYGDNAME
    }

    data class Gjenoppretting(
        val tidspunkt: Tidspunkt,
    ) : Kilde(GJENOPPRETTINGNAME) {
        companion object {
            fun create() = Gjenoppretting(Tidspunkt.now())
        }

        override fun toString(): String = GJENOPPRETTINGNAME
    }

    data class Privatperson(
        val fnr: String,
        val mottatDato: Tidspunkt,
    ) : Kilde(PRIVATPERSONNAME)

    data class Pdl(
        val tidspunktForInnhenting: Tidspunkt,
        val registersReferanse: String?,
        val opplysningId: String?,
    ) : Kilde(PDLNAME) {
        val navn = PDLNAME

        override fun toString(): String = PDLNAME
    }

    data class Persondata(
        val tidspunktForInnhenting: Tidspunkt,
        val registersReferanse: String?,
        val opplysningId: String?,
    ) : Kilde(PERSONDATANAME) {
        val navn = PERSONDATANAME

        override fun toString(): String = PERSONDATANAME
    }

    data class RegelKilde(
        val navn: String,
        val ts: Tidspunkt,
        val versjon: String,
    ) : Kilde(REGELNAME) {
        override fun toString(): String = "beregningsregel  $navn"
    }

    data class UkjentInnsender(
        val tidspunkt: Tidspunkt,
    ) : Kilde(UKJENTINNSENDERNAME) {
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
    kilde: Kilde,
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
