package no.nav.etterlatte.behandling.tilbakekreving

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingAvbruttAarsak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingSkyld
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekrevingsbelop
import no.nav.etterlatte.libs.common.tilbakekreving.tilbakekrevingsbeloepComparator
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.setSakId
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
                    SELECT t.id, t.sak_id, s.saktype, s.fnr, s.enhet, s.adressebeskyttelse, s.erSkjermet, t.opprettet, t.status, t.kravgrunnlag, t.vurdering, t.sende_brev, t.aarsak_for_avbrytelse, t.omgjoering_av_id, t.overstyr_netto_brutto
                    FROM tilbakekreving t INNER JOIN sak s on t.sak_id = s.id
                    WHERE t.sak_id = ?
                    """.trimIndent(),
                )
            statement.setSakId(1, sakId)
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
                    SELECT t.id, t.sak_id, s.saktype, s.fnr, s.enhet, s.adressebeskyttelse, s.erSkjermet, t.opprettet, t.status, t.kravgrunnlag, t.vurdering, t.sende_brev, t.aarsak_for_avbrytelse, t.omgjoering_av_id, t.overstyr_netto_brutto
                    FROM tilbakekreving t INNER JOIN sak s on t.sak_id = s.id
                    WHERE t.sak_id = ? 
                    ORDER BY t.opprettet DESC LIMIT 1
                    """.trimIndent(),
                )
            statement.setSakId(1, sakId)
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
                    SELECT t.id, t.sak_id, s.saktype, s.fnr, s.enhet, s.adressebeskyttelse, s.erSkjermet, t.opprettet, t.status, t.kravgrunnlag, t.vurdering, t.sende_brev, t.aarsak_for_avbrytelse, t.omgjoering_av_id, t.overstyr_netto_brutto
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
            return allePerioderOgTyper
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, value) -> value }
                .map { (maaned, perioder) ->
                    TilbakekrevingPeriode(
                        maaned = maaned,
                        tilbakekrevingsbeloep =
                            perioder.sortedWith(tilbakekrevingsbeloepComparator),
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
                    id, status, sak_id, opprettet, kravgrunnlag, vurdering, sende_brev, aarsak_for_avbrytelse, omgjoering_av_id, overstyr_netto_brutto
                ) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
                ON CONFLICT (id) DO UPDATE SET
                    status = excluded.status,
                    kravgrunnlag = excluded.kravgrunnlag,
                    vurdering = excluded.vurdering,
                    sende_brev = excluded.sende_brev,
                    aarsak_for_avbrytelse = excluded.aarsak_for_avbrytelse,
                    overstyr_netto_brutto = excluded.overstyr_netto_brutto
                """.trimIndent(),
            )
        statement.setObject(1, tilbakekrevingBehandling.id)
        statement.setString(2, tilbakekrevingBehandling.status.name)
        statement.setSakId(3, tilbakekrevingBehandling.sak.id)
        statement.setTidspunkt(4, tilbakekrevingBehandling.opprettet)
        with(tilbakekrevingBehandling.tilbakekreving) {
            statement.setJsonb(5, kravgrunnlag.toJsonNode())
            statement.setJsonb(6, vurdering)
        }
        statement.setBoolean(7, tilbakekrevingBehandling.sendeBrev)
        statement.setString(8, tilbakekrevingBehandling.aarsakForAvbrytelse?.name)
        statement.setObject(9, tilbakekrevingBehandling.omgjoeringAvId)
        statement.setString(10, tilbakekrevingBehandling.tilbakekreving.overstyrBehandletNettoTilBruttoMotTilbakekreving?.name)
        statement.executeUpdate().also {
            krev(it == 1) {
                "Kunne ikke lagre tilbaekreving behandling for sakid ${tilbakekrevingBehandling.sak.id}"
            }
        }
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
        statement.executeUpdate().also {
            krev(it > 0) {
                "Kunne ikke deleteTilbakekrevingsperioder behandling for id $tilbakekrevingId"
            }
        }
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
            beloep: Tilbakekrevingsbelop,
        ) {
            statement.setObject(1, beloep.id)
            statement.setString(2, maaned.toString())
            statement.setString(3, beloep.klasseKode)
            statement.setString(4, beloep.klasseType)
            statement.setInt(5, beloep.bruttoUtbetaling)
            statement.setInt(6, beloep.nyBruttoUtbetaling)
            statement.setBigDecimal(7, beloep.skatteprosent)
            statement.setInt(8, beloep.beregnetFeilutbetaling)
            statement.setInt(9, beloep.bruttoTilbakekreving)
            statement.setInt(10, beloep.nettoTilbakekreving)
            statement.setInt(11, beloep.skatt)
            statement.setString(12, beloep.skyld?.name)
            statement.setString(13, beloep.resultat?.name)
            statement.setInt(14, beloep.tilbakekrevingsprosent)
            statement.setInt(15, beloep.rentetillegg)
            statement.setObject(16, tilbakekrevingBehandling.id)
            statement.addBatch()
        }
        tilbakekrevingBehandling.tilbakekreving.perioder.forEach { periode ->
            periode.tilbakekrevingsbeloep.forEach { belop ->
                addArgumentsAndBatch(periode.maaned, belop)
            }
        }
        statement.executeBatch()
    }

    private fun ResultSet.toTilbakekreving() =
        TilbakekrevingBehandling(
            id = getString("id").let { UUID.fromString(it) },
            sak =
                Sak(
                    id = SakId(getLong("sak_id")),
                    sakType = enumValueOf(getString("saktype")),
                    ident = getString("fnr"),
                    enhet = Enhetsnummer(getString("enhet")),
                    adressebeskyttelse = getString("adressebeskyttelse")?.let { enumValueOf<AdressebeskyttelseGradering>(it) },
                    erSkjermet = getBoolean("erSkjermet"),
                ),
            opprettet = getTidspunkt("opprettet"),
            status = enumValueOf(getString("status")),
            tilbakekreving =
                Tilbakekreving(
                    vurdering = getString("vurdering")?.let { objectMapper.readValue(it) },
                    perioder = emptyList(),
                    kravgrunnlag = getString("kravgrunnlag").let { objectMapper.readValue(it) },
                    overstyrBehandletNettoTilBruttoMotTilbakekreving =
                        getString(
                            "overstyr_netto_brutto",
                        )?.let { objectMapper.readValue(it) },
                ),
            sendeBrev = getBoolean("sende_brev"),
            aarsakForAvbrytelse = getString("aarsak_for_avbrytelse")?.let { enumValueOf<TilbakekrevingAvbruttAarsak>(it) },
            omgjoeringAvId = getString("omgjoering_av_id")?.let { UUID.fromString(it) },
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
