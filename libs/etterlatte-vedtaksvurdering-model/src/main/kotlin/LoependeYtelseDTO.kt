package no.nav.etterlatte.libs.common.vedtak

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class LoependeYtelseDTO(
    val erLoepende: Boolean,
    val underSamordning: Boolean,
    val dato: LocalDate,
    val behandlingId: UUID? = null,
    // TODO kun relevant for regulering 2024 da feltet opphoerFraOgMed er innført i forkant av reguleringen 2024
    val opphoerFraOgMed: YearMonth? = null,
)
