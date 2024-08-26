package no.nav.etterlatte.vilkaarsvurdering.service

import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.VilkaarTypeOgUtfall
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.Vilkaarsvurdering
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.VurdertVilkaar
import java.time.LocalDateTime
import java.util.UUID

class AldersovergangService(
    private val vilkaarsvurderingService: VilkaarsvurderingService,
) {
    suspend fun behandleOpphoerAldersovergang(
        behandlingId: UUID,
        loependeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        tidspunkt: () -> LocalDateTime = { LocalDateTime.now() },
    ): Vilkaarsvurdering {
        val vilkaarsvurdering =
            vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId)
                ?: vilkaarsvurderingService
                    .kopierVilkaarsvurdering(
                        behandlingId = behandlingId,
                        kopierFraBehandling = loependeBehandlingId,
                        brukerTokenInfo = brukerTokenInfo,
                        kopierResultat = false,
                    ).vilkaarsvurdering

        val aldersvilkaar =
            vilkaarsvurdering.vilkaar.single {
                it.hovedvilkaar.type in
                    listOf(
                        VilkaarType.BP_ALDER_BARN_2024,
                        VilkaarType.OMS_OVERLAPPENDE_YTELSER,
                    )
            }

        // Aldersvilkår => ikke oppfylt
        vilkaarsvurderingService.oppdaterVurderingPaaVilkaar(
            behandlingId = behandlingId,
            brukerTokenInfo = brukerTokenInfo,
            vurdertVilkaar =
                VurdertVilkaar(
                    vilkaarId = aldersvilkaar.id,
                    hovedvilkaar =
                        VilkaarTypeOgUtfall(
                            type = aldersvilkaar.hovedvilkaar.type,
                            resultat = Utfall.IKKE_OPPFYLT,
                        ),
                    vurdering =
                        VilkaarVurderingData(
                            kommentar = "Aldersgrensen er passert",
                            tidspunkt = tidspunkt(),
                            saksbehandler = Fagsaksystem.EY.navn,
                        ),
                ),
        )

        // Resultat på hele vurderingen => ikke oppfylt
        return vilkaarsvurderingService
            .oppdaterTotalVurdering(
                behandlingId,
                brukerTokenInfo,
                VilkaarsvurderingResultat(
                    utfall = VilkaarsvurderingUtfall.IKKE_OPPFYLT,
                    kommentar = "Automatisk aldersovergang",
                    tidspunkt = tidspunkt(),
                    saksbehandler = Fagsaksystem.EY.navn,
                ),
            ).vilkaarsvurdering
    }
}
