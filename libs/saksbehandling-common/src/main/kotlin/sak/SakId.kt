package no.nav.etterlatte.libs.common.sak

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
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
