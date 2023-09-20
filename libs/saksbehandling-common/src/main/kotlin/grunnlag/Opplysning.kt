package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = Opplysning.Konstant::class, name = "konstant"),
)
sealed class Opplysning<T>(val type: String) {
    data class Konstant<T>(
        val id: UUID,
        val kilde: Grunnlagsopplysning.Kilde,
        val verdi: T,
    ) : Opplysning<T>("konstant") {
        companion object {
            fun <T> create(grunnlagsopplysning: Grunnlagsopplysning<T>) =
                Konstant(
                    id = grunnlagsopplysning.id,
                    kilde = grunnlagsopplysning.kilde,
                    verdi = grunnlagsopplysning.opplysning,
                )
        }
    }
}
