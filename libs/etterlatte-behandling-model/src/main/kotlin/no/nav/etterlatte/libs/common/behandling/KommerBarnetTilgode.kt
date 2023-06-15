package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning

data class KommerBarnetTilgode(
    val svar: JaNeiMedBegrunnelse,
    val kilde: Grunnlagsopplysning.Kilde
)