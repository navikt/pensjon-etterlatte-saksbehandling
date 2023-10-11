package no.nav.etterlatte.beregning.grunnlag

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.objectMapper
import org.postgresql.util.PGobject
import java.util.UUID
import javax.sql.DataSource

class BeregningsGrunnlagRepository(private val dataSource: DataSource) {
    fun finnBarnepensjonGrunnlagForBehandling(id: UUID): BeregningsGrunnlag? =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    statement = finnBarnepensjonsGrunnlagForBehandling,
                    paramMap = mapOf("behandlings_id" to id),
                ).map { it.asBeregningsGrunnlagBP() }.asSingle,
            )
        }

    fun finnOmstillingstoenadGrunnlagForBehandling(id: UUID): BeregningsGrunnlagOMS? =
        using(
            sessionOf(dataSource),
        ) { session ->
            session.run(
                queryOf(
                    statement = finnOmstillingstoenadGrunnlagForBehandling,
                    paramMap = mapOf("behandlings_id" to id),
                ).map { it.asBeregningsGrunnlagOMS() }.asSingle,
            )
        }

    fun lagre(beregningsGrunnlag: BeregningsGrunnlag): Boolean {
        val query =
            if (finnBarnepensjonGrunnlagForBehandling(beregningsGrunnlag.behandlingId) == null) {
                lagreGrunnlagQuery
            } else {
                oppdaterGrunnlagQuery
            }

        val count =
            using(sessionOf(dataSource)) { session ->
                session.run(
                    queryOf(
                        statement = query,
                        paramMap =
                            mapOf<String, Any>(
                                "behandlings_id" to beregningsGrunnlag.behandlingId,
                                "soesken_med_i_beregning" to beregningsGrunnlag.soeskenMedIBeregning.somJsonb(),
                                "institusjonsopphold" to
                                    objectMapper.writeValueAsString(
                                        beregningsGrunnlag.institusjonsoppholdBeregningsgrunnlag,
                                    ),
                                "beregningsmetode" to
                                    objectMapper.writeValueAsString(
                                        beregningsGrunnlag.beregningsMetode,
                                    ),
                                "kilde" to beregningsGrunnlag.kilde.toJson(),
                            ),
                    ).asUpdate,
                )
            }

        return count > 0
    }

    fun lagreOMS(beregningsGrunnlagOMS: BeregningsGrunnlagOMS): Boolean {
        val query =
            if (finnOmstillingstoenadGrunnlagForBehandling(beregningsGrunnlagOMS.behandlingId) == null) {
                lagreGrunnlagQuery
            } else {
                oppdaterGrunnlagQuery
            }

        val count =
            using(sessionOf(dataSource)) { session ->
                session.run(
                    queryOf(
                        statement = query,
                        paramMap =
                            mapOf<String, Any>(
                                "behandlings_id" to beregningsGrunnlagOMS.behandlingId,
                                "institusjonsopphold" to
                                    objectMapper.writeValueAsString(
                                        beregningsGrunnlagOMS.institusjonsoppholdBeregningsgrunnlag,
                                    ),
                                "beregningsmetode" to
                                    objectMapper.writeValueAsString(
                                        beregningsGrunnlagOMS.beregningsMetode,
                                    ),
                                "kilde" to beregningsGrunnlagOMS.kilde.toJson(),
                            ),
                    ).asUpdate,
                )
            }

        return count > 0
    }

    companion object {
        val lagreGrunnlagQuery =
            """
            INSERT INTO beregningsgrunnlag
                (behandlings_id, soesken_med_i_beregning_perioder, institusjonsopphold, beregningsmetode, kilde)
            VALUES(
                :behandlings_id,
                :soesken_med_i_beregning,
                :institusjonsopphold,
                :beregningsmetode,
                :kilde
            )
            """.trimMargin()

        val oppdaterGrunnlagQuery =
            """
            UPDATE beregningsgrunnlag
            SET
                soesken_med_i_beregning_perioder = :soesken_med_i_beregning,
                institusjonsopphold = :institusjonsopphold,
                beregningsmetode = :beregningsmetode,
                kilde = :kilde
            WHERE behandlings_id = :behandlings_id
            """.trimMargin()

        val finnBarnepensjonsGrunnlagForBehandling =
            """
            SELECT behandlings_id, soesken_med_i_beregning_perioder, institusjonsopphold, beregningsmetode, kilde
            FROM beregningsgrunnlag
            WHERE behandlings_id = :behandlings_id
            """.trimIndent()

        val finnOmstillingstoenadGrunnlagForBehandling =
            """
            SELECT behandlings_id, institusjonsopphold, beregningsmetode, kilde
            FROM beregningsgrunnlag
            WHERE behandlings_id = :behandlings_id
            """.trimIndent()
    }
}

inline fun <reified T> T.somJsonb(): PGobject {
    val that = this
    val jsonObject =
        PGobject().apply {
            type = "json"
            value = objectMapper.writeValueAsString(that)
        }
    return jsonObject
}

private fun Row.asBeregningsGrunnlagBP(): BeregningsGrunnlag {
    return BeregningsGrunnlag(
        behandlingId = this.uuid("behandlings_id"),
        soeskenMedIBeregning = objectMapper.readValue(this.string("soesken_med_i_beregning_perioder")),
        institusjonsoppholdBeregningsgrunnlag =
            this.string("institusjonsopphold").let {
                objectMapper.readValue(
                    it,
                )
            },
        beregningsMetode =
            this.string("beregningsmetode").let {
                objectMapper.readValue(
                    it,
                )
            },
        kilde = objectMapper.readValue(this.string("kilde")),
    )
}

private fun Row.asBeregningsGrunnlagOMS(): BeregningsGrunnlagOMS {
    return BeregningsGrunnlagOMS(
        behandlingId = this.uuid("behandlings_id"),
        institusjonsoppholdBeregningsgrunnlag =
            this.string("institusjonsopphold").let {
                objectMapper.readValue(
                    it,
                )
            },
        beregningsMetode =
            this.string("beregningsmetode").let {
                objectMapper.readValue(
                    it,
                )
            },
        kilde = objectMapper.readValue(this.string("kilde")),
    )
}
