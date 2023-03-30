package no.nav.etterlatte.libs.common.person

@JvmInline
value class NavPersonIdent(val ident: String) {

    init {
        if (!ident.matches(Regex("^\\d{11}$"))) {
            throw IllegalArgumentException("Fikk en verdi som ikke er en Npid")
        }
        val month = ident.substring(2 until 4).toInt()
        if (month < 21 || month > 32) {
            throw IllegalArgumentException("Fikk en verdi som ikke er en Npid")
        }
    }

    override fun toString(): String {
        return ident.replaceRange(6 until 11, "*****")
    }
}