package no.nav.etterlatte.beregning.grunnlag

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.transaction
import org.postgresql.util.PGobject
import java.util.UUID
import javax.sql.DataSource

class BeregningsGrunnlagRepository(
    private val dataSource: DataSource,
) {
    fun finnBeregningsGrunnlag(id: UUID): BeregningsGrunnlag? =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    statement = finnBarnepensjonsGrunnlagForBehandling,
                    paramMap = mapOf("behandlings_id" to id),
                ).map { it.asBeregningsGrunnlag() }.asSingle,
            )
        }

    fun lagreBeregningsGrunnlag(beregningsGrunnlag: BeregningsGrunnlag): Boolean {
        val query =
            if (finnBeregningsGrunnlag(beregningsGrunnlag.behandlingId) == null) {
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
                            mapOf<String, Any?>(
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
                                "beregnings_metode_flere_avdoede" to
                                    beregningsGrunnlag.begegningsmetodeFlereAvdoede.takeIf { it.isNotEmpty() }?.somJsonb(),
                            ),
                    ).asUpdate,
                )
            }

        return count > 0
    }

    fun finnOverstyrBeregningGrunnlagForBehandling(id: UUID): List<OverstyrBeregningGrunnlagDao> =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    statement = finnOverstyrBeregningGrunnlagForBehandling,
                    paramMap = mapOf("behandlings_id" to id),
                ).map { it.asOverstyrBeregningGrunnlag() }.asList,
            )
        }

    fun lagreOverstyrBeregningGrunnlagForBehandling(
        behandlingId: UUID,
        data: List<OverstyrBeregningGrunnlagDao>,
    ) {
        dataSource.transaction { tx ->
            tx.run(
                queryOf(
                    statement = slettOverstyrBeregningGrunnlagForBehandling,
                    paramMap = mapOf("behandlings_id" to behandlingId),
                ).asUpdate,
            )

            data.forEach { grunnlag ->
                tx.run(
                    queryOf(
                        statement = lagreOverstyrBeregningGrunnlagForBehandling,
                        paramMap =
                            mapOf(
                                "id" to grunnlag.id,
                                "behandlings_id" to behandlingId,
                                "dato_fom" to grunnlag.datoFOM,
                                "dato_tom" to grunnlag.datoTOM,
                                "utbetalt_beloep" to grunnlag.utbetaltBeloep,
                                "trygdetid" to grunnlag.trygdetid,
                                "trygdetid_for_ident" to grunnlag.trygdetidForIdent,
                                "prorata_broek_teller" to grunnlag.prorataBroekTeller,
                                "prorata_broek_nevner" to grunnlag.prorataBroekNevner,
                                "sak_id" to grunnlag.sakId,
                                "beskrivelse" to grunnlag.beskrivelse,
                                "aarsak" to grunnlag.aarsak,
                                "kilde" to grunnlag.kilde.toJson(),
                                "regulering_regelresultat" to grunnlag.reguleringRegelresultat?.toJson(),
                            ),
                    ).asUpdate,
                )
            }
        }
    }

    companion object {
        val lagreGrunnlagQuery =
            """
            INSERT INTO beregningsgrunnlag (
                behandlings_id,
                soesken_med_i_beregning_perioder,
                institusjonsopphold,
                beregningsmetode,
                kilde,
                beregnings_metode_flere_avdoede
            )
            VALUES (
                :behandlings_id,
                :soesken_med_i_beregning,
                :institusjonsopphold,
                :beregningsmetode,
                :kilde,
                :beregnings_metode_flere_avdoede
            )
            """.trimMargin()

        val oppdaterGrunnlagQuery =
            """
            UPDATE beregningsgrunnlag
            SET
                soesken_med_i_beregning_perioder = :soesken_med_i_beregning,
                institusjonsopphold = :institusjonsopphold,
                beregningsmetode = :beregningsmetode,
                kilde = :kilde,
                beregnings_metode_flere_avdoede = :beregnings_metode_flere_avdoede
            WHERE behandlings_id = :behandlings_id
            """.trimMargin()

        val finnBarnepensjonsGrunnlagForBehandling =
            """
            SELECT
                behandlings_id,
                soesken_med_i_beregning_perioder,
                institusjonsopphold,
                beregningsmetode,
                kilde,
                beregnings_metode_flere_avdoede
            FROM beregningsgrunnlag
            WHERE behandlings_id = :behandlings_id
            """.trimIndent()

        val finnOverstyrBeregningGrunnlagForBehandling =
            """
            SELECT
                id,
                behandlings_id,
                dato_fra_og_med,
                dato_til_og_med,
                utbetalt_beloep,
                trygdetid,
                trygdetid_for_ident,
                prorata_broek_teller,
                prorata_broek_nevner,
                sak_id,
                beskrivelse,
                aarsak,
                kilde,
                regulering_regelresultat 
            FROM overstyr_beregningsgrunnlag
            WHERE behandlings_id = :behandlings_id
            """.trimIndent()

        val slettOverstyrBeregningGrunnlagForBehandling =
            """
            DELETE FROM overstyr_beregningsgrunnlag
            WHERE behandlings_id = :behandlings_id
            """.trimIndent()

        val lagreOverstyrBeregningGrunnlagForBehandling =
            """
            INSERT INTO overstyr_beregningsgrunnlag (
                id,
                behandlings_id,
                dato_fra_og_med,
                dato_til_og_med,
                utbetalt_beloep,
                trygdetid,
                trygdetid_for_ident,
                prorata_broek_teller,
                prorata_broek_nevner,
                sak_id,
                beskrivelse,
                aarsak,
                kilde,
                regulering_regelresultat 
            )                
            VALUES (
                :id,
                :behandlings_id,
                :dato_fom,
                :dato_tom,
                :utbetalt_beloep,
                :trygdetid,
                :trygdetid_for_ident,
                :prorata_broek_teller,
                :prorata_broek_nevner,
                :sak_id,
                :beskrivelse,
                :aarsak,
                :kilde,
                :regulering_regelresultat 
            )
            """.trimMargin()
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

private fun Row.asBeregningsGrunnlag(): BeregningsGrunnlag =
    BeregningsGrunnlag(
        behandlingId = this.uuid("behandlings_id"),
        soeskenMedIBeregning =
            this.stringOrNull("soesken_med_i_beregning_perioder")?.let {
                objectMapper.readValue(it)
            } ?: emptyList(),
        institusjonsoppholdBeregningsgrunnlag =
            this.stringOrNull("institusjonsopphold")?.let {
                objectMapper.readValue(
                    it,
                )
            } ?: emptyList(),
        beregningsMetode =
            this.string("beregningsmetode").let {
                objectMapper.readValue(
                    it,
                )
            },
        kilde = objectMapper.readValue(this.string("kilde")),
        begegningsmetodeFlereAvdoede =
            this.stringOrNull("beregnings_metode_flere_avdoede")?.let {
                objectMapper.readValue(it)
            } ?: emptyList(),
    )

private fun Row.asOverstyrBeregningGrunnlag(): OverstyrBeregningGrunnlagDao =
    OverstyrBeregningGrunnlagDao(
        id = this.uuid("id"),
        behandlingId = this.uuid("behandlings_id"),
        datoFOM = this.sqlDate("dato_fra_og_med").toLocalDate(),
        datoTOM = this.sqlDateOrNull("dato_til_og_med")?.toLocalDate(),
        utbetaltBeloep = this.longOrNull("utbetalt_beloep") ?: 0L,
        trygdetid = this.longOrNull("trygdetid") ?: 0,
        trygdetidForIdent = stringOrNull("trygdetid_for_ident"),
        prorataBroekTeller = this.longOrNull("prorata_broek_teller"),
        prorataBroekNevner = this.longOrNull("prorata_broek_nevner"),
        sakId = this.long("sak_id"),
        beskrivelse = this.string("beskrivelse"),
        aarsak = this.stringOrNull("aarsak"),
        kilde = objectMapper.readValue(this.string("kilde")),
        reguleringRegelresultat = this.stringOrNull("regulering_regelresultat")?.let { objectMapper.readValue(it) },
    )
