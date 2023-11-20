package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDate

data class Flyktning(
    val erFlyktning: Boolean,
    val virkningstidspunkt: LocalDate,
    val begrunnelse: String,
    val kilde: Grunnlagsopplysning.Kilde,
)
