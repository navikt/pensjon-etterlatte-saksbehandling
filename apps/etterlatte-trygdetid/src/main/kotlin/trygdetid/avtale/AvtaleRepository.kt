package no.nav.etterlatte.trygdetid.avtale

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import java.util.UUID
import javax.sql.DataSource

class AvtaleRepository(private val dataSource: DataSource) {
    fun hentAvtale(behandlingId: UUID): Trygdeavtale? =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement =
                    """
                    SELECT id, behandling_id, avtale_kode, avtale_dato_kode, avtale_kriteria_kode, kilde
                    FROM trygdeavtale
                    WHERE behandling_id = :behandlingId
                    """.trimIndent(),
                paramMap = mapOf("behandlingId" to behandlingId),
            ).let { query ->
                session.run(
                    query.map { row ->
                        row.toTrygdeavtale()
                    }.asSingle,
                )
            }
        }

    fun lagreAvtale(trygdeavtale: Trygdeavtale) {
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement =
                    """
                    UPDATE trygdeavtale
                    SET
                    avtale_kode = :avtaleKode,
                    avtale_dato_kode = :avtaleDatoKode,
                    avtale_kriteria_kode = :avtaleKriteriaKode,
                    kilde = :kilde
                    WHERE
                    id = :id AND behandling_id = :behandlingId
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to trygdeavtale.id,
                        "behandlingId" to trygdeavtale.behandlingId,
                        "avtaleKode" to trygdeavtale.avtaleKode,
                        "avtaleDatoKode" to trygdeavtale.avtaleDatoKode,
                        "avtaleKriteriaKode" to trygdeavtale.avtaleKriteriaKode,
                        "kilde" to trygdeavtale.kilde.toJson(),
                    ),
            ).let { query ->
                session.execute(query)
            }
        }
    }

    fun opprettAvtale(trygdeavtale: Trygdeavtale) {
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement =
                    """
                    INSERT INTO trygdeavtale(id, behandling_id, avtale_kode, avtale_dato_kode, avtale_kriteria_kode, kilde)
                    VALUES(:id, :behandlingId, :avtaleKode, :avtaleDatoKode, :avtaleKriteriaKode, :kilde)
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to trygdeavtale.id,
                        "behandlingId" to trygdeavtale.behandlingId,
                        "avtaleKode" to trygdeavtale.avtaleKode,
                        "avtaleDatoKode" to trygdeavtale.avtaleDatoKode,
                        "avtaleKriteriaKode" to trygdeavtale.avtaleKriteriaKode,
                        "kilde" to trygdeavtale.kilde.toJson(),
                    ),
            ).let { query ->
                session.execute(query)
            }
        }
    }

    private fun Row.toTrygdeavtale() =
        Trygdeavtale(
            id = uuid("id"),
            behandlingId = uuid("behandling_id"),
            avtaleKode = string("avtale_kode"),
            avtaleDatoKode = stringOrNull("avtale_dato_kode"),
            avtaleKriteriaKode = stringOrNull("avtale_kriteria_kode"),
            kilde = string("kilde").let { objectMapper.readValue(it) },
        )
}
