package no.nav.etterlatte.libs.common.gyldigSoeknad

import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import java.time.LocalDateTime

data class VurdertGyldighet(
    val navn: GyldighetsTyper,
    val resultat: VurderingsResultat,
    val basertPaaOpplysninger: Any?
)

data class GyldighetsResultat(
    val resultat: VurderingsResultat?,
    val vurderinger: List<VurdertGyldighet>,
    val vurdertDato: LocalDateTime
)