package no.nav.etterlatte.trygdetid

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.toJsonNode
import java.util.*
import javax.sql.DataSource

class TrygdetidRepository(private val dataSource: DataSource) {

    fun hentTrygdetid(behandlingId: UUID): Trygdetid? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, behandling_id, trygdetid_nasjonal, trygdetid_fremtidig, trygdetid_total 
                    FROM trygdetid 
                    WHERE behandling_id = :behandlingId
                """.trimIndent(),
                paramMap = mapOf("behandlingId" to behandlingId)
            ).let { query ->
                session.run(
                    query.map { row ->
                        val trygdetidId = row.uuid("id")
                        val trygdetidGrunnlag = hentTrygdetidGrunnlag(trygdetidId)
                        val opplysninger = hentGrunnlagOpplysninger(trygdetidId)
                        row.toTrygdetid(trygdetidGrunnlag, opplysninger)
                    }.asSingle
                )
            }
        }

    fun hentEnkeltTrygdetidGrunnlag(trygdetidGrunnlagId: UUID): TrygdetidGrunnlag? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, trygdetid_id, type, bosted, periode_fra, periode_til, trygdetid, kilde 
                    FROM trygdetid_grunnlag
                    WHERE id = :trygdetidGrunnlagId
                """.trimIndent(),
                paramMap = mapOf("trygdetidGrunnlagId" to trygdetidGrunnlagId)
            ).let { query ->
                session.run(
                    query.map { row -> row.toTrygdetidGrunnlag() }.asSingle
                )
            }
        }

    fun opprettTrygdetid(behandlingId: UUID, opplysninger: Map<Opplysningstype, String?>): Trygdetid =
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                val trygdetidId = UUID.randomUUID()
                opprettTrygdetid(behandlingId, trygdetidId, tx)
                lagreOpplysningsgrunnlag(trygdetidId, opplysninger, tx)
            }
        }.let { hentTrygdtidNotNull(behandlingId) }

    private fun opprettTrygdetid(behandlingId: UUID, trygdetidId: UUID, tx: TransactionalSession) =
        queryOf(
            statement = """
                        INSERT INTO trygdetid(id, behandling_id) VALUES(:id, :behandlingId)
            """.trimIndent(),
            paramMap = mapOf(
                "id" to trygdetidId,
                "behandlingId" to behandlingId
            )
        ).let { query -> tx.update(query) }

    private fun lagreOpplysningsgrunnlag(
        trygdetidId: UUID?,
        opplysninger: Map<Opplysningstype, String?>,
        tx: TransactionalSession
    ) = opplysninger.forEach { (type, opplysning) ->
        queryOf(
            statement = """
                INSERT INTO opplysningsgrunnlag(id, trygdetid_id, type, opplysning)
                 VALUES(:id, :trygdetidId, :type, :opplysning::JSONB)
            """.trimIndent(),
            paramMap = mapOf(
                "id" to UUID.randomUUID(),
                "trygdetidId" to trygdetidId,
                "type" to type.name,
                "opplysning" to opplysning
            )
        ).let { query -> tx.update(query) }
    }

    fun opprettTrygdetidGrunnlag(behandlingId: UUID, trygdetidGrunnlag: TrygdetidGrunnlag): Trygdetid =
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                val trygdetid = hentTrygdtidNotNull(behandlingId)

                queryOf(
                    statement = """
                        INSERT INTO trygdetid_grunnlag(
                            id, 
                            trygdetid_id, 
                            type, bosted, 
                            periode_fra, 
                            periode_til,
                            trygdetid,
                            kilde
                        ) 
                        VALUES(:id, :trygdetidId, :type, :bosted, :periodeFra, :periodeTil, :trygdetid, :kilde)
                    """.trimIndent(),
                    paramMap = mapOf(
                        "id" to trygdetidGrunnlag.id,
                        "trygdetidId" to trygdetid.id,
                        "type" to trygdetidGrunnlag.type.name,
                        "bosted" to trygdetidGrunnlag.bosted,
                        "periodeFra" to trygdetidGrunnlag.periode.fra,
                        "periodeTil" to trygdetidGrunnlag.periode.til,
                        "trygdetid" to trygdetidGrunnlag.trygdetid,
                        "kilde" to trygdetidGrunnlag.kilde
                    )
                ).let { query -> tx.update(query) }
            }
        }.let { hentTrygdtidNotNull(behandlingId) }

    fun oppdaterTrygdetidGrunnlag(behandlingId: UUID, trygdetidGrunnlag: TrygdetidGrunnlag): Trygdetid =
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                queryOf(
                    statement = """
                        UPDATE trygdetid_grunnlag
                        SET bosted = :bosted,
                         periode_fra = :periodeFra,
                         periode_til = :periodeTil,
                         trygdetid = :trygdetid,
                         kilde = :kilde
                        WHERE id = :trygdetidGrunnlagId
                    """.trimIndent(),
                    paramMap = mapOf(
                        "trygdetidGrunnlagId" to trygdetidGrunnlag.id,
                        "bosted" to trygdetidGrunnlag.bosted,
                        "periodeFra" to trygdetidGrunnlag.periode.fra,
                        "periodeTil" to trygdetidGrunnlag.periode.til,
                        "trygdetid" to trygdetidGrunnlag.trygdetid,
                        "kilde" to trygdetidGrunnlag.kilde
                    )
                ).let { query -> tx.update(query) }
            }
        }.let { hentTrygdtidNotNull(behandlingId) }

    fun oppdaterBeregnetTrygdetid(behandlingId: UUID, beregnetTrygdetid: BeregnetTrygdetid): Trygdetid =
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                queryOf(
                    statement = """
                        UPDATE trygdetid 
                        SET trygdetid_nasjonal = :trygdetidNasjonal, trygdetid_fremtidig = :trygdetidFremtidig, 
                            trygdetid_total = :trygdetidTotal 
                        WHERE behandling_id = :behandlingId
                    """.trimIndent(),
                    paramMap = mapOf(
                        "behandlingId" to behandlingId,
                        "trygdetidNasjonal" to beregnetTrygdetid.nasjonal,
                        "trygdetidFremtidig" to beregnetTrygdetid.fremtidig,
                        "trygdetidTotal" to beregnetTrygdetid.total
                    )
                ).let { query -> tx.update(query) }
            }
        }.let { hentTrygdtidNotNull(behandlingId) }

    private fun hentTrygdetidGrunnlag(trygdetidId: UUID): List<TrygdetidGrunnlag> =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                    SELECT id, trygdetid_id, type, bosted, periode_fra, periode_til, trygdetid, kilde 
                    FROM trygdetid_grunnlag
                    WHERE trygdetid_id = :trygdetidId
                """.trimIndent(),
                paramMap = mapOf("trygdetidId" to trygdetidId)
            ).let { query ->
                session.run(
                    query.map { row -> row.toTrygdetidGrunnlag() }.asList
                )
            }
        }

    private fun hentGrunnlagOpplysninger(trygdetidId: UUID): List<Opplysningsgrunnlag> =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement = """
                SELECT id, trygdetid_id, type, opplysning
                FROM opplysningsgrunnlag
                WHERE trygdetid_id = :trygdetidId
                """.trimIndent(),
                paramMap = mapOf("trygdetidId" to trygdetidId)
            ).let { query ->
                session.run(
                    query.map { row -> row.toOpplysningsgrunnlag() }.asList
                )
            }
        }

    private fun hentTrygdtidNotNull(behandlingsId: UUID) =
        hentTrygdetid(behandlingsId)
            ?: throw Exception("Fant ikke trygdetid for $behandlingsId")

    private fun Row.toTrygdetid(trygdetidGrunnlag: List<TrygdetidGrunnlag>, opplysninger: List<Opplysningsgrunnlag>) =
        Trygdetid(
            id = uuid("id"),
            behandlingId = uuid("behandling_id"),
            beregnetTrygdetid = intOrNull("trygdetid_nasjonal")?.let {
                BeregnetTrygdetid(
                    nasjonal = it,
                    fremtidig = int("trygdetid_fremtidig"),
                    total = int("trygdetid_total")
                )
            },
            trygdetidGrunnlag = trygdetidGrunnlag,
            opplysninger = opplysninger
        )

    private fun Row.toTrygdetidGrunnlag() =
        TrygdetidGrunnlag(
            id = uuid("id"),
            type = string("type").let { TrygdetidType.valueOf(it) },
            bosted = string("bosted"),
            periode = TrygdetidPeriode(
                fra = localDate("periode_fra"),
                til = localDate("periode_til")
            ),
            trygdetid = int("trygdetid"),
            kilde = string("kilde")
        )

    private fun Row.toOpplysningsgrunnlag(): Opplysningsgrunnlag {
        return Opplysningsgrunnlag(
            id = uuid("id"),
            type = Opplysningstype.valueOf(string("type")),
            opplysning = string("opplysning").toJsonNode()
        )
    }
}