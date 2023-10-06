package no.nav.etterlatte.brev.model

import java.time.LocalDate

abstract class EndringBrevData : BrevData()

data class EtterbetalingDTO(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
)
