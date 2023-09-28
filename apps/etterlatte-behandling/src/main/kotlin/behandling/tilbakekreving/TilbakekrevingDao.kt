package no.nav.etterlatte.behandling.tilbakekreving

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseType
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.YearMonth
import java.util.UUID

class TilbakekrevingDao(private val connection: () -> Connection) {
    fun hentTilbakekreving(tilbakekrevingId: UUID): Tilbakekreving {
        with(connection()) {
            val perioder = selectTilbakekrevingsperioder(this, tilbakekrevingId)
            return selectTilbakekreving(this, tilbakekrevingId, perioder)
                ?: throw TilbakekrevingFinnesIkkeException("Tilbakekreving med id=$tilbakekrevingId finnes ikke")
        }
    }

    private fun selectTilbakekreving(
        connection: Connection,
        tilbakekrevingId: UUID,
        perioder: List<TilbakekrevingPeriode>,
    ): Tilbakekreving? =
        with(connection) {
            val statement =
                prepareStatement(
                    """
                    SELECT t.id, t.sak_id, saktype, fnr, enhet, opprettet, status, kravgrunnlag 
                    FROM tilbakekreving t INNER JOIN sak s on t.sak_id = s.id
                    WHERE t.id = ?
                    """.trimIndent(),
                )
            statement.setObject(1, tilbakekrevingId)
            return statement.executeQuery().singleOrNull { toTilbakekreving(perioder) }
        }

    private fun selectTilbakekrevingsperioder(
        connection: Connection,
        tilbakekrevingId: UUID,
    ): List<TilbakekrevingPeriode> =
        with(connection) {
            val statement =
                prepareStatement(
                    """
                    SELECT * FROM tilbakekrevingsperiode WHERE tilbakekreving_id = ?
                    """.trimIndent(),
                )
            statement.setObject(1, tilbakekrevingId)
            val alle = statement.executeQuery().toList { toTilbakekrevingsperiode() }
            val ytelsebeloeper = alle.filter { it.second.klasseType == KlasseType.YTEL.name }
            val feilkonto = alle.filter { it.second.klasseType == KlasseType.FEIL.name }
            return ytelsebeloeper.map { (maaned, ytelsebeloeper) ->
                TilbakekrevingPeriode(
                    maaned = maaned,
                    ytelsebeloeper = ytelsebeloeper,
                    feilkonto =
                        feilkonto.find { it.first == maaned }?.second
                            ?: throw TilbakekrevingHarMangelException("Mangler feilkonto for tilbakekrevingsperiode"),
                )
            }
        }

    fun lagreTilbakekreving(tilbakekreving: Tilbakekreving): Tilbakekreving {
        with(connection()) {
            insertTilbakekreving(this, tilbakekreving)
            insertTilbakekrevingsperioder(this, tilbakekreving)
        }
        return hentTilbakekreving(tilbakekreving.id)
    }

    private fun insertTilbakekreving(
        connection: Connection,
        tilbakekreving: Tilbakekreving,
    ) = with(connection) {
        val statement =
            prepareStatement(
                """
                INSERT INTO tilbakekreving(id, status, sak_id, opprettet, kravgrunnlag)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET status = excluded.status, kravgrunnlag = excluded.kravgrunnlag
                """.trimIndent(),
            )
        statement.setObject(1, tilbakekreving.id)
        statement.setString(2, tilbakekreving.status.name)
        statement.setLong(3, tilbakekreving.sak.id)
        statement.setTidspunkt(4, tilbakekreving.opprettet)
        statement.setJsonb(5, tilbakekreving.kravgrunnlag.toJsonNode())
        statement.executeUpdate().also { require(it == 1) }
    }

    private fun insertTilbakekrevingsperioder(
        connection: Connection,
        tilbakekreving: Tilbakekreving,
    ) = with(connection) {
        val statement =
            prepareStatement(
                """
                INSERT INTO tilbakekrevingsperiode(
                    id,
                    maaned,
                    klasse_kode,
                    klasse_type,
                    brutto_utbetaling,
                    ny_brutto_utbetaling,
                    skatteprosent,
                    beregnet_feilutbetaling,
                    brutto_tilbakekreving,
                    netto_tilbakekreving,
                    skatt,
                    skyld,
                    resultat,
                    tilbakekrevingsprosent,
                    rentetillegg,
                    tilbakekreving_id
                )
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT (id) DO UPDATE SET
                 beregnet_feilutbetaling = excluded.beregnet_feilutbetaling,
                 brutto_tilbakekreving = excluded.brutto_tilbakekreving,
                 netto_tilbakekreving = excluded.netto_tilbakekreving,
                 skatt = excluded.skatt,
                 skyld = excluded.skyld,
                 resultat = excluded.resultat,
                 tilbakekrevingsprosent = excluded.tilbakekrevingsprosent,
                 rentetillegg = excluded.rentetillegg
                """.trimIndent(),
            )

        fun addArgumentsAndBatch(
            maaned: YearMonth,
            beloeper: Tilbakekrevingsbelop,
        ) {
            statement.setObject(1, beloeper.id)
            statement.setString(2, maaned.toString())
            statement.setString(3, beloeper.klasseKode)
            statement.setString(4, beloeper.klasseType)
            statement.setInt(5, beloeper.bruttoUtbetaling)
            statement.setInt(6, beloeper.nyBruttoUtbetaling)
            statement.setBigDecimal(7, beloeper.skatteprosent)
            statement.setInt(8, beloeper.beregnetFeilutbetaling)
            statement.setInt(9, beloeper.bruttoTilbakekreving)
            statement.setInt(10, beloeper.nettoTilbakekreving)
            statement.setInt(11, beloeper.skatt)
            statement.setString(12, beloeper.skyld?.name)
            statement.setString(13, beloeper.resultat?.name)
            statement.setInt(14, beloeper.tilbakekrevingsprosent)
            statement.setInt(15, beloeper.rentetillegg)
            statement.setObject(16, tilbakekreving.id)
            statement.addBatch()
        }
        tilbakekreving.perioder.forEach {
            addArgumentsAndBatch(it.maaned, it.ytelsebeloeper)
            addArgumentsAndBatch(it.maaned, it.feilkonto)
        }
        statement.executeBatch()
    }

    private fun ResultSet.toTilbakekreving(perioder: List<TilbakekrevingPeriode>) =
        Tilbakekreving(
            id = getString("id").let { UUID.fromString(it) },
            sak =
                Sak(
                    id = getLong("sak_id"),
                    sakType = enumValueOf(getString("saktype")),
                    ident = getString("fnr"),
                    enhet = getString("enhet"),
                ),
            opprettet = getTidspunkt("opprettet"),
            status = enumValueOf(getString("status")),
            perioder = perioder,
            kravgrunnlag = getString("kravgrunnlag").let { objectMapper.readValue(it) },
        )

    private fun ResultSet.toTilbakekrevingsperiode(): Pair<YearMonth, Tilbakekrevingsbelop> {
        return Pair(
            YearMonth.parse(getString("maaned")),
            Tilbakekrevingsbelop(
                id = getUUID("id"),
                klasseKode = getString("klasse_kode"),
                klasseType = getString("klasse_type"),
                bruttoUtbetaling = getInt("brutto_utbetaling"),
                nyBruttoUtbetaling = getInt("ny_brutto_utbetaling"),
                skatteprosent = getBigDecimal("skatteprosent"),
                beregnetFeilutbetaling = getIntOrNull("beregnet_feilutbetaling"),
                bruttoTilbakekreving = getIntOrNull("brutto_tilbakekreving"),
                nettoTilbakekreving = getIntOrNull("netto_tilbakekreving"),
                skatt = getIntOrNull("skatt"),
                skyld = getString("skyld")?.let { TilbakekrevingSkyld.valueOf(it) },
                resultat = getString("resultat")?.let { TilbakekrevingResultat.valueOf(it) },
                tilbakekrevingsprosent = getIntOrNull("tilbakekrevingsprosent"),
                rentetillegg = getIntOrNull("rentetillegg"),
            ),
        )
    }
}

fun PreparedStatement.setInt(
    index: Int,
    value: Int?,
) = if (value == null) setNull(index, Types.BIGINT) else setInt(index, value)

fun ResultSet.getIntOrNull(name: String) = getInt(name).takeUnless { wasNull() }
