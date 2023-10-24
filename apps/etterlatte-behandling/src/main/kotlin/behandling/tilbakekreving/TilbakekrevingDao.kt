package no.nav.etterlatte.behandling.tilbakekreving

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseType
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingAarsak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingAktsomhet
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHjemmel
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingSkyld
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurdering
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurderingUaktsomhet
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

class TilbakekrevingDao(private val connection: () -> Connection) {
    fun hentTilbakekreving(tilbakekrevingId: UUID): TilbakekrevingBehandling {
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
    ): TilbakekrevingBehandling? =
        with(connection) {
            val statement =
                prepareStatement(
                    """
                    SELECT t.id, t.sak_id, saktype, fnr, enhet, opprettet, status, kravgrunnlag,
                            vurdering_beskrivelse, vurdering_konklusjon, vurdering_aarsak, vurdering_hjemmel,
                            vurdering_aktsomhet, akstomhet_redusering_av_kravet,
                            aktsomhet_strafferettslig_vurdering, aktsomhet_rentevurdering
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

    fun lagreTilbakekreving(tilbakekrevingBehandling: TilbakekrevingBehandling): TilbakekrevingBehandling {
        with(connection()) {
            insertTilbakekreving(this, tilbakekrevingBehandling)
            insertTilbakekrevingsperioder(this, tilbakekrevingBehandling)
        }
        return hentTilbakekreving(tilbakekrevingBehandling.id)
    }

    private fun insertTilbakekreving(
        connection: Connection,
        tilbakekrevingBehandling: TilbakekrevingBehandling,
    ) = with(connection) {
        val statement =
            prepareStatement(
                """
                INSERT INTO tilbakekreving(
                    id, status, sak_id, opprettet, kravgrunnlag,
                    vurdering_beskrivelse, vurdering_konklusjon, vurdering_aarsak, vurdering_hjemmel,
                    vurdering_aktsomhet, akstomhet_redusering_av_kravet,
                    aktsomhet_strafferettslig_vurdering, aktsomhet_rentevurdering
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    status = excluded.status,
                    kravgrunnlag = excluded.kravgrunnlag,
                    vurdering_beskrivelse = excluded.vurdering_beskrivelse,
                    vurdering_konklusjon = excluded.vurdering_konklusjon,
                    vurdering_aarsak = excluded.vurdering_aarsak,
                    vurdering_hjemmel = excluded.vurdering_hjemmel,
                    vurdering_aktsomhet = excluded.vurdering_aktsomhet,
                    akstomhet_redusering_av_kravet = excluded.akstomhet_redusering_av_kravet,
                    aktsomhet_strafferettslig_vurdering = excluded.aktsomhet_strafferettslig_vurdering, 
                    aktsomhet_rentevurdering = excluded.aktsomhet_rentevurdering 
                """.trimIndent(),
            )
        statement.setObject(1, tilbakekrevingBehandling.id)
        statement.setString(2, tilbakekrevingBehandling.status.name)
        statement.setLong(3, tilbakekrevingBehandling.sak.id)
        statement.setTidspunkt(4, tilbakekrevingBehandling.opprettet)
        with(tilbakekrevingBehandling.tilbakekreving) {
            statement.setJsonb(5, kravgrunnlag.toJsonNode())
            statement.setString(6, vurdering.beskrivelse)
            statement.setString(7, vurdering.konklusjon)
            statement.setString(8, vurdering.aarsak?.name)
            statement.setString(9, vurdering.hjemmel?.name)
            statement.setString(10, vurdering.aktsomhet.aktsomhet?.name)
            statement.setString(11, vurdering.aktsomhet.reduseringAvKravet)
            statement.setString(12, vurdering.aktsomhet.strafferettsligVurdering)
            statement.setString(13, vurdering.aktsomhet.rentevurdering)
        }
        statement.executeUpdate().also { require(it == 1) }
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

    private fun ResultSet.toTilbakekreving(perioder: List<TilbakekrevingPeriode>) =
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
                    vurdering =
                        TilbakekrevingVurdering(
                            beskrivelse = getString("vurdering_beskrivelse"),
                            konklusjon = getString("vurdering_konklusjon"),
                            aarsak = getString("vurdering_aarsak")?.let { TilbakekrevingAarsak.valueOf(it) },
                            aktsomhet =
                                TilbakekrevingVurderingUaktsomhet(
                                    aktsomhet = getString("vurdering_aktsomhet")?.let { TilbakekrevingAktsomhet.valueOf(it) },
                                    reduseringAvKravet = getString("akstomhet_redusering_av_kravet"),
                                    strafferettsligVurdering = getString("aktsomhet_strafferettslig_vurdering"),
                                    rentevurdering = getString("aktsomhet_rentevurdering"),
                                ),
                            hjemmel = getString("vurdering_hjemmel")?.let { TilbakekrevingHjemmel.valueOf(it) },
                        ),
                    perioder = perioder,
                    kravgrunnlag = getString("kravgrunnlag").let { objectMapper.readValue(it) },
                ),
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
