package no.nav.etterlatte.token

sealed class Bruker {
    abstract fun ident(): String
    abstract fun saksbehandlerEnhet(saksbehandlere: Map<String, String>): String

    abstract fun accessToken(): String

    companion object {
        private fun erSystembruker(oid: String?, sub: String?) = (oid == sub) && (oid != null)
        fun of(accessToken: String, saksbehandler: String?, oid: String?, sub: String?): Bruker {
            return if (erSystembruker(oid = oid, sub = sub)) {
                System(oid!!, sub!!)
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

data class System(val oid: String, val sub: String) : Bruker() {
    override fun ident() = Fagsaksystem.EY.name

    override fun saksbehandlerEnhet(saksbehandlere: Map<String, String>) = Fagsaksystem.EY.name

    override fun accessToken() = throw NotImplementedError("Kun relevant for saksbehandler")
}

data class Saksbehandler(val accessToken: String, val ident: String) : Bruker() {
    override fun ident() = ident

    override fun saksbehandlerEnhet(saksbehandlere: Map<String, String>) = saksbehandlere[ident]
        ?: throw SaksbehandlerManglerEnhetException("Saksbehandler $ident mangler enhet fra secret")

    override fun accessToken() = accessToken
}

enum class Claims {
    NAVident,
    oid, // ktlint-disable enum-entry-name-case
    sub // ktlint-disable enum-entry-name-case
}