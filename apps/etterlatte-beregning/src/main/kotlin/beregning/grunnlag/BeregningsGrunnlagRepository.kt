package no.nav.etterlatte.beregning.grunnlag

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.objectMapper
import org.postgresql.util.PGobject
import java.util.*
import javax.sql.DataSource

class BeregningsGrunnlagRepository(private val dataSource: DataSource) {
    fun finnGrunnlagForBehandling(id: UUID): BeregningsGrunnlag? = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                statement = finnGrunnlagForBehandling,
                paramMap = mapOf("behandlings_id" to id)
            ).map { it.asBeregningsGrunnlag() }.asSingle
        )
    }

    fun lagre(beregningsGrunnlag: BeregningsGrunnlag): Boolean {
        val query = if (finnGrunnlagForBehandling(beregningsGrunnlag.behandlingId) != null) {
            oppdaterGrunnlagQuery
        } else {
            lagreGrunnlagQuery
        }

        val count = using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    statement = query,
                    paramMap = mapOf<String, Any>(
                        "behandlings_id" to beregningsGrunnlag.behandlingId,
                        "soesken_med_i_beregning" to beregningsGrunnlag.soeskenMedIBeregning.somJsonb(),
                        "institusjonsopphold" to objectMapper.writeValueAsString(
                            beregningsGrunnlag.institusjonsoppholdBeregnignsGrunnlag
                        ),
                        "kilde" to beregningsGrunnlag.kilde.toJson()

                    )
                ).asUpdate
            )
        }

        return count > 0
    }

    companion object {

        val lagreGrunnlagQuery = """
            INSERT INTO bp_beregningsgrunnlag(behandlings_id, soesken_med_i_beregning_perioder, institusjonsopphold, kilde)
            VALUES(
                :behandlings_id,
                :soesken_med_i_beregning,
                :institusjonsopphold,
                :kilde
            )
        """.trimMargin()

        val oppdaterGrunnlagQuery = """
            UPDATE bp_beregningsgrunnlag
            SET soesken_med_i_beregning_perioder = :soesken_med_i_beregning, institusjonsopphold = :institusjonsopphold, kilde = :kilde
            WHERE behandlings_id = :behandlings_id
        """.trimMargin()

        val finnGrunnlagForBehandling = """
            SELECT behandlings_id, soesken_med_i_beregning_perioder, institusjonsopphold, kilde
            FROM bp_beregningsgrunnlag
            WHERE behandlings_id = :behandlings_id
        """.trimIndent()
    }
}

inline fun <reified T> T.somJsonb(): PGobject {
    val that = this
    val jsonObject = PGobject().apply {
        type = "json"
        value = objectMapper.writeValueAsString(that)
    }
    return jsonObject
}

private fun Row.asBeregningsGrunnlag(): BeregningsGrunnlag {
    return BeregningsGrunnlag(
        behandlingId = this.uuid("behandlings_id"),
        soeskenMedIBeregning = objectMapper.readValue(this.string("soesken_med_i_beregning_perioder")),
        institusjonsoppholdBeregnignsGrunnlag = objectMapper.readValue(this.string("institusjonsopphold")),
        kilde = objectMapper.readValue(this.string("kilde"))
    )
}