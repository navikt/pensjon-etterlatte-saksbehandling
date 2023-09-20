package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.util.UUID

data class KommerBarnetTilgode(
    val svar: JaNei,
    val begrunnelse: String,
    val kilde: Grunnlagsopplysning.Kilde,
    val behandlingId: UUID? = null,
)
