package no.nav.etterlatte.behandling.vedtaksbehandling

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingStatus
import no.nav.etterlatte.libs.database.single
import java.sql.ResultSet
import java.util.UUID

class VedtaksbehandlingDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun erBehandlingRedigerbar(behandlingId: UUID): Boolean =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT 'BEHANDLING', id, status FROM behandling WHERE id = ?
                        union SELECT 'TILBAKEKREVING', id, status FROM tilbakekreving WHERE id = ?
                        union SELECT 'KLAGE', id, status FROM klage WHERE id = ?
                        """.trimIndent(),
                    )
                statement.setObject(1, behandlingId)
                statement.setObject(2, behandlingId)
                statement.setObject(3, behandlingId)

                statement
                    .executeQuery()
                    .single { toVedtaksbehandling() }
                    .erRedigerbar()
            }
        }

    private fun ResultSet.toVedtaksbehandling(): Vedtaksbehandling {
        val type = BehandlingType.valueOf(getString(1))
        val id = UUID.fromString(getString(2))
        val status = getString(3)

        return Vedtaksbehandling(id, type, status)
    }
}

private data class Vedtaksbehandling(
    val id: UUID,
    val type: BehandlingType,
    val status: String,
) {
    fun erRedigerbar(): Boolean =
        when (type) {
            BehandlingType.BEHANDLING -> BehandlingStatus.valueOf(status).kanEndres()
            BehandlingType.TILBAKEKREVING -> TilbakekrevingStatus.valueOf(status).kanEndres()
            BehandlingType.KLAGE -> KlageStatus.kanEndres(KlageStatus.valueOf(status))
        }
}

private enum class BehandlingType {
    BEHANDLING,
    TILBAKEKREVING,
    KLAGE,
}
