package no.nav.etterlatte.vilkaarsvurdering.dao

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.database.Transactions
import no.nav.etterlatte.libs.database.tidspunkt
import no.nav.etterlatte.libs.vilkaarsvurdering.VurdertVilkaarsvurderingDto
import no.nav.etterlatte.vilkaarsvurdering.OpprettVilkaarsvurderingFraBehandling
import org.slf4j.LoggerFactory
import vilkaarsvurdering.OppdaterVurdertVilkaar
import vilkaarsvurdering.Vilkaarsvurdering
import vilkaarsvurdering.VurdertVilkaar
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class VilkaarsvurderingRepository(
    private val vilkaarsvurderingKlientDao: VilkaarsvurderingKlientDao,
    private val delvilkaarRepository: DelvilkaarRepository,
) : Transactions<VilkaarsvurderingRepository> {
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

    private fun hentNonNull(behandlingId: UUID): Vilkaarsvurdering =
        hent(behandlingId) ?: throw RuntimeException("Fant ikke vilkårsvurdering for $behandlingId")

    private fun hentVilkaar(
        vilkaarsvurderingId: UUID,
        session: Session,
    ): List<Vilkaar> =
        queryOf(Queries.HENT_VILKAAR, mapOf("vilkaarsvurdering_id" to vilkaarsvurderingId))
            .let { query ->
                session
                    .run(
                        query.map { row -> row.toVilkaarWrapper() }.asList,
                    ).groupBy { it.vilkaarId }
                    .map { (vilkaarId, alleDelvilkaar) ->
                        val hovedvilkaar = alleDelvilkaar.first { it.hovedvilkaar }
                        val unntaksvilkaar = alleDelvilkaar.filter { !it.hovedvilkaar }.map { it.delvilkaar }
                        Vilkaar(
                            id = vilkaarId,
                            hovedvilkaar = hovedvilkaar.delvilkaar,
                            unntaksvilkaar = unntaksvilkaar,
                            vurdering = hovedvilkaar.vurderingData,
                        )
                    }.sortedBy { it.hovedvilkaar.type.rekkefoelge }
            }

    private fun lagreVilkaarsvurdering(
        vilkaarsvurdering: Vilkaarsvurdering,
        tx: TransactionalSession,
    ): UUID {
        queryOf(
            statement = Queries.LAGRE_VILKAARSVURDERING,
            paramMap =
                mapOf(
                    "id" to vilkaarsvurdering.id,
                    "behandling_id" to vilkaarsvurdering.behandlingId,
                    "virkningstidspunkt" to vilkaarsvurdering.virkningstidspunkt.atDay(1),
                    "grunnlag_versjon" to vilkaarsvurdering.grunnlagVersjon,
                ),
        ).let { tx.run(it.asUpdate) }

        return vilkaarsvurdering.id
    }

    private fun lagreVilkaar(
        vilkaarsvurderingId: UUID,
        vilkaar: Vilkaar,
        tx: TransactionalSession,
    ): UUID {
        queryOf(
            statement = Queries.LAGRE_VILKAAR,
            paramMap =
                mapOf(
                    "id" to vilkaar.id,
                    "vilkaarsvurdering_id" to vilkaarsvurderingId,
                    "resultat_kommentar" to vilkaar.vurdering?.kommentar,
                    "resultat_tidspunkt" to
                        vilkaar.vurdering
                            ?.tidspunkt
                            ?.toTidspunkt()
                            ?.toTimestamp(),
                    "resultat_saksbehandler" to vilkaar.vurdering?.saksbehandler,
                ),
        ).let { tx.run(it.asUpdate) }

        return vilkaar.id
    }

    private fun Row.toVilkaarsvurdering(vilkaar: List<Vilkaar>) =
        Vilkaarsvurdering(
            id = uuid("id"),
            behandlingId = uuid("behandling_id"),
            grunnlagVersjon = long("grunnlag_versjon"),
            virkningstidspunkt = YearMonth.from(localDate("virkningstidspunkt")),
            vilkaar = vilkaar,
            resultat =
                stringOrNull("resultat_utfall")?.let { utfall ->
                    VilkaarsvurderingResultat(
                        utfall = VilkaarsvurderingUtfall.valueOf(utfall),
                        kommentar = stringOrNull("resultat_kommentar"),
                        tidspunkt = tidspunkt("resultat_tidspunkt").toLocalDatetimeUTC(),
                        saksbehandler = string("resultat_saksbehandler"),
                    )
                },
        )

    private fun Row.toVilkaarWrapper() =
        VilkaarDataWrapper(
            vilkaarId = uuid("id"),
            hovedvilkaar = boolean("hovedvilkaar"),
            delvilkaar = this.toDelvilkaar(),
            vurderingData =
                stringOrNull("resultat_kommentar")?.let { kommentar ->
                    VilkaarVurderingData(
                        kommentar = kommentar,
                        tidspunkt = tidspunkt("resultat_tidspunkt").toLocalDatetimeUTC(),
                        saksbehandler = string("resultat_saksbehandler"),
                    )
                },
        )

    data class VilkaarDataWrapper(
        val vilkaarId: UUID,
        val vurderingData: VilkaarVurderingData?,
        val hovedvilkaar: Boolean,
        val delvilkaar: Delvilkaar,
    )

    private object Queries {
        const val LAGRE_VILKAARSVURDERING = """
            INSERT INTO vilkaarsvurdering(id, behandling_id, virkningstidspunkt, grunnlag_versjon) 
            VALUES(:id, :behandling_id, :virkningstidspunkt, :grunnlag_versjon)
        """

        const val LAGRE_VILKAARSVURDERING_KILDE = """
            INSERT INTO vilkaarsvurdering_kilde(vilkaarsvurdering_id, kopiert_fra_vilkaarsvurdering_id) VALUES(:id, :kopiert_fra)
        """

        const val LAGRE_VILKAAR = """
            INSERT INTO vilkaar(id, vilkaarsvurdering_id, resultat_kommentar, resultat_tidspunkt, resultat_saksbehandler) 
            VALUES(:id, :vilkaarsvurdering_id, :resultat_kommentar, :resultat_tidspunkt, :resultat_saksbehandler) 
        """

        const val LAGRE_VILKAARSVURDERING_RESULTAT = """
            UPDATE vilkaarsvurdering
            SET virkningstidspunkt = :virkningstidspunkt, 
                resultat_utfall = :resultat_utfall, 
                resultat_kommentar = :resultat_kommentar, 
                resultat_tidspunkt = :resultat_tidspunkt, 
                resultat_saksbehandler = :resultat_saksbehandler 
            WHERE id = :id
        """

        const val LAGRE_VILKAAR_RESULTAT = """
            UPDATE vilkaar
            SET resultat_kommentar = :resultat_kommentar, resultat_tidspunkt = :resultat_tidspunkt, 
                resultat_saksbehandler = :resultat_saksbehandler   
            WHERE id = :id
        """

        const val HENT_VILKAARSVURDERING = """
            SELECT id, behandling_id, virkningstidspunkt, grunnlag_versjon, resultat_utfall, 
                resultat_kommentar, resultat_tidspunkt, resultat_saksbehandler 
            FROM vilkaarsvurdering WHERE behandling_id = :behandling_id
        """

        const val HENT_MIGRERT_YRKESSKADE = """
            SELECT sak_id FROM migrert_yrkesskade WHERE sak_id = :sak_id
        """

        const val HENT_VILKAAR = """
            SELECT v.id,
                   v.resultat_kommentar,
                   v.resultat_tidspunkt,
                   v.resultat_saksbehandler,
                   dv.vilkaar_id,
                   dv.vilkaar_type,
                   dv.hovedvilkaar,
                   dv.tittel,
                   dv.beskrivelse,
                   dv.spoersmaal,
                   dv.paragraf,
                   dv.ledd,
                   dv.bokstav,
                   dv.lenke,
                   dv.resultat
            FROM vilkaar v
              JOIN delvilkaar dv on dv.vilkaar_id = v.id
            WHERE v.vilkaarsvurdering_id = :vilkaarsvurdering_id
        """

        const val SLETT_VILKAARSVURDERING_RESULTAT = """
            UPDATE vilkaarsvurdering
            SET resultat_utfall = null, resultat_kommentar = null, resultat_tidspunkt = null, 
                resultat_saksbehandler = null 
            WHERE id = :id
        """

        const val SLETT_VILKAAR_RESULTAT = """
            UPDATE vilkaar
            SET resultat_kommentar = null, resultat_tidspunkt = null, resultat_saksbehandler = null   
            WHERE id = :id
        """
        const val SLETT_VILKAARSVURDERING_KILDE = """
            DELETE FROM vilkaarsvurdering_kilde
            WHERE vilkaarsvurdering_id = :vilkaarsvurdering_id
        """

        const val SLETT_VILKAARSVURDERING = """
            DELETE FROM vilkaarsvurdering
            WHERE id = :id
        """

        const val OPPDATER_GRUNNLAGSVERSJON = """
            UPDATE vilkaarsvurdering
            SET grunnlag_versjon = :grunnlag_versjon 
            WHERE id = :id
        """
    }
}
