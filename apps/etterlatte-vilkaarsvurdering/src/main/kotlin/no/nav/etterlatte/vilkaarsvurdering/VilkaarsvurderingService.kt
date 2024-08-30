package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vilkaarsvurdering.OppdaterVurdertVilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.vilkaarsvurdering.VurdertVilkaarsvurderingDto
import java.util.UUID

class VilkaarsvurderingService(
    private val vilkaarsvurderingRepository: VilkaarsvurderingRepository,
) {
    fun hentVilkaarsvurdering(behandlingId: UUID): Vilkaarsvurdering? = vilkaarsvurderingRepository.hent(behandlingId)

    fun erMigrertYrkesskadefordel(sakId: SakId): Boolean = vilkaarsvurderingRepository.hentMigrertYrkesskadefordel(sakId = sakId)

    fun harRettUtenTidsbegrensning(behandlingId: UUID): Boolean =
        vilkaarsvurderingRepository
            .hent(behandlingId)
            ?.vilkaar
            ?.filter { it.hovedvilkaar.type == VilkaarType.OMS_RETT_UTEN_TIDSBEGRENSNING }
            ?.any { it.hovedvilkaar.resultat == Utfall.OPPFYLT }
            ?: false

    fun oppdaterTotalVurdering(vurdertVilkaar: VurdertVilkaarsvurderingDto): Vilkaarsvurdering =
        vilkaarsvurderingRepository.lagreVilkaarsvurderingResultatvanlig(
            vurdertVilkaar.virkningstidspunkt,
            vurdertVilkaar.resultat,
            vurdertVilkaar.vilkaarsvurdering,
        )

    fun slettVilkaarsvurderingResultat(behandlingId: UUID): Vilkaarsvurdering =
        vilkaarsvurderingRepository.slettVilkaarsvurderingResultat(behandlingId)

    fun slettVilkaarsvurdering(vilkaarsvurderingId: UUID): Boolean = vilkaarsvurderingRepository.slettVilkaarvurdering(vilkaarsvurderingId)

    fun oppdaterVurderingPaaVilkaar(oppdatertvilkaar: OppdaterVurdertVilkaar): Vilkaarsvurdering =
        vilkaarsvurderingRepository.oppdaterVurderingPaaVilkaar(oppdatertvilkaar.behandlingId, oppdatertvilkaar.vurdertVilkaar)

    fun slettVurderingPaaVilkaar(
        behandlingId: UUID,
        vilkaarId: UUID,
    ): Vilkaarsvurdering {
        vilkaarsvurderingRepository.slettVilkaarResultat(behandlingId, vilkaarId)
        return vilkaarsvurderingRepository.hent(behandlingId)!!
    }

    fun kopierVilkaarsvurdering(
        nyVilkaarsvurdering: Vilkaarsvurdering,
        tidligereVilkaarsvurderingId: UUID,
    ): Vilkaarsvurdering =
        vilkaarsvurderingRepository.kopierVilkaarsvurdering(
            nyVilkaarsvurdering = nyVilkaarsvurdering,
            kopiertFraId = tidligereVilkaarsvurderingId,
        )

    fun opprettVilkaarsvurdering(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering =
        vilkaarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)

    fun oppdaterGrunnlagsversjon(
        behandlingId: UUID,
        grunnlagsversjon: Long,
    ) {
        vilkaarsvurderingRepository.oppdaterGrunnlagsversjon(
            behandlingId = behandlingId,
            grunnlagVersjon = grunnlagsversjon,
        )
    }
}

class BehandlingstilstandException : IllegalStateException()

class VilkaarsvurderingTilstandException(
    message: String,
) : IllegalStateException(message)
