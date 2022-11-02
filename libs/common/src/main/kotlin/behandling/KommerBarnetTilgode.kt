package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke

data class KommerBarnetTilgode(
    val svar: JaNeiVetIkke,
    val begrunnelse: String,
    val kilde: Grunnlagsopplysning.Saksbehandler
)