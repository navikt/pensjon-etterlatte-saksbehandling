package no.nav.etterlatte.vilkaarsvurdering.migrering

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
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
        when (vilkaar.hovedvilkaar.type) {
            VilkaarType.BP_FORMAAL_2024 -> {
                val kommentar = "Ja på virkningstidspunktet jf PDL. Informasjon hentet fra Pesys."
                lagreVilkaarsResultat(vilkaarsvurdering, vilkaar, Utfall.OPPFYLT, kommentar)
            }
            VilkaarType.BP_DOEDSFALL_FORELDER_2024 -> {
                val kommentar = "Ja. Bruker har hatt BP i Pesys etter en forelder død."
                lagreVilkaarsResultat(vilkaarsvurdering, vilkaar, Utfall.OPPFYLT, kommentar)
            }
            VilkaarType.BP_YRKESSKADE_AVDOED_2024 -> {
                val utfall = if (yrkesskade) Utfall.OPPFYLT else Utfall.IKKE_OPPFYLT
                lagreVilkaarsResultat(vilkaarsvurdering, vilkaar, utfall)
            }
            VilkaarType.BP_ALDER_BARN_2024 -> {
                val kommentar =
                    "Ja, på virkningstidspunktet etter nye regler fra 01.01.2024. Informasjon hentet fra Pesys."
                lagreVilkaarsResultat(vilkaarsvurdering, vilkaar, Utfall.OPPFYLT, kommentar)
            }
            VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024 -> {
                val kommentar = "Bor i Norge på virkningstidspunktet. Informasjon hentet fra PDL."
                // Saker hvor medlemskap er endret gjøres manuelt
                val utfall = Utfall.OPPFYLT
                lagreVilkaarsResultat(vilkaarsvurdering, vilkaar, utfall, kommentar)
            }
            else -> lagreVilkaarsResultat(vilkaarsvurdering, vilkaar, Utfall.IKKE_VURDERT)
        }
    }

    private fun lagreVilkaarsResultat(
        vilkaarsvurdering: Vilkaarsvurdering,
        vilkaar: Vilkaar,
        yrkesskadeUtfall: Utfall,
        kommentar: String = "Automatisk gjenoppretta basert på opphørt sak fra Pesys",
    ) = vilkaarsvurderingRepository.lagreVilkaarResultat(
        behandlingId = vilkaarsvurdering.behandlingId,
        vurdertVilkaar =
            VurdertVilkaar(
                vilkaarId = vilkaar.id,
                hovedvilkaar = VilkaarTypeOgUtfall(vilkaar.hovedvilkaar.type, yrkesskadeUtfall),
                vurdering =
                    VilkaarVurderingData(
                        kommentar = kommentar,
                        tidspunkt = Tidspunkt.now().toLocalDatetimeUTC(),
                        saksbehandler = Fagsaksystem.EY.navn,
                    ),
            ),
    )
}
