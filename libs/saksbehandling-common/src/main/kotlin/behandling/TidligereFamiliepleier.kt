package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDate

data class TidligereFamiliepleier(
    val svar: Boolean,
    val kilde: Grunnlagsopplysning.Kilde,
    val foedselsnummer: String?,
    val opphoertPleieforhold: LocalDate?,
    val begrunnelse: String,
)

data class TidligereFamiliepleierRequest(
    val svar: Boolean,
    val foedselsnummer: String?,
    val opphoertPleieforhold: LocalDate?,
    val begrunnelse: String,
)
