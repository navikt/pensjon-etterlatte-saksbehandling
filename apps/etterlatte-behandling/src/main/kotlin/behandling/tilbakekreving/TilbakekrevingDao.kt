package no.nav.etterlatte.behandling.tilbakekreving

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseType
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingSkyld
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekrevingsbelop
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

class TilbakekrevingDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun hentTilbakekrevinger(sakId: SakId): List<TilbakekrevingBehandling> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val tilbakekrevinger = selectTilbakekrevinger(this, sakId)
                tilbakekrevinger.map { tilbakekreving ->
                    tilbakekreving.copy(
                        tilbakekreving =
                            tilbakekreving.tilbakekreving.copy(
                                perioder = selectTilbakekrevingsperioder(this, tilbakekreving.id),
                            ),
                    )
                }
            }
        }

    private fun selectTilbakekrevinger(
        connection: Connection,
        sakId: SakId,
    ): List<TilbakekrevingBehandling> =
        with(connection) {
            val statement =
                prepareStatement(
                    """
                    SELECT t.id, t.sak_id, s.saktype, s.fnr, s.enhet, t.opprettet, t.status, t.kravgrunnlag, t.vurdering, t.sende_brev 
                    FROM tilbakekreving t INNER JOIN sak s on t.sak_id = s.id
                    WHERE t.sak_id = ?
                    """.trimIndent(),
                )
            statement.setObject(1, sakId)
            statement.executeQuery().toList { toTilbakekreving() }
        }

    fun hentTilbakekreving(tilbakekrevingId: UUID): TilbakekrevingBehandling =
        connectionAutoclosing.hentConnection {
            with(it) {
                val tilbakekreving = selectTilbakekreving(this, tilbakekrevingId)
                tilbakekreving?.copy(
                    tilbakekreving =
                        tilbakekreving.tilbakekreving.copy(
                            perioder = selectTilbakekrevingsperioder(this, tilbakekrevingId),
                        ),
                ) ?: throw TilbakekrevingFinnesIkkeException("Tilbakekreving med id=$tilbakekrevingId finnes ikke")
            }
        }

    fun hentNyesteTilbakekreving(sakId: SakId): TilbakekrevingBehandling =
        connectionAutoclosing.hentConnection {
            with(it) {
                val tilbakekreving = hentNyesteTilbakekrevingForSak(this, sakId)
                tilbakekreving?.copy(
                    tilbakekreving =
                        tilbakekreving.tilbakekreving.copy(
                            perioder = selectTilbakekrevingsperioder(this, tilbakekreving.id),
                        ),
                ) ?: throw TilbakekrevingFinnesIkkeException("Tilbakekreving for sakId=$sakId finnes ikke")
            }
        }

    private fun hentNyesteTilbakekrevingForSak(
        connection: Connection,
        sakId: SakId,
    ): TilbakekrevingBehandling? =
        with(connection) {
            val statement =
                prepareStatement(
                    """
                    SELECT t.id, t.sak_id, s.saktype, s.fnr, s.enhet, t.opprettet, t.status, t.kravgrunnlag, t.vurdering, t.sende_brev 
                    FROM tilbakekreving t INNER JOIN sak s on t.sak_id = s.id
                    WHERE t.sak_id = ? 
                    ORDER BY t.opprettet DESC LIMIT 1
                    """.trimIndent(),
                )
            statement.setObject(1, sakId)
            statement.executeQuery().singleOrNull { toTilbakekreving() }
        }

    private fun selectTilbakekreving(
        connection: Connection,
        tilbakekrevingId: UUID,
    ): TilbakekrevingBehandling? =
        with(connection) {
            val statement =
                prepareStatement(
                    """
                    SELECT t.id, t.sak_id, s.saktype, s.fnr, s.enhet, t.opprettet, t.status, t.kravgrunnlag, t.vurdering, t.sende_brev 
                    FROM tilbakekreving t INNER JOIN sak s on t.sak_id = s.id
                    WHERE t.id = ?
                    """.trimIndent(),
                )
            statement.setObject(1, tilbakekrevingId)
            statement.executeQuery().singleOrNull { toTilbakekreving() }
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
                    ORDER BY maaned
                    """.trimIndent(),
                )
            statement.setObject(1, tilbakekrevingId)
            val allePerioderOgTyper = statement.executeQuery().toList { toTilbakekrevingsperiode() }
            val ytelseperioder = allePerioderOgTyper.filter { it.second.klasseType == KlasseType.YTEL.name }
            val feilkonto = allePerioderOgTyper.filter { it.second.klasseType == KlasseType.FEIL.name }
            return ytelseperioder.map { (maaned, ytelse) ->
                TilbakekrevingPeriode(
                    maaned = maaned,
                    ytelse = ytelse,
                    feilkonto =
                        feilkonto.find { it.first == maaned }?.second
                            ?: throw TilbakekrevingHarMangelException("Mangler feilkonto for tilbakekrevingsperiode"),
                )
            }
        }

    fun lagreTilbakekreving(tilbakekrevingBehandling: TilbakekrevingBehandling): TilbakekrevingBehandling =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                insertTilbakekreving(this, tilbakekrevingBehandling)
                insertTilbakekrevingsperioder(this, tilbakekrevingBehandling)
            }
            hentTilbakekreving(tilbakekrevingBehandling.id)
        }

    fun lagreTilbakekrevingMedNyePerioder(tilbakekrevingBehandling: TilbakekrevingBehandling): TilbakekrevingBehandling =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                deleteTilbakekrevingsperioder(this, tilbakekrevingBehandling.id)
                insertTilbakekreving(this, tilbakekrevingBehandling)
                insertTilbakekrevingsperioder(this, tilbakekrevingBehandling)
            }
            hentTilbakekreving(tilbakekrevingBehandling.id)
        }

    private fun insertTilbakekreving(
        connection: Connection,
        tilbakekrevingBehandling: TilbakekrevingBehandling,
    ) = with(connection) {
        val statement =
            prepareStatement(
                """
                INSERT INTO tilbakekreving(
                    id, status, sak_id, opprettet, kravgrunnlag, vurdering, sende_brev
                ) 
                VALUES (?, ?, ?, ?, ?, ?, ?) 
                ON CONFLICT (id) DO UPDATE SET
                    status = excluded.status,
                    kravgrunnlag = excluded.kravgrunnlag,
                    vurdering = excluded.vurdering,
                    sende_brev = excluded.sende_brev
                """.trimIndent(),
            )
        statement.setObject(1, tilbakekrevingBehandling.id)
        statement.setString(2, tilbakekrevingBehandling.status.name)
        statement.setLong(3, tilbakekrevingBehandling.sak.id)
        statement.setTidspunkt(4, tilbakekrevingBehandling.opprettet)
        with(tilbakekrevingBehandling.tilbakekreving) {
            statement.setJsonb(5, kravgrunnlag.toJsonNode())
            statement.setJsonb(6, vurdering)
        }
        statement.setBoolean(7, tilbakekrevingBehandling.sendeBrev)
        statement.executeUpdate().also { require(it == 1) }
    }

    private fun deleteTilbakekrevingsperioder(
        connection: Connection,
        tilbakekrevingId: UUID,
    ) = with(connection) {
        val statement =
            prepareStatement(
                """
                DELETE FROM tilbakekrevingsperiode 
                WHERE tilbakekreving_id = ?
                """.trimIndent(),
            )
        statement.setObject(1, tilbakekrevingId)
        statement.executeUpdate().also { require(it > 0) }
    }

    private fun insertTilbakekrevingsperioder(
        connection: Connection,
        tilbakekrevingBehandling: TilbakekrevingBehandling,
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
            statement.setObject(16, tilbakekrevingBehandling.id)
            statement.addBatch()
        }
        tilbakekrevingBehandling.tilbakekreving.perioder.forEach {
            addArgumentsAndBatch(it.maaned, it.ytelse)
            addArgumentsAndBatch(it.maaned, it.feilkonto)
        }
        statement.executeBatch()
    }

    private fun ResultSet.toTilbakekreving() =
        TilbakekrevingBehandling(
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
            tilbakekreving =
                Tilbakekreving(
                    vurdering = getString("vurdering")?.let { objectMapper.readValue(it) },
                    perioder = emptyList(),
                    kravgrunnlag = getString("kravgrunnlag").let { objectMapper.readValue(it) },
                ),
            sendeBrev = getBoolean("sende_brev"),
        )

    private fun ResultSet.toTilbakekrevingsperiode(): Pair<YearMonth, Tilbakekrevingsbelop> =
        Pair(
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

fun PreparedStatement.setInt(
    index: Int,
    value: Int?,
) = if (value == null) setNull(index, Types.BIGINT) else setInt(index, value)

fun ResultSet.getIntOrNull(name: String) = getInt(name).takeUnless { wasNull() }
