package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.person.PersonRolle
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class PersonMedSakerOgRoller(
    val fnr: String,
    val sakerOgRoller: List<SakOgRolle>,
)

data class SakOgRolle(
    val sakId: Long,
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
            UKJENT -> {
                sikkerlogger().warn("Ukjent saks")
                throw Exception("Ukjent Saksrolle kan ikke castes til PersonRolle")
            }
        }

    companion object {
        val log: Logger = LoggerFactory.getLogger(Saksrolle::class.java)

        fun enumVedNavnEllerUkjent(rolle: String) =
            try {
                Saksrolle.valueOf(rolle.uppercase())
            } catch (e: Exception) {
                log.error(
                    "Kunne ikke bestemme rolle fra kolonnen $rolle i databasen. Dette betyr at kolonne-navnet i " +
                        "databasen verdien er hentet fra ikke samsvarer med noen av enum-verdiene til Saksrolle. " +
                        "Setter rolle som ukjent",
                )
                UKJENT
            }
    }
}
