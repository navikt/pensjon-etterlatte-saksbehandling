package no.nav.etterlatte.vilkaarsvurdering

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Lovreferanse
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Unntaksvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarOpplysningType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarTypeOgUtfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarVurderingData
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsgrunnlag
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaar
import java.sql.Timestamp
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

interface VilkaarsvurderingRepository {
    fun hent(behandlingId: UUID): VilkaarsvurderingIntern?
    fun opprettVilkaarsvurdering(vilkaarsvurdering: VilkaarsvurderingIntern): VilkaarsvurderingIntern
    fun lagreVilkaarsvurderingResultat(behandlingId: UUID, resultat: VilkaarsvurderingResultat): VilkaarsvurderingIntern
    fun slettVilkaarsvurderingResultat(behandlingId: UUID): VilkaarsvurderingIntern
    fun lagreVilkaarResultat(
        behandlingId: UUID,
        vurdertVilkaar: VurdertVilkaar
    ): VilkaarsvurderingIntern

    fun slettVilkaarResultat(behandlingId: UUID, vilkaarId: UUID): VilkaarsvurderingIntern
    fun slettVilkaarsvurderingerISak(sakId: Long)
}

class VilkaarsvurderingRepositoryImpl(private val ds: DataSource) : VilkaarsvurderingRepository {

    override fun hent(behandlingId: UUID): VilkaarsvurderingIntern? =
        using(sessionOf(ds)) { session ->
            queryOf(Queries.hentVilkaarsvurdering, mapOf("behandling_id" to behandlingId))
                .let { query ->
                    session.run(
                        query.map { row ->
                            val vilkaarsvurderingId = row.uuid("id")
                            row.toVilkaarsvurderingIntern(hentVilkaar(vilkaarsvurderingId, session))
                        }.asSingle
                    )
                }
        }

    override fun opprettVilkaarsvurdering(vilkaarsvurdering: VilkaarsvurderingIntern): VilkaarsvurderingIntern =
        using(sessionOf(ds)) { session ->
            session.transaction { tx ->
                val vilkaarsvurderingId = lagreVilkaarsvurdering(vilkaarsvurdering, tx)
                vilkaarsvurdering.vilkaar.forEach { vilkaar ->
                    val vilkaarId = lagreVilkaar(vilkaarsvurderingId, vilkaar, tx)
                    vilkaar.grunnlag?.forEach { grunnlag ->
                        lagreGrunnlag(vilkaarId, grunnlag, tx)
                    }
                    lagreDelvilkaar(vilkaarId, vilkaar.hovedvilkaar, true, tx)
                    vilkaar.unntaksvilkaar?.forEach { unntaksvilkaar ->
                        lagreDelvilkaar(vilkaarId, unntaksvilkaar, false, tx)
                    }
                }
            }
        }.let { hentNonNull(vilkaarsvurdering.behandlingId) }

    override fun lagreVilkaarsvurderingResultat(
        behandlingId: UUID,
        resultat: VilkaarsvurderingResultat
    ): VilkaarsvurderingIntern {
        using(sessionOf(ds)) { session ->
            val vilkaarsvurdering = hentNonNull(behandlingId)

            queryOf(
                statement = Queries.lagreVilkaarsvurderingResultat,
                paramMap = mapOf(
                    "id" to vilkaarsvurdering.id,
                    "resultat_utfall" to resultat.utfall.name,
                    "resultat_kommentar" to resultat.kommentar,
                    "resultat_tidspunkt" to Timestamp.valueOf(resultat.tidspunkt),
                    "resultat_saksbehandler" to resultat.saksbehandler
                )
            ).let { session.run(it.asUpdate) }
        }

        return hentNonNull(behandlingId)
    }

    override fun slettVilkaarsvurderingResultat(behandlingId: UUID): VilkaarsvurderingIntern {
        using(sessionOf(ds)) { session ->
            val vilkaarsvurdering = hentNonNull(behandlingId)

            queryOf(Queries.slettVilkaarsvurderingResultat, mapOf("id" to vilkaarsvurdering.id))
                .let { session.run(it.asUpdate) }
        }

        return hentNonNull(behandlingId)
    }

    override fun lagreVilkaarResultat(
        behandlingId: UUID,
        vurdertVilkaar: VurdertVilkaar
    ): VilkaarsvurderingIntern {
        using(sessionOf(ds)) { session ->
            session.transaction { tx ->
                queryOf(
                    statement = Queries.lagreVilkaarResultat,
                    paramMap = mapOf(
                        "id" to vurdertVilkaar.vilkaarId,
                        "resultat_kommentar" to vurdertVilkaar.vurdering.kommentar,
                        "resultat_tidspunkt" to Timestamp.valueOf(vurdertVilkaar.vurdering.tidspunkt),
                        "resultat_saksbehandler" to vurdertVilkaar.vurdering.saksbehandler
                    )
                ).let { tx.run(it.asUpdate) }

                lagreDelvilkaarResultat(vurdertVilkaar.vilkaarId, vurdertVilkaar.hovedvilkaar, tx)
                vurdertVilkaar.unntaksvilkaar?.let { vilkaar ->
                    lagreDelvilkaarResultat(vurdertVilkaar.vilkaarId, vilkaar, tx)
                } ?: run {
                    // Alle unntaksvilkår settes til IKKE_OPPFYLT hvis ikke hovedvilkår eller unntaksvilkår er oppfylt
                    if (vurdertVilkaar.hovedvilkaarOgUnntaksvilkaarIkkeOppfylt()) {
                        queryOf(
                            statement = Queries.lagreDelvilkaarResultatIngenOppfylt,
                            paramMap = mapOf(
                                "vilkaar_id" to vurdertVilkaar.vilkaarId,
                                "resultat" to Utfall.IKKE_OPPFYLT.name
                            )
                        ).let { tx.run(it.asUpdate) }
                    }
                }
            }
        }

        return hentNonNull(behandlingId)
    }

    private fun lagreDelvilkaarResultat(
        vilkaarId: UUID,
        vilkaarTypeOgUtfall: VilkaarTypeOgUtfall,
        tx: TransactionalSession
    ) {
        queryOf(
            statement = Queries.lagreDelvilkaarResultat,
            paramMap = mapOf(
                "vilkaar_id" to vilkaarId,
                "vilkaar_type" to vilkaarTypeOgUtfall.type.name,
                "resultat" to vilkaarTypeOgUtfall.resultat.name
            )
        ).let { tx.run(it.asUpdate) }
    }

    override fun slettVilkaarResultat(
        behandlingId: UUID,
        vilkaarId: UUID
    ): VilkaarsvurderingIntern =
        using(sessionOf(ds)) { session ->
            session.transaction { tx ->
                queryOf(Queries.slettVilkaarResultat, mapOf("id" to vilkaarId))
                    .let { tx.run(it.asUpdate) }

                queryOf(Queries.slettDelvilkaarResultat, mapOf("vilkaar_id" to vilkaarId))
                    .let { tx.run(it.asUpdate) }
            }
        }.let { hentNonNull(behandlingId) }

    override fun slettVilkaarsvurderingerISak(sakId: Long) {
        using(sessionOf(ds)) { session ->
            queryOf(Queries.slettVilkaarsvurdering, mapOf("sak_id" to sakId))
                .let { session.run(it.asUpdate) }
        }
    }

    private fun hentNonNull(behandlingId: UUID): VilkaarsvurderingIntern =
        hent(behandlingId) ?: throw RuntimeException("Fant ikke vilkårsvurdering for $behandlingId")

    private fun hentVilkaar(vilkaarsvurderingId: UUID, session: Session): List<Vilkaar> =
        queryOf(Queries.hentVilkaar, mapOf("vilkaarsvurdering_id" to vilkaarsvurderingId))
            .let { query ->
                session.run(
                    query.map { row ->
                        val vilkaarId = row.uuid("id")
                        row.toVilkaar(
                            hovedvilkaar = hentDelvilkaar(vilkaarId, true, session).first(),
                            unntaksvilkaar = hentDelvilkaar(vilkaarId, false, session),
                            grunnlag = hentGrunnlag(vilkaarId, session)
                        )
                    }.asList
                ).sortedBy { it.hovedvilkaar.type.rekkefoelge }
            }

    private fun hentDelvilkaar(vilkaarId: UUID, hovedvilkaar: Boolean, session: Session): List<Delvilkaar> =
        queryOf(Queries.hentDelvilkaar, mapOf("vilkaar_id" to vilkaarId, "hovedvilkaar" to hovedvilkaar))
            .let { query -> session.run(query.map { row -> row.toDelvilkaar() }.asList) }
            .sortedBy { it.type.rekkefoelge }

    private fun hentGrunnlag(vilkaarId: UUID, session: Session): List<Vilkaarsgrunnlag<JsonNode>> =
        queryOf(Queries.hentGrunnlag, mapOf("vilkaar_id" to vilkaarId))
            .let { session.run(it.map { row -> row.toGrunnlag() }.asList) }

    private fun lagreVilkaarsvurdering(
        vilkaarsvurdering: VilkaarsvurderingIntern,
        tx: TransactionalSession
    ): UUID {
        val id = UUID.randomUUID()
        queryOf(
            statement = Queries.lagreVilkaarsvurdering,
            paramMap = mapOf(
                "id" to id,
                "behandling_id" to vilkaarsvurdering.behandlingId,
                "virkningstidspunkt" to vilkaarsvurdering.virkningstidspunkt.atDay(1),
                "sak_id" to vilkaarsvurdering.grunnlagsmetadata.sakId,
                "grunnlag_versjon" to vilkaarsvurdering.grunnlagsmetadata.versjon
            )
        ).let { tx.run(it.asUpdate) }

        return id
    }

    private fun lagreVilkaar(vilkaarsvurderingId: UUID, vilkaar: Vilkaar, tx: TransactionalSession): UUID {
        val id = UUID.randomUUID()
        queryOf(
            statement = Queries.lagreVilkaar,
            paramMap = mapOf(
                "id" to id,
                "vilkaarsvurdering_id" to vilkaarsvurderingId,
                "resultat_kommentar" to vilkaar.vurdering?.kommentar,
                "resultat_tidspunkt" to vilkaar.vurdering?.tidspunkt?.let { Timestamp.valueOf(it) },
                "resultat_saksbehandler" to vilkaar.vurdering?.saksbehandler
            )
        ).let { tx.run(it.asUpdate) }

        return id
    }

    private fun lagreDelvilkaar(
        vilkaarId: UUID,
        unntaksvilkaar: Unntaksvilkaar,
        hovedvilkaar: Boolean,
        tx: TransactionalSession
    ) = queryOf(
        statement = Queries.lagreDelvilkaar,
        paramMap = mapOf(
            "vilkaar_id" to vilkaarId,
            "vilkaar_type" to unntaksvilkaar.type.name,
            "hovedvilkaar" to hovedvilkaar,
            "tittel" to unntaksvilkaar.tittel,
            "beskrivelse" to unntaksvilkaar.beskrivelse,
            "paragraf" to unntaksvilkaar.lovreferanse.paragraf,
            "ledd" to unntaksvilkaar.lovreferanse.ledd,
            "bokstav" to unntaksvilkaar.lovreferanse.paragraf,
            "lenke" to unntaksvilkaar.lovreferanse.lenke,
            "resultat" to unntaksvilkaar.resultat?.name
        )
    ).let { tx.run(it.asExecute) }

    private fun lagreGrunnlag(
        vilkaarId: UUID,
        grunnlag: Vilkaarsgrunnlag<out Any?>,
        tx: TransactionalSession
    ) = queryOf(
        statement = Queries.lagreGrunnlag,
        paramMap = mapOf(
            "vilkaar_id" to vilkaarId,
            "grunnlag_id" to grunnlag.id,
            "opplysning_type" to grunnlag.opplysningsType.name,
            "kilde" to grunnlag.kilde.toJson(),
            "opplysning" to grunnlag.opplysning?.toJson()
        )
    ).let { tx.run(it.asExecute) }

    private fun Row.toVilkaarsvurderingIntern(vilkaar: List<Vilkaar>) =
        VilkaarsvurderingIntern(
            id = uuidOrNull("id"),
            behandlingId = uuid("behandling_id"),
            vilkaar = vilkaar,
            resultat = stringOrNull("resultat_utfall")?.let { utfall ->
                VilkaarsvurderingResultat(
                    utfall = VilkaarsvurderingUtfall.valueOf(utfall),
                    kommentar = stringOrNull("resultat_kommentar"),
                    tidspunkt = sqlTimestamp("resultat_tidspunkt").toLocalDateTime(),
                    saksbehandler = string("resultat_saksbehandler")
                )
            },
            virkningstidspunkt = YearMonth.from(localDate("virkningstidspunkt")),
            grunnlagsmetadata = Metadata(
                sakId = long("sak_id"),
                versjon = long("grunnlag_versjon")
            )
        )

    private fun Row.toVilkaar(
        hovedvilkaar: Delvilkaar,
        unntaksvilkaar: List<Delvilkaar>,
        grunnlag: List<Vilkaarsgrunnlag<JsonNode>>
    ) =
        Vilkaar(
            id = uuid("id"),
            hovedvilkaar = hovedvilkaar,
            unntaksvilkaar = unntaksvilkaar,
            vurdering = stringOrNull("resultat_kommentar")?.let { kommentar ->
                VilkaarVurderingData(
                    kommentar = kommentar,
                    tidspunkt = sqlTimestamp("resultat_tidspunkt").toLocalDateTime(),
                    saksbehandler = string("resultat_saksbehandler")
                )
            },
            grunnlag = grunnlag
        )

    private fun Row.toDelvilkaar() =
        Delvilkaar(
            type = VilkaarType.valueOf(string("vilkaar_type")),
            tittel = string("tittel"),
            beskrivelse = stringOrNull("beskrivelse"),
            lovreferanse = Lovreferanse(
                paragraf = string("paragraf"),
                ledd = intOrNull("ledd"),
                bokstav = stringOrNull("bokstav"),
                lenke = stringOrNull("lenke")
            ),
            resultat = stringOrNull("resultat")?.let { Utfall.valueOf(it) }
        )

    private fun Row.toGrunnlag(): Vilkaarsgrunnlag<JsonNode> =
        Vilkaarsgrunnlag(
            id = uuid("id"),
            opplysningsType = string("opplysning_type").let { VilkaarOpplysningType.valueOf(it) },
            kilde = string("kilde").let { objectMapper.readValue(it) },
            opplysning = string("opplysning").let { objectMapper.readValue(it) }
        )

    private object Queries {
        const val lagreVilkaarsvurdering = """
            INSERT INTO vilkaarsvurdering(id, behandling_id, virkningstidspunkt, sak_id, grunnlag_versjon) 
            VALUES(:id, :behandling_id, :virkningstidspunkt, :sak_id, :grunnlag_versjon)
        """

        const val lagreVilkaar = """
            INSERT INTO vilkaar(id, vilkaarsvurdering_id, resultat_kommentar, resultat_tidspunkt, resultat_saksbehandler) 
            VALUES(:id, :vilkaarsvurdering_id, :resultat_kommentar, :resultat_tidspunkt, :resultat_saksbehandler) 
        """

        const val lagreDelvilkaar = """
            INSERT INTO delvilkaar(vilkaar_id, vilkaar_type, hovedvilkaar, tittel, beskrivelse, paragraf, ledd, bokstav, lenke, resultat) 
            VALUES(:vilkaar_id, :vilkaar_type, :hovedvilkaar, :tittel, :beskrivelse, :paragraf, :ledd, :bokstav, :lenke, :resultat)
        """

        const val lagreGrunnlag = """
            INSERT INTO grunnlag(vilkaar_id, grunnlag_id, opplysning_type, kilde, opplysning) 
            VALUES(:vilkaar_id, :grunnlag_id, :opplysning_type, :kilde::JSONB, :opplysning::JSONB)
        """

        const val lagreVilkaarsvurderingResultat = """
            UPDATE vilkaarsvurdering
            SET resultat_utfall = :resultat_utfall, resultat_kommentar = :resultat_kommentar, 
                resultat_tidspunkt = :resultat_tidspunkt, resultat_saksbehandler = :resultat_saksbehandler 
            WHERE id = :id
        """

        const val lagreVilkaarResultat = """
            UPDATE vilkaar
            SET resultat_kommentar = :resultat_kommentar, resultat_tidspunkt = :resultat_tidspunkt, 
                resultat_saksbehandler = :resultat_saksbehandler   
            WHERE id = :id
        """

        const val lagreDelvilkaarResultat = """
            UPDATE delvilkaar
            SET resultat = :resultat
            WHERE vilkaar_id = :vilkaar_id AND vilkaar_type = :vilkaar_type
        """

        const val lagreDelvilkaarResultatIngenOppfylt = """
            UPDATE delvilkaar
            SET resultat = :resultat
            WHERE vilkaar_id = :vilkaar_id AND hovedvilkaar != true
        """

        const val hentVilkaarsvurdering = """
            SELECT id, behandling_id, virkningstidspunkt, sak_id, grunnlag_versjon, resultat_utfall, 
                resultat_kommentar, resultat_tidspunkt, resultat_saksbehandler 
            FROM vilkaarsvurdering WHERE behandling_id = :behandling_id
        """

        const val hentVilkaar = """
            SELECT id, resultat_kommentar, resultat_tidspunkt, resultat_saksbehandler FROM vilkaar 
            WHERE vilkaarsvurdering_id = :vilkaarsvurdering_id
        """

        const val hentDelvilkaar = """
            SELECT vilkaar_id, vilkaar_type, hovedvilkaar, tittel, beskrivelse, paragraf, ledd, bokstav, lenke, resultat 
            FROM delvilkaar WHERE vilkaar_id = :vilkaar_id AND hovedvilkaar = :hovedvilkaar
        """

        const val hentGrunnlag = """
            SELECT id, opplysning_type, kilde, opplysning FROM grunnlag WHERE vilkaar_id = :vilkaar_id
        """

        const val slettVilkaarsvurderingResultat = """
            UPDATE vilkaarsvurdering
            SET resultat_utfall = null, resultat_kommentar = null, resultat_tidspunkt = null, 
                resultat_saksbehandler = null 
            WHERE id = :id
        """

        const val slettVilkaarResultat = """
            UPDATE vilkaar
            SET resultat_kommentar = null, resultat_tidspunkt = null, resultat_saksbehandler = null   
            WHERE id = :id
        """

        const val slettDelvilkaarResultat = """
            UPDATE delvilkaar
            SET resultat = null
            WHERE vilkaar_id = :vilkaar_id
        """

        const val slettVilkaarsvurdering = "DELETE FROM vilkaarsvurdering WHERE sak_id = :sak_id"
    }
}