package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.person.PersonRolle

data class PersonMedSakerOgRoller(
    val fnr: String,
    val sakiderOgRoller: List<SakidOgRolle>,
)

data class SakidOgRolle(
    val sakId: no.nav.etterlatte.libs.common.sak.SakId,
    val rolle: Saksrolle,
)

enum class Saksrolle {
    SOEKER,
    SOESKEN,
    AVDOED,
    GJENLEVENDE,
    INNSENDER,
    UKJENT,
    ;

    fun toPersonrolle(sakType: SakType): PersonRolle =
        when (this) {
            SOEKER ->
                when (sakType) {
                    SakType.BARNEPENSJON -> PersonRolle.BARN
                    SakType.OMSTILLINGSSTOENAD -> PersonRolle.GJENLEVENDE
                }
            SOESKEN -> PersonRolle.TILKNYTTET_BARN
            AVDOED -> PersonRolle.AVDOED
            GJENLEVENDE -> PersonRolle.GJENLEVENDE
            INNSENDER -> PersonRolle.INNSENDER
            UKJENT -> throw Exception("Ukjent Saksrolle kan ikke castes til PersonRolle")
        }
}
