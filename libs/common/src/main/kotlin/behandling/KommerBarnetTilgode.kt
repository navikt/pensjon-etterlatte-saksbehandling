package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning

data class KommerBarnetTilgode(
    val svar: JaNeiVetIkke,
    val begrunnelse: String,
    val kilde: Grunnlagsopplysning.Saksbehandler
)

enum class JaNeiVetIkke { JA, NEI, VET_IKKE }