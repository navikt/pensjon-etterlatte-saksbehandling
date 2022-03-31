package no.nav.etterlatte.libs.common.gyldigSoeknad

import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import java.time.LocalDateTime

data class VurdertGyldighet(
    val navn: GyldighetsTyper,
    val resultat: VurderingsResultat,
    val vurdertDato: LocalDateTime
)