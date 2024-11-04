package no.nav.etterlatte.vilkaarsvurdering.ektedao

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.dbutils.toTimestamp
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaar
import no.nav.etterlatte.libs.database.tidspunkt
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

fun <T> Session.hent(
    queryString: String,
    params: Map<String, Any> = mapOf(),
    converter: (r: Row) -> T,
) = queryOf(statement = queryString, paramMap = params)
    .let { query -> this.run(query.map { row -> converter.invoke(row) }.asSingle) }

fun Session.oppdater(
    query: String,
    params: Map<String, Any?>,
    ekstra: ((tx: Session) -> Unit)? = null,
) = queryOf(statement = query, paramMap = params)
    .let { this.run(it.asUpdate) }
    .also { ekstra?.invoke(this) }

class VilkaarsvurderingRepository(
    private val connectionAutoclosing: ConnectionAutoclosing,
    private val delvilkaarRepository: DelvilkaarRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hent(behandlingId: UUID): Vilkaarsvurdering? =
        connectionAutoclosing.hentKotliquerySession { session ->
            queryOf(Queries.HENT_VILKAARSVURDERING, mapOf("behandling_id" to behandlingId))
                .let { query ->
                    session.run(
                        query
                            .map { row ->
                                val vilkaarsvurderingId = row.uuid("id")
                                row.toVilkaarsvurdering(hentVilkaar(vilkaarsvurderingId, session))
                            }.asSingle,
                    )
                }
        }

    fun hentMigrertYrkesskadefordel(sakId: SakId) =
        connectionAutoclosing.hentKotliquerySession { session ->
            session.hent(
                queryString = Queries.HENT_MIGRERT_YRKESSKADE,
                params =
                    mapOf("sak_id" to sakId.sakId),
            ) {
                true
            } ?: false
        }

    fun opprettVilkaarsvurdering(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering {
        connectionAutoclosing.hentKotliquerySession { session ->
            opprettVilkaarsvurdering(vilkaarsvurdering, session)
        }

        return hentNonNull(vilkaarsvurdering.behandlingId)
    }

    fun lagreVilkaarsvurderingResultatvanlig(
        virkningstidspunkt: LocalDate,
        resultat: VilkaarsvurderingResultat,
        vilkaarsvurdering: Vilkaarsvurdering,
    ): Vilkaarsvurdering {
        connectionAutoclosing.hentKotliquerySession { session ->
            vilkaarsvurderingResultatQuery(vilkaarsvurdering.id, virkningstidspunkt, resultat).let {
                session.run(
                    it.asExecute,
                )
            }
        }

        return hentNonNull(vilkaarsvurdering.behandlingId)
    }

    fun kopierVilkaarsvurdering(
        nyVilkaarsvurdering: Vilkaarsvurdering, // TODO ny eller gammel?
        kopiertFraId: UUID,
    ): Vilkaarsvurdering {
        connectionAutoclosing.hentKotliquerySession { session ->
            lagreVilkaarsvurderingResultatKopiering(nyVilkaarsvurdering, session)
            opprettVilkaarsvurderingKilde(nyVilkaarsvurdering.id, kopiertFraId, session)
        }

        return hentNonNull(nyVilkaarsvurdering.behandlingId)
    }

    fun slettVilkaarsvurderingResultat(behandlingId: UUID): Vilkaarsvurdering {
        connectionAutoclosing.hentKotliquerySession { session ->
            val vilkaarsvurdering = hentNonNull(behandlingId)

            queryOf(Queries.SLETT_VILKAARSVURDERING_RESULTAT, mapOf("id" to vilkaarsvurdering.id))
                .let { session.run(it.asUpdate) }
        }

        return hentNonNull(behandlingId)
    }

    fun oppdaterGrunnlagsversjon(
        behandlingId: UUID,
        grunnlagVersjon: Long,
    ) {
        connectionAutoclosing.hentKotliquerySession { session ->
            val vilkaarsvurdering = hentNonNull(behandlingId)
            queryOf(
                statement = Queries.OPPDATER_GRUNNLAGSVERSJON,
                paramMap =
                    mapOf(
                        "id" to vilkaarsvurdering.id,
                        "grunnlag_versjon" to grunnlagVersjon,
                    ),
            ).let { session.run(it.asUpdate) }

            logger.info(
                "Grunnlagsversjon oppdatert til $grunnlagVersjon på vilkårsvurdering for behandling $behandlingId",
            )
        }
    }

    fun oppdaterVurderingPaaVilkaar(
        behandlingId: UUID,
        vurdertVilkaar: VurdertVilkaar,
    ): Vilkaarsvurdering {
        connectionAutoclosing.hentKotliquerySession { session ->
            session.oppdater(
                query = Queries.LAGRE_VILKAAR_RESULTAT,
                params =
                    mapOf(
                        "id" to vurdertVilkaar.vilkaarId,
                        "resultat_kommentar" to vurdertVilkaar.vurdering.kommentar,
                        "resultat_tidspunkt" to
                            vurdertVilkaar.vurdering.tidspunkt
                                .toTidspunkt()
                                .toTimestamp(),
                        "resultat_saksbehandler" to vurdertVilkaar.vurdering.saksbehandler,
                    ),
                ekstra = { delvilkaarRepository.oppdaterDelvilkaar(vurdertVilkaar, session) },
            )
        }
        return hentNonNull(behandlingId)
    }

    fun slettVilkaarResultat(
        behandlingId: UUID,
        vilkaarId: UUID,
    ): Vilkaarsvurdering =
        connectionAutoclosing
            .hentKotliquerySession { session ->
                queryOf(Queries.SLETT_VILKAAR_RESULTAT, mapOf("id" to vilkaarId))
                    .let { session.run(it.asUpdate) }

                delvilkaarRepository.slettDelvilkaarResultat(vilkaarId, session)
            }.let { hentNonNull(behandlingId) }

    fun slettVilkaarvurdering(vilkaarsvurderingId: UUID) =
        connectionAutoclosing.hentKotliquerySession { session ->
            queryOf(Queries.SLETT_VILKAARSVURDERING_KILDE, mapOf("vilkaarsvurdering_id" to vilkaarsvurderingId))
                .let { session.run(it.asUpdate) }
            queryOf(Queries.SLETT_VILKAARSVURDERING, mapOf("id" to vilkaarsvurderingId))
                .let { session.run(it.asUpdate) }
        } == 1

    private fun opprettVilkaarsvurdering(
        vilkaarsvurdering: Vilkaarsvurdering,
        tx: Session,
    ) {
        val vilkaarsvurderingId = lagreVilkaarsvurdering(vilkaarsvurdering, tx)
        vilkaarsvurdering.vilkaar.forEach { vilkaar ->
            val vilkaarId = lagreVilkaar(vilkaarsvurderingId, vilkaar, tx)
            delvilkaarRepository.opprettVilkaarsvurdering(vilkaarId, vilkaar, tx)
        }
    }

    private fun opprettVilkaarsvurderingKilde(
        vilkaarsvurderingId: UUID,
        kopiertFraId: UUID,
        tx: Session,
    ) {
        queryOf(
            statement = Queries.LAGRE_VILKAARSVURDERING_KILDE,
            paramMap =
                mapOf(
                    "id" to vilkaarsvurderingId,
                    "kopiert_fra" to kopiertFraId,
                ),
        ).let { tx.run(it.asUpdate) }
    }

    private fun hentNonNull(
        behandlingId: UUID,
        tx: Session,
    ): Vilkaarsvurdering {
        val params = mapOf("behandling_id" to behandlingId)
        return tx.hent(Queries.HENT_VILKAARSVURDERING, params) { row ->
            val vilkaarsvurderingId = row.uuid("id")
            row.toVilkaarsvurdering(hentVilkaar(vilkaarsvurderingId, tx))
        } ?: throw NullPointerException("Forventet å hente en vilkaarsvurdering men var null.")
    }

    private fun lagreVilkaarsvurderingResultatKopiering(
        nyVilkaarsvurdering: Vilkaarsvurdering,
        tx: Session,
    ): Vilkaarsvurdering {
        vilkaarsvurderingResultatQuery(
            nyVilkaarsvurdering.id,
            nyVilkaarsvurdering.virkningstidspunkt.atDay(1),
            nyVilkaarsvurdering.resultat!!,
        ).let {
            tx.run(it.asExecute)
        }
        return hentNonNull(nyVilkaarsvurdering.behandlingId, tx)
    }

    private fun vilkaarsvurderingResultatQuery(
        vilkaarsvurderingId: UUID,
        virkningstidspunkt: LocalDate,
        resultat: VilkaarsvurderingResultat,
    ) = queryOf(
        statement = Queries.LAGRE_VILKAARSVURDERING_RESULTAT,
        paramMap =
            mapOf(
                "id" to vilkaarsvurderingId,
                "virkningstidspunkt" to virkningstidspunkt,
                "resultat_utfall" to resultat.utfall.name,
                "resultat_kommentar" to resultat.kommentar,
                "resultat_tidspunkt" to resultat.tidspunkt.toTidspunkt().toTimestamp(),
                "resultat_saksbehandler" to resultat.saksbehandler,
            ),
    )

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
        tx: Session,
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
        tx: Session,
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
