package no.nav.etterlatte.libs.common.person

import com.fasterxml.jackson.annotation.JsonProperty

data class NavPersonIdent(@JsonProperty("value") val ident: String) {

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
}