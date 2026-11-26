package no.nav.etterlatte.behandling.forlengetoms

import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.libs.database.singleOrNull
import tools.jackson.module.kotlin.readValue
import java.sql.ResultSet
import java.util.UUID

class ForlengetOmsDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun hentVurdering(behandlingId: UUID): ForlengetOmsVurdering? =
        connectionAutoclosing.hentConnection { connection ->
            connection
                .prepareStatement(
                    """
                    SELECT behandling_id, aktivitet, begrunnelse, utfall, opprettet, endret
                    FROM forlenget_oms_vurdering
                    WHERE behandling_id = ?
                    """.trimIndent(),
                ).use { statement ->
                    statement.setObject(1, behandlingId)
                    statement.executeQuery().use { resultSet ->
                        resultSet.singleOrNull { toVurdering() }
                    }
                }
        }

    fun lagreVurdering(
        behandlingId: UUID,
        aktivitet: ForlengetOmsAktivitet,
        begrunnelse: String,
        utfall: ForlengetOmsUtfall,
        kilde: Grunnlagsopplysning.Saksbehandler,
    ): ForlengetOmsVurdering =
        connectionAutoclosing.hentConnection { connection ->
            connection
                .prepareStatement(
                    """
                    INSERT INTO forlenget_oms_vurdering
                        (behandling_id, aktivitet, begrunnelse, utfall, opprettet, endret)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (behandling_id) DO UPDATE SET
                        aktivitet = excluded.aktivitet,
                        begrunnelse = excluded.begrunnelse,
                        utfall = excluded.utfall,
                        endret = excluded.endret
                    RETURNING behandling_id, aktivitet, begrunnelse, utfall, opprettet, endret
                    """.trimIndent(),
                ).use { statement ->
                    statement.setObject(1, behandlingId)
                    statement.setJsonb(2, aktivitet)
                    statement.setString(3, begrunnelse)
                    statement.setString(4, utfall.name)
                    statement.setJsonb(5, kilde)
                    statement.setJsonb(6, kilde)
                    statement.executeQuery().use { resultSet ->
                        resultSet.single { toVurdering() }
                    }
                }
        }

    private fun ResultSet.toVurdering() =
        ForlengetOmsVurdering(
            behandlingId = getUUID("behandling_id"),
            aktivitet = objectMapper.readValue(getString("aktivitet")),
            begrunnelse = getString("begrunnelse"),
            utfall = ForlengetOmsUtfall.valueOf(getString("utfall")),
            opprettet = objectMapper.readValue(getString("opprettet")),
            endret = objectMapper.readValue(getString("endret")),
        )
}
