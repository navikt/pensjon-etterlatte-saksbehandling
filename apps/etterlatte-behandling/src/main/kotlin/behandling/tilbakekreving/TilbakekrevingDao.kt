package no.nav.etterlatte.behandling.tilbakekreving

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.singleOrNull
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID

class TilbakekrevingDao(private val connection: () -> Connection) {
    fun hentTilbakekreving(tilbakekrevingId: UUID): Tilbakekreving? {
        with(connection()) {
            val statement =
                prepareStatement(
                    """
                    SELECT t.id, t.sak_id, saktype, fnr, enhet, opprettet, status, kravgrunnlag 
                    FROM tilbakekreving t INNER JOIN sak s on t.sak_id = s.id
                    WHERE t.id = ?
                    """.trimIndent(),
                )
            statement.setObject(1, tilbakekrevingId)
            return statement.executeQuery().singleOrNull {
                toTilbakekreving()
            }
        }
    }

    fun hentTilbakekrevingNonNull(tilbakekrevingId: UUID): Tilbakekreving =
        hentTilbakekreving(tilbakekrevingId) ?: throw Exception("Tilbakekreving med id=$tilbakekrevingId finnes ikke")

    fun lagreTilbakekreving(tilbakekreving: Tilbakekreving): Tilbakekreving {
        with(connection()) {
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
            statement.executeUpdate()
        }
        return hentTilbakekrevingNonNull(tilbakekreving.id).also { require(it == tilbakekreving) }
    }

    private fun ResultSet.toTilbakekreving(): Tilbakekreving {
        val saktype: SakType = enumValueOf(getString("saktype"))
        val kravgrunnlag = getString("kravgrunnlag").let { objectMapper.readValue<Kravgrunnlag>(it) }
        // TODO EY-2723 Midlertidig frem til utbetalinger persisteres
        val utbetalinger = kravgrunnlag.perioder.tilUtbetalinger(saktype)
        return Tilbakekreving(
            id = getString("id").let { UUID.fromString(it) },
            sak =
                Sak(
                    id = getLong("sak_id"),
                    sakType = saktype,
                    ident = getString("fnr"),
                    enhet = getString("enhet"),
                ),
            opprettet = getTidspunkt("opprettet"),
            status = enumValueOf(getString("status")),
            utbetalinger = utbetalinger,
            kravgrunnlag = kravgrunnlag,
        )
    }
}
