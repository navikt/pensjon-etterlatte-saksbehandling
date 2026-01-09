package no.nav.etterlatte.brev.model

import java.time.LocalDate

data class EtterbetalingDTO(
    val datoFom: LocalDate,
    val datoTom: LocalDate,
)
