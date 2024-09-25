package no.nav.etterlatte.libs.common.sak

import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable

data class SakId(
    @JsonValue val sakId: Long,
) : Serializable {
    override fun toString() = sakId.toString()
}

fun String.tilSakId() = SakId(this.toLong())
