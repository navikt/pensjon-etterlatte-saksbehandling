package no.nav.etterlatte.libs.common.behandling

import java.time.LocalDate
import java.util.UUID

data class Etterbetaling(
    val behandlingId: UUID,
    val fraDato: LocalDate,
    val tilDato: LocalDate,
)
