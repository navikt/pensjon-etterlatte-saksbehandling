package no.nav.etterlatte.token

sealed class Bruker(
    open val accessToken: String
) {
    abstract fun ident(): String

    abstract fun erSystembruker(): Boolean

    abstract fun saksbehandlerEnhet(saksbehandlere: Map<String, String>): String

    companion object {
        private fun erSystembruker(oid: String?, sub: String?) = (oid == sub) && (oid != null)
        fun of(accessToken: String, saksbehandler: String?, oid: String?, sub: String?): Bruker {
            return if (erSystembruker(oid = oid, sub = sub)) {
                System(accessToken, oid!!, sub!!)
            } else if (saksbehandler != null) {
                Saksbehandler(accessToken, saksbehandler)
            } else {
                throw Exception(
                    "Er ikke systembruker, og Navident er null i token, sannsynligvis manglende claim NAVident"
                )
            }
        }
    }
}

data class System(override val accessToken: String, val oid: String, val sub: String) : Bruker(accessToken) {
    override fun erSystembruker() = true

    override fun ident() = Fagsaksystem.EY.name

    override fun saksbehandlerEnhet(saksbehandlere: Map<String, String>) = Fagsaksystem.EY.name
}

data class Saksbehandler(override val accessToken: String, val ident: String) : Bruker(accessToken) {
    override fun erSystembruker() = false

    override fun ident() = ident

    override fun saksbehandlerEnhet(saksbehandlere: Map<String, String>) = saksbehandlere[ident]
        ?: throw SaksbehandlerManglerEnhetException("Saksbehandler $ident mangler enhet fra secret")
}

enum class Claims {
    NAVident,
    oid, // ktlint-disable enum-entry-name-case
    sub // ktlint-disable enum-entry-name-case
}