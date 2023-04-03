package no.nav.etterlatte.libs.common.gyldigSoeknad

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDateTime

data class VurdertGyldighet(
    val navn: GyldighetsTyper,
    val resultat: VurderingsResultat,
    val basertPaaOpplysninger: Any?
)

data class ManuellVurdering(
    val begrunnelse: String,
    val kilde: Grunnlagsopplysning.Saksbehandler
)

data class GyldighetsResultat(
    val resultat: VurderingsResultat?,
    val vurderinger: List<VurdertGyldighet>,
    val vurdertDato: LocalDateTime
)

enum class VurderingsResultat(val prioritet: Int) {
    OPPFYLT(1),
    KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING(2),
    IKKE_OPPFYLT(3)
}