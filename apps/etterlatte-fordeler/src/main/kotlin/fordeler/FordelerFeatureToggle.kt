package fordeler

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle

enum class FordelerFeatureToggle(private val key: String) : FeatureToggle {
    TillatAlleScenarier("fordeler.tillat-alle-scenarier"),
    ManuellJournalfoering("fordeler.manuell-journalfoering"),
    ;

    override fun key() = key
}
