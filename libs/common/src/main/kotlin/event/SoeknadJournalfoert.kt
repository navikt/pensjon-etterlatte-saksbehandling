package no.nav.etterlatte.libs.common.event

interface ISoeknadJournalfoert {
    val dokarkivKey get() = "@dokarkivRetur"
}

object SoeknadJournalfoert : ISoeknadJournalfoert