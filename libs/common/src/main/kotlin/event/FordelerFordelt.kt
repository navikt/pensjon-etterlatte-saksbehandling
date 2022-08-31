package no.nav.etterlatte.libs.common.event

interface IFordelerFordelt {
    val soeknadFordeltKey get() = "soeknadFordelt"
}

object FordelerFordelt : ISoeknadInnsendt, IFordelerFordelt {
    val eventName: String get() = "FORDELER:FORDELT"
}