package no.nav.etterlatte.trygdetid

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
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
            )
                .let { query ->
                    session.run(
                        query.map { row ->
                            val trygdetidId = row.uuid("id")
                            val trygdetidGrunnlag = hentTrygdetidGrunnlag(trygdetidId)
                            row.toTrygdetid(trygdetidGrunnlag)
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
            )
                .let { query ->
                    session.run(
                        query.map { row -> row.toTrygdetidGrunnlag() }.asSingle
                    )
                }
        }

    fun opprettTrygdetid(behandlingId: UUID): Trygdetid =
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                queryOf(
                    statement = """
                        INSERT INTO trygdetid(id, behandling_id) VALUES(:id, :behandlingId)
                    """.trimIndent(),
                    paramMap = mapOf(
                        "id" to UUID.randomUUID(),
                        "behandlingId" to behandlingId
                    )
                )
                    .let { query -> tx.update(query) }
            }
        }.let { hentTrygdtidNotNull(behandlingId) }

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
                )
                    .let { query -> tx.update(query) }
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
                )
                    .let { query -> tx.update(query) }
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
                )
                    .let { query -> tx.update(query) }
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
            )
                .let { query ->
                    session.run(
                        query.map { row -> row.toTrygdetidGrunnlag() }.asList
                    )
                }
        }

    private fun hentTrygdtidNotNull(behandlingsId: UUID) =
        hentTrygdetid(behandlingsId)
            ?: throw Exception("Fant ikke trygdetid for $behandlingsId")

    private fun Row.toTrygdetid(trygdetidGrunnlag: List<TrygdetidGrunnlag>) =
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
            trygdetidGrunnlag = trygdetidGrunnlag
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
}