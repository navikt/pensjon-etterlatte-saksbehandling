package no.nav.etterlatte.vilkaarsvurdering

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarOpplysningType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsgrunnlag
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.database.hent
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.tidspunkt
import no.nav.etterlatte.libs.database.transaction
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

class VilkaarsvurderingRepository(private val ds: DataSource, private val delvilkaarRepository: DelvilkaarRepository) {
    fun hent(behandlingId: UUID): Vilkaarsvurdering? =
        using(sessionOf(ds)) { session ->
            queryOf(Queries.HENT_VILKAARSVURDERING, mapOf("behandling_id" to behandlingId))
                .let { query ->
                    session.run(
                        query.map { row ->
                            val vilkaarsvurderingId = row.uuid("id")
                            row.toVilkaarsvurdering(hentVilkaar(vilkaarsvurderingId, session))
                        }.asSingle,
                    )
                }
        }

    private fun hentNonNull(
        behandlingId: UUID,
        tx: TransactionalSession,
    ): Vilkaarsvurdering {
        val params = mapOf("behandling_id" to behandlingId)
        return tx.hent(Queries.HENT_VILKAARSVURDERING, params) { row ->
            val vilkaarsvurderingId = row.uuid("id")
            row.toVilkaarsvurdering(hentVilkaar(vilkaarsvurderingId, tx))
        } ?: throw NullPointerException("Forventet å hente en vilkaarsvurdering men var null.")
    }

    fun opprettVilkaarsvurdering(vilkaarsvurdering: Vilkaarsvurdering): Vilkaarsvurdering {
        ds.transaction { tx ->
            opprettVilkaarsvurdering(vilkaarsvurdering, tx)
        }

        return hentNonNull(vilkaarsvurdering.behandlingId)
    }

    fun kopierVilkaarsvurdering(
        nyVilkaarsvurdering: Vilkaarsvurdering,
        kopiertFraId: UUID,
    ): Vilkaarsvurdering {
        ds.transaction { tx ->
            opprettVilkaarsvurdering(nyVilkaarsvurdering, tx)
            lagreVilkaarsvurderingResultat(
                nyVilkaarsvurdering.behandlingId,
                nyVilkaarsvurdering.virkningstidspunkt.atDay(1),
                nyVilkaarsvurdering.resultat ?: throw IllegalStateException("Fant ikke vilkårsvurderingsresultat"),
                tx,
            )
            opprettVilkaarsvurderingKilde(nyVilkaarsvurdering.id, kopiertFraId, tx)
        }

        return hentNonNull(nyVilkaarsvurdering.behandlingId)
    }

    private fun opprettVilkaarsvurdering(
        vilkaarsvurdering: Vilkaarsvurdering,
        tx: TransactionalSession,
    ) {
        val vilkaarsvurderingId = lagreVilkaarsvurdering(vilkaarsvurdering, tx)
        vilkaarsvurdering.vilkaar.forEach { vilkaar ->
            val vilkaarId = lagreVilkaar(vilkaarsvurderingId, vilkaar, tx)
            vilkaar.grunnlag.forEach { grunnlag ->
                lagreGrunnlag(vilkaarId, grunnlag, tx)
            }
            delvilkaarRepository.opprettVilkaarsvurdering(vilkaarId, vilkaar, tx)
        }
    }

    private fun opprettVilkaarsvurderingKilde(
        vilkaarsvurderingId: UUID,
        kopiertFraId: UUID,
        tx: TransactionalSession,
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

    fun lagreVilkaarsvurderingResultat(
        behandlingId: UUID,
        virkningstidspunkt: LocalDate,
        resultat: VilkaarsvurderingResultat,
    ): Vilkaarsvurdering {
        using(sessionOf(ds)) { session ->
            val vilkaarsvurdering = hentNonNull(behandlingId)
            vilkaarsvurderingResultatQuery(vilkaarsvurdering, virkningstidspunkt, resultat).let {
                session.run(
                    it.asExecute,
                )
            }
        }

        return hentNonNull(behandlingId)
    }

    private fun lagreVilkaarsvurderingResultat(
        behandlingId: UUID,
        virkningstidspunkt: LocalDate,
        resultat: VilkaarsvurderingResultat,
        tx: TransactionalSession,
    ): Vilkaarsvurdering {
        val vilkaarsvurdering = hentNonNull(behandlingId, tx)
        vilkaarsvurderingResultatQuery(vilkaarsvurdering, virkningstidspunkt, resultat).let { tx.run(it.asExecute) }

        return hentNonNull(behandlingId, tx)
    }

    private fun vilkaarsvurderingResultatQuery(
        vilkaarsvurdering: Vilkaarsvurdering,
        virkningstidspunkt: LocalDate,
        resultat: VilkaarsvurderingResultat,
    ) = queryOf(
        statement = Queries.LAGRE_VILKAARSVURDERING_RESULTAT,
        paramMap =
            mapOf(
                "id" to vilkaarsvurdering.id,
                "virkningstidspunkt" to virkningstidspunkt,
                "resultat_utfall" to resultat.utfall.name,
                "resultat_kommentar" to resultat.kommentar,
                "resultat_tidspunkt" to resultat.tidspunkt.toTidspunkt().toTimestamp(),
                "resultat_saksbehandler" to resultat.saksbehandler,
            ),
    )

    fun slettVilkaarsvurderingResultat(behandlingId: UUID): Vilkaarsvurdering {
        using(sessionOf(ds)) { session ->
            val vilkaarsvurdering = hentNonNull(behandlingId)

            queryOf(Queries.SLETT_VILKAARSVURDERING_RESULTAT, mapOf("id" to vilkaarsvurdering.id))
                .let { session.run(it.asUpdate) }
        }

        return hentNonNull(behandlingId)
    }

    fun lagreVilkaarResultat(
        behandlingId: UUID,
        vurdertVilkaar: VurdertVilkaar,
    ): Vilkaarsvurdering {
        ds.transaction { tx ->
            tx.oppdater(
                query = Queries.LAGRE_VILKAAR_RESULTAT,
                params =
                    mapOf(
                        "id" to vurdertVilkaar.vilkaarId,
                        "resultat_kommentar" to vurdertVilkaar.vurdering.kommentar,
                        "resultat_tidspunkt" to vurdertVilkaar.vurdering.tidspunkt.toTidspunkt().toTimestamp(),
                        "resultat_saksbehandler" to vurdertVilkaar.vurdering.saksbehandler,
                    ),
                loggtekst = "Lagrer vilkårresultat",
                ekstra = { delvilkaarRepository.oppdaterDelvilkaar(vurdertVilkaar, tx) },
            )
        }
        return hentNonNull(behandlingId)
    }

    fun slettVilkaarResultat(
        behandlingId: UUID,
        vilkaarId: UUID,
    ): Vilkaarsvurdering =
        ds.transaction { tx ->
            queryOf(Queries.SLETT_VILKAAR_RESULTAT, mapOf("id" to vilkaarId))
                .let { tx.run(it.asUpdate) }

            delvilkaarRepository.slettDelvilkaarResultat(vilkaarId, tx)
        }.let { hentNonNull(behandlingId) }

    private fun hentNonNull(behandlingId: UUID): Vilkaarsvurdering =
        hent(behandlingId) ?: throw RuntimeException("Fant ikke vilkårsvurdering for $behandlingId")

    private fun hentVilkaar(
        vilkaarsvurderingId: UUID,
        session: Session,
    ): List<Vilkaar> =
        queryOf(Queries.HENT_VILKAAR, mapOf("vilkaarsvurdering_id" to vilkaarsvurderingId))
            .let { query ->
                session.run(
                    query.map { row ->
                        val vilkaarId = row.uuid("id")
                        row.toVilkaar(
                            hovedvilkaar = delvilkaarRepository.hentDelvilkaar(vilkaarId, true, session).first(),
                            unntaksvilkaar = delvilkaarRepository.hentDelvilkaar(vilkaarId, false, session),
                            grunnlag = hentGrunnlag(vilkaarId, session),
                        )
                    }.asList,
                ).sortedBy { it.hovedvilkaar.type.rekkefoelge }
            }

    private fun hentGrunnlag(
        vilkaarId: UUID,
        session: Session,
    ): List<Vilkaarsgrunnlag<JsonNode>> =
        queryOf(Queries.HENT_GRUNNLAG, mapOf("vilkaar_id" to vilkaarId))
            .let { session.run(it.map { row -> row.toGrunnlag() }.asList) }

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
                    "resultat_tidspunkt" to vilkaar.vurdering?.tidspunkt?.toTidspunkt()?.toTimestamp(),
                    "resultat_saksbehandler" to vilkaar.vurdering?.saksbehandler,
                ),
        ).let { tx.run(it.asUpdate) }

        return vilkaar.id
    }

    private fun lagreGrunnlag(
        vilkaarId: UUID,
        grunnlag: Vilkaarsgrunnlag<out Any?>,
        tx: TransactionalSession,
    ) = queryOf(
        statement = Queries.LAGRE_GRUNNLAG,
        paramMap =
            mapOf(
                "vilkaar_id" to vilkaarId,
                "grunnlag_id" to grunnlag.id,
                "opplysning_type" to grunnlag.opplysningsType.name,
                "kilde" to grunnlag.kilde.toJson(),
                "opplysning" to grunnlag.opplysning?.toJson(),
            ),
    ).let { tx.run(it.asExecute) }

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

    private fun Row.toVilkaar(
        hovedvilkaar: Delvilkaar,
        unntaksvilkaar: List<Delvilkaar>,
        grunnlag: List<Vilkaarsgrunnlag<JsonNode>>,
    ) = Vilkaar(
        id = uuid("id"),
        hovedvilkaar = hovedvilkaar,
        unntaksvilkaar = unntaksvilkaar,
        vurdering =
            stringOrNull("resultat_kommentar")?.let { kommentar ->
                VilkaarVurderingData(
                    kommentar = kommentar,
                    tidspunkt = tidspunkt("resultat_tidspunkt").toLocalDatetimeUTC(),
                    saksbehandler = string("resultat_saksbehandler"),
                )
            },
        grunnlag = grunnlag,
    )

    private fun Row.toGrunnlag(): Vilkaarsgrunnlag<JsonNode> =
        Vilkaarsgrunnlag(
            id = uuid("id"),
            opplysningsType = string("opplysning_type").let { VilkaarOpplysningType.valueOf(it) },
            kilde = string("kilde").let { objectMapper.readValue(it) },
            opplysning = string("opplysning").let { objectMapper.readValue(it) },
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

        const val LAGRE_GRUNNLAG = """
            INSERT INTO grunnlag(vilkaar_id, grunnlag_id, opplysning_type, kilde, opplysning) 
            VALUES(:vilkaar_id, :grunnlag_id, :opplysning_type, :kilde::JSONB, :opplysning::JSONB)
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

        const val HENT_VILKAAR = """
            SELECT id, resultat_kommentar, resultat_tidspunkt, resultat_saksbehandler FROM vilkaar 
            WHERE vilkaarsvurdering_id = :vilkaarsvurdering_id
        """

        const val HENT_GRUNNLAG = """
            SELECT id, opplysning_type, kilde, opplysning FROM grunnlag WHERE vilkaar_id = :vilkaar_id
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
    }
}
