package no.nav.etterlatte.libs.common.event

interface IFordelerFordelt {
    val soeknadFordeltKey get() = "soeknadFordelt"
    val sakIdKey get() = "sakId"
}

object FordelerFordelt : ISoeknadInnsendt, IFordelerFordelt