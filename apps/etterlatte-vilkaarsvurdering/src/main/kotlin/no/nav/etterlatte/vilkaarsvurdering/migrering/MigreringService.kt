package no.nav.etterlatte.vilkaarsvurdering.migrering

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.token.Fagsaksystem
import no.nav.etterlatte.vilkaarsvurdering.VilkaarTypeOgUtfall
import no.nav.etterlatte.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepository
import no.nav.etterlatte.vilkaarsvurdering.VurdertVilkaar

class MigreringService(
    private val vilkaarsvurderingRepository: VilkaarsvurderingRepository,
) {
    // Vilkår for yrkesskade må settes til oppfylt hvis det er yrkesskade for å få rett grunnlag/trygdetid
    fun settUtfallForAlleVilkaar(
        vilkaarsvurdering: Vilkaarsvurdering,
        yrkesskade: Boolean,
    ) = vilkaarsvurdering.vilkaar.forEach { vilkaar ->
        if (vilkaar.hovedvilkaar.type == VilkaarType.BP_YRKESSKADE_AVDOED_2024) {
            val utfall = if (yrkesskade) Utfall.OPPFYLT else Utfall.IKKE_OPPFYLT
            lagreVilkaarsResultat(vilkaarsvurdering, vilkaar, utfall)
        } else {
            lagreVilkaarsResultat(vilkaarsvurdering, vilkaar, Utfall.IKKE_VURDERT)
        }
    }

    private fun lagreVilkaarsResultat(
        vilkaarsvurdering: Vilkaarsvurdering,
        vilkaar: Vilkaar,
        yrkesskadeUtfall: Utfall,
    ) = vilkaarsvurderingRepository.lagreVilkaarResultat(
        behandlingId = vilkaarsvurdering.behandlingId,
        vurdertVilkaar =
            VurdertVilkaar(
                vilkaarId = vilkaar.id,
                hovedvilkaar = VilkaarTypeOgUtfall(vilkaar.hovedvilkaar.type, yrkesskadeUtfall),
                vurdering =
                    VilkaarVurderingData(
                        kommentar = "Automatisk gjenoppretta basert på opphørt sak fra Pesys",
                        tidspunkt = Tidspunkt.now().toLocalDatetimeUTC(),
                        saksbehandler = Fagsaksystem.EY.navn,
                    ),
            ),
    )
}
