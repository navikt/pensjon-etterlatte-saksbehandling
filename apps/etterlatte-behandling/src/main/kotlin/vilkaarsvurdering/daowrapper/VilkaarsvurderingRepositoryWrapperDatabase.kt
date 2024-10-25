package no.nav.etterlatte.vilkaarsvurdering.dao

import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaar
import no.nav.etterlatte.vilkaarsvurdering.ektedao.VilkaarsvurderingRepository
import java.time.LocalDate
import java.util.UUID

interface VilkaarsvurderingRepositoryWrapper {
    fun hent(behandlingId: UUID): Vilkaarsvurdering?

    fun hentMigrertYrkesskadefordel(
        behandlingId: UUID,
        sakId: SakId,
    ): Boolean

    fun opprettVilkaarsvurdering(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering

    fun kopierVilkaarsvurdering(
        nyVilkaarsvurdering: Vilkaarsvurdering,
        kopiertFraId: UUID,
    ): Vilkaarsvurdering

    fun slettVilkaarsvurderingResultat(behandlingId: UUID): Vilkaarsvurdering

    fun lagreVilkaarsvurderingResultat(
        behandlingId: UUID,
        virkningstidspunkt: LocalDate,
        resultat: VilkaarsvurderingResultat,
    ): Vilkaarsvurdering

    fun oppdaterVurderingPaaVilkaar(
        behandlingId: UUID,
        vurdertVilkaar: VurdertVilkaar,
    ): Vilkaarsvurdering

    fun slettVilkaarResultat(
        behandlingId: UUID,
        vilkaarId: UUID,
    ): Vilkaarsvurdering

    fun oppdaterGrunnlagsversjon(
        behandlingId: UUID,
        grunnlagVersjon: Long,
    )

    fun slettVilkaarvurdering(
        behandlingId: UUID,
        vilkaarsvurderingId: UUID,
    )
}

class VilkaarsvurderingRepositoryWrapperDatabase(
    private val vilkarsvurderingRepository: VilkaarsvurderingRepository,
) : VilkaarsvurderingRepositoryWrapper {
    override fun hent(behandlingId: UUID): Vilkaarsvurdering? = vilkarsvurderingRepository.hent(behandlingId)

    override fun hentMigrertYrkesskadefordel(
        behandlingId: UUID,
        sakId: SakId,
    ): Boolean = vilkarsvurderingRepository.hentMigrertYrkesskadefordel(sakId)

    override fun opprettVilkaarsvurdering(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering =
        vilkarsvurderingRepository.opprettVilkaarsvurdering(vilkaarsvurdering)

    override fun kopierVilkaarsvurdering(
        nyVilkaarsvurdering: Vilkaarsvurdering,
        kopiertFraId: UUID,
    ): Vilkaarsvurdering {
        opprettVilkaarsvurdering(nyVilkaarsvurdering)
        vilkarsvurderingRepository.kopierVilkaarsvurdering(nyVilkaarsvurdering, kopiertFraId)
        return hent(nyVilkaarsvurdering.behandlingId)!!
    }

    override fun slettVilkaarsvurderingResultat(behandlingId: UUID): Vilkaarsvurdering =
        vilkarsvurderingRepository.slettVilkaarsvurderingResultat(behandlingId)

    override fun lagreVilkaarsvurderingResultat(
        behandlingId: UUID,
        virkningstidspunkt: LocalDate,
        resultat: VilkaarsvurderingResultat,
    ): Vilkaarsvurdering {
        val vv = hent(behandlingId)!!
        return vilkarsvurderingRepository.lagreVilkaarsvurderingResultatvanlig(virkningstidspunkt, resultat, vv)
    }

    override fun oppdaterVurderingPaaVilkaar(
        behandlingId: UUID,
        vurdertVilkaar: VurdertVilkaar,
    ): Vilkaarsvurdering = vilkarsvurderingRepository.oppdaterVurderingPaaVilkaar(behandlingId, vurdertVilkaar)

    override fun slettVilkaarResultat(
        behandlingId: UUID,
        vilkaarId: UUID,
    ): Vilkaarsvurdering = vilkarsvurderingRepository.slettVilkaarResultat(behandlingId, vilkaarId)

    override fun oppdaterGrunnlagsversjon(
        behandlingId: UUID,
        grunnlagVersjon: Long,
    ) = vilkarsvurderingRepository.oppdaterGrunnlagsversjon(behandlingId, grunnlagVersjon)

    override fun slettVilkaarvurdering(
        behandlingId: UUID, // Kan fjernes når vi er ferdig med migreringen
        vilkaarsvurderingId: UUID,
    ) {
        vilkarsvurderingRepository.slettVilkaarvurdering(vilkaarsvurderingId)
    }
}
