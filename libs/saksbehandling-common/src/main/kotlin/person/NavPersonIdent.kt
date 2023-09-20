package no.nav.etterlatte.libs.common.person

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class NavPersonIdent(
    @JsonProperty("value") val ident: String,
) {
    init {
        if (!ident.matches(Regex("^\\d{11}$"))) {
            throw IllegalArgumentException("Fikk en verdi som ikke er en Npid")
        }
        val month = ident.substring(2 until 4).toInt()
        if (month !in 21..32 && month !in 61..72) {
            throw IllegalArgumentException("Fikk en verdi som ikke er en Npid")
        }
    }

    override fun toString(): String {
        return ident.replaceRange(6 until 11, "*****")
    }

    companion object {
        @JvmStatic
        @JsonCreator
        fun of(npid: String?): NavPersonIdent =
            try {
                NavPersonIdent(npid!!)
            } catch (e: Exception) {
                throw IllegalArgumentException("$npid er ikke en gyldig NPID", e)
            }
    }
}
