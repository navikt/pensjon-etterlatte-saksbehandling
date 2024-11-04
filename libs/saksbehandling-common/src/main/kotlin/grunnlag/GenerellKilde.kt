package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.libs.common.dbutils.Tidspunkt

data class GenerellKilde(
    val type: String,
    val tidspunkt: Tidspunkt,
    val detalj: String? = null,
)
