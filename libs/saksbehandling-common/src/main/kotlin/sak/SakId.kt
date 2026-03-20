package no.nav.etterlatte.libs.common.sak

import com.fasterxml.jackson.annotation.JsonValue
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.KeyDeserializer
import tools.jackson.databind.annotation.JsonDeserialize
import java.io.Serializable

@JsonDeserialize(keyUsing = SakIdKeyDeserializer::class)
@JvmInline
value class SakId(
    @JsonValue val sakId: Long,
) : Serializable {
    override fun toString() = sakId.toString()
}

fun String.tilSakId() = SakId(this.toLong())

class SakIdKeyDeserializer : KeyDeserializer() {
    override fun deserializeKey(
        key: String,
        ctx: DeserializationContext,
    ): SakId = key.tilSakId()
}
