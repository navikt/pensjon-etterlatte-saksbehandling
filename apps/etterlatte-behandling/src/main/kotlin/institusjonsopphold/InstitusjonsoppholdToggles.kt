package no.nav.etterlatte.institusjonsopphold

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle

enum class InstitusjonsoppholdToggles(
    private val key: String,
) : FeatureToggle {
    KjoerHentingFraInst2("inst2-opphold-kjoer"),
    SettOppKjoering("inst2-opphold-sett-opp"),
    LagOppgaverForOpphold("inst2-opphold-oppgaver-kjoer"),
    LagOppgaverForAnnullerteOpphold("inst2-annullerte-opphold-oppgaver-kjoer"),
    SettOppOppgaverForOpphold("inst2-opphold-oppgaver-sett-opp"),
    ;

    override fun key(): String = key
}
