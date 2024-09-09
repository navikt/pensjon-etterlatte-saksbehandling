package no.nav.etterlatte.vilkaarsvurdering.dao

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vilkaarsvurdering.OppdaterVurdertVilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaar
import no.nav.etterlatte.libs.vilkaarsvurdering.VurdertVilkaarsvurderingDto
import no.nav.etterlatte.vilkaarsvurdering.OpprettVilkaarsvurderingFraBehandling
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class VilkaarsvurderingRepository(
    private val vilkaarsvurderingKlientDaoImpl: VilkaarsvurderingKlientDao,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // TODO: se over struktur med runblocking her
    fun hent(behandlingId: UUID): Vilkaarsvurdering? = runBlocking { vilkaarsvurderingKlientDaoImpl.hent(behandlingId) }

    // TODO: trenger å gjøre behandlingKlient.hentBehandling(behandlingId, bruker).sak, o.l.
    fun hentMigrertYrkesskadefordel(
        behandlingId: UUID,
        sakId: SakId,
    ): Boolean = runBlocking { vilkaarsvurderingKlientDaoImpl.erMigrertYrkesskadefordel(behandlingId, sakId).migrertYrkesskadefordel }

    fun opprettVilkaarsvurdering(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering =
        runBlocking {
            vilkaarsvurderingKlientDaoImpl.opprettVilkaarsvurdering(vilkaarsvurdering)
        }

    fun kopierVilkaarsvurdering(
        nyVilkaarsvurdering: Vilkaarsvurdering,
        kopiertFraId: UUID,
    ): Vilkaarsvurdering {
        opprettVilkaarsvurdering(nyVilkaarsvurdering)
        runBlocking {
            vilkaarsvurderingKlientDaoImpl.kopierVilkaarsvurdering(
                OpprettVilkaarsvurderingFraBehandling(kopiertFraId, nyVilkaarsvurdering),
            )
        }
        return hent(nyVilkaarsvurdering.behandlingId)!!
    }

    fun slettVilkaarsvurderingResultat(behandlingId: UUID): Vilkaarsvurdering =
        runBlocking {
            vilkaarsvurderingKlientDaoImpl.slettVilkaarsvurderingResultat(behandlingId)
        }

    fun lagreVilkaarsvurderingResultat(
        behandlingId: UUID,
        virkningstidspunkt: LocalDate,
        resultat: VilkaarsvurderingResultat,
    ): Vilkaarsvurdering {
        val vv = hent(behandlingId)!!
        return runBlocking {
            vilkaarsvurderingKlientDaoImpl.lagreVilkaarsvurderingResultatvanlig(
                behandlingId,
                VurdertVilkaarsvurderingDto(virkningstidspunkt, resultat, vv),
            )
        }
    }

    fun oppdaterVurderingPaaVilkaar(
        behandlingId: UUID,
        vurdertVilkaar: VurdertVilkaar,
    ): Vilkaarsvurdering =
        runBlocking {
            vilkaarsvurderingKlientDaoImpl.oppdaterVurderingPaaVilkaar(OppdaterVurdertVilkaar(behandlingId, vurdertVilkaar))
        }

    fun slettVilkaarResultat(
        behandlingId: UUID,
        vilkaarId: UUID,
    ): Vilkaarsvurdering = runBlocking { vilkaarsvurderingKlientDaoImpl.slettVurderingPaaVilkaar(behandlingId, vilkaarId) }

    fun oppdaterGrunnlagsversjon(
        behandlingId: UUID,
        grunnlagVersjon: Long,
    ) = runBlocking { vilkaarsvurderingKlientDaoImpl.oppdaterGrunnlagsversjon(behandlingId, grunnlagVersjon) }

    fun slettVilkaarvurdering(
        behandlingId: UUID,
        vilkaarsvurderingId: UUID,
    ) = runBlocking {
        vilkaarsvurderingKlientDaoImpl.slettVilkaarsvurdering(behandlingId, vilkaarsvurderingId)
    }
}
