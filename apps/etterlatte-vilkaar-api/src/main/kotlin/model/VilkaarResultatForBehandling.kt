package model

import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat

data class VilkaarResultatForBehandling(
    val behandlingId: String,
    val vilkaarResultat: VilkaarResultat
)