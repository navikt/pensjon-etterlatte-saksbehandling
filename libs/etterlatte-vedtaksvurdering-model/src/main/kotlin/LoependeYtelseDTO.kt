package no.nav.etterlatte.libs.common.vedtak

import java.time.LocalDate
import java.util.UUID

data class LoependeYtelseDTO(val erLoepende: Boolean, val dato: LocalDate, val behandlingId: UUID? = null)
