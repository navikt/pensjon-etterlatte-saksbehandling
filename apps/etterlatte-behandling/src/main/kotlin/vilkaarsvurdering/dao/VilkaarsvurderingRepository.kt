package no.nav.etterlatte.vilkaarsvurdering.dao

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.vilkaarsvurdering.VurdertVilkaarsvurderingDto
import no.nav.etterlatte.vilkaarsvurdering.OpprettVilkaarsvurderingFraBehandling
import org.slf4j.LoggerFactory
import vilkaarsvurdering.OppdaterVurdertVilkaar
import vilkaarsvurdering.Vilkaarsvurdering
import vilkaarsvurdering.VurdertVilkaar
import java.time.LocalDate
import java.util.UUID

class VilkaarsvurderingRepository(
    private val vilkaarsvurderingKlientDao: VilkaarsvurderingKlientDao,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // TODO: se over struktur med runblocking her
    fun hent(behandlingId: UUID): Vilkaarsvurdering? = runBlocking { vilkaarsvurderingKlientDao.hent(behandlingId) }

    // TODO: trenger å gjøre behandlingKlient.hentBehandling(behandlingId, bruker).sak, o.l.
    fun hentMigrertYrkesskadefordel(
        behandlingId: UUID,
        sakId: Long,
    ): Boolean = runBlocking { vilkaarsvurderingKlientDao.erMigrertYrkesskadefordel(behandlingId, sakId) }

    fun opprettVilkaarsvurdering(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering =
        runBlocking {
            vilkaarsvurderingKlientDao.opprettVilkaarsvurdering(vilkaarsvurdering)
        }

    fun kopierVilkaarsvurdering(
        nyVilkaarsvurdering: Vilkaarsvurdering,
        kopiertFraId: UUID,
    ): Vilkaarsvurdering {
        opprettVilkaarsvurdering(nyVilkaarsvurdering)
        runBlocking {
            vilkaarsvurderingKlientDao.kopierVilkaarsvurdering(
                OpprettVilkaarsvurderingFraBehandling(kopiertFraId, nyVilkaarsvurdering),
            )
        }
        return hent(nyVilkaarsvurdering.behandlingId)!!
    }

    fun slettVilkaarsvurderingResultat(behandlingId: UUID): Vilkaarsvurdering =
        runBlocking {
            vilkaarsvurderingKlientDao.slettVilkaarsvurderingResultat(behandlingId)
        }

    fun lagreVilkaarsvurderingResultat(
        behandlingId: UUID,
        virkningstidspunkt: LocalDate,
        resultat: VilkaarsvurderingResultat,
    ): Vilkaarsvurdering {
        val vv = hent(behandlingId)!!
        return runBlocking {
            vilkaarsvurderingKlientDao.lagreVilkaarsvurderingResultatvanlig(
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
            vilkaarsvurderingKlientDao.oppdaterVurderingPaaVilkaar(OppdaterVurdertVilkaar(behandlingId, vurdertVilkaar))
        }

    fun slettVilkaarResultat(
        behandlingId: UUID,
        vilkaarId: UUID,
    ): Vilkaarsvurdering = runBlocking { vilkaarsvurderingKlientDao.slettVurderingPaaVilkaar(behandlingId, vilkaarId) }

    fun oppdaterGrunnlagsversjon(
        behandlingId: UUID,
        grunnlagVersjon: Long,
    ) = runBlocking { vilkaarsvurderingKlientDao.oppdaterGrunnlagsversjon(behandlingId, grunnlagVersjon) }

    fun slettVilkaarvurdering(
        behandlingId: UUID,
        vilkaarsvurderingId: UUID,
    ) = runBlocking {
        vilkaarsvurderingKlientDao.slettVilkaarsvurdering(behandlingId, vilkaarsvurderingId)
    }
}
