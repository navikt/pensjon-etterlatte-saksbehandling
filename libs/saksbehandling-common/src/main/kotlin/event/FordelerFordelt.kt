package no.nav.etterlatte.libs.common.event

interface IFordelerFordelt {
    val soeknadFordeltKey get() = "soeknadFordelt"
    val soeknadTrengerManuellJournalfoering get() = "trengerManuellJournalfoering"
}

object FordelerFordelt : ISoeknadInnsendt, IFordelerFordelt
