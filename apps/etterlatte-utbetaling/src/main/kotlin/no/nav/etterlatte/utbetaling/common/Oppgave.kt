package no.nav.etterlatte.utbetaling.common

enum class Oppgavetype {
    START_GRENSESNITTAVSTEMMING,
    SETT_KVITTERING_MANUELT,
}

data class Oppgave(
    val oppgavetype: Oppgavetype,
    val vedtakId: Long?,
)
