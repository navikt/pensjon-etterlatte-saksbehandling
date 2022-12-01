package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vilkaarsvurdering.EnkeltVilkaarDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.LovreferanseDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.UtfallDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarTypeOgUtfallDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingDataDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultatDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfallDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VirkningstidspunktDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaarDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaarsvurderingResultatDto
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.VilkaarType
import java.time.LocalDateTime

fun VurdertVilkaarDto.toDomain(saksbehandler: String, tidspunkt: LocalDateTime) =
    VurdertVilkaar(
        hovedvilkaar = hovedvilkaar.toDomain(),
        unntaksvilkaar = unntaksvilkaar?.toDomain(),
        vurdering = VilkaarVurderingData(
            kommentar = kommentar,
            tidspunkt = tidspunkt,
            saksbehandler = saksbehandler
        )
    )

fun VurdertVilkaarsvurderingResultatDto.toDomain(saksbehandler: String, tidspunkt: LocalDateTime) =
    VilkaarsvurderingResultat(
        utfall = when (resultat) {
            VilkaarsvurderingUtfallDto.OPPFYLT -> VilkaarsvurderingUtfall.OPPFYLT
            VilkaarsvurderingUtfallDto.IKKE_OPPFYLT -> VilkaarsvurderingUtfall.IKKE_OPPFYLT
        },
        kommentar = kommentar,
        tidspunkt = tidspunkt,
        saksbehandler = saksbehandler
    )

private fun VilkaarTypeOgUtfallDto.toDomain() = VilkaarTypeOgUtfall(
    type = VilkaarType.valueOf(type),
    resultat = when (resultat) {
        UtfallDto.IKKE_OPPFYLT -> Utfall.IKKE_OPPFYLT
        UtfallDto.OPPFYLT -> Utfall.OPPFYLT
        UtfallDto.IKKE_VURDERT -> Utfall.IKKE_VURDERT
    }
)

fun Vilkaarsvurdering.toDto() = VilkaarsvurderingDto(
    behandlingId = behandlingId,
    vilkaar = vilkaar.map { it.toDto() },
    virkningstidspunkt = virkningstidspunkt.toDto(),
    resultat = resultat?.toDto()
)

private fun VilkaarsvurderingResultat.toDto() = VilkaarsvurderingResultatDto(
    utfall = when (utfall) {
        VilkaarsvurderingUtfall.OPPFYLT -> VilkaarsvurderingUtfallDto.OPPFYLT
        VilkaarsvurderingUtfall.IKKE_OPPFYLT -> VilkaarsvurderingUtfallDto.IKKE_OPPFYLT
    },
    kommentar = kommentar,
    saksbehandler = saksbehandler,
    tidspunkt = tidspunkt
)

private fun Vilkaar.toDto() = VilkaarDto(
    hovedvilkaar = hovedvilkaar.toDto(),
    unntaksvilkaar = unntaksvilkaar?.map { it.toDto() },
    vurdering = vurdering?.let {
        VilkaarVurderingDataDto(
            kommentar = it.kommentar,
            tidspunkt = it.tidspunkt,
            saksbehandler = it.saksbehandler
        )
    }
)

private fun EnkeltVilkaar.toDto() = EnkeltVilkaarDto(
    type = type.name,
    tittel = tittel,
    beskrivelse = beskrivelse,
    lovreferanse = lovreferanse.toDto(),
    resultat = resultat?.let { UtfallDto.valueOf(it.name) }
)

private fun Lovreferanse.toDto() = LovreferanseDto(
    paragraf = paragraf,
    ledd = ledd,
    bokstav = bokstav,
    lenke = lenke
)

private fun Virkningstidspunkt.toDto() = VirkningstidspunktDto(
    dato = dato,
    kilde = kilde.toJsonNode()
)