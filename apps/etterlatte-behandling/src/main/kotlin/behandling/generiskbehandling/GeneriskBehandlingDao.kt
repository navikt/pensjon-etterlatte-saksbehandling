package no.nav.etterlatte.behandling.generiskbehandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingStatus
import no.nav.etterlatte.libs.database.single
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID

class GeneriskBehandlingDao(private val connection: () -> Connection) {
    fun erBehandlingRedigerbar(behandlingId: UUID): Boolean {
        return with(connection()) {
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

            statement.executeQuery()
                .single { toGeneriskBehandling() }
                .erRedigerbar()
        }
    }

    private fun ResultSet.toGeneriskBehandling(): GeneriskBehandling {
        val type = BehandlingType.valueOf(getString(1))
        val id = UUID.fromString(getString(2))
        val status = getString(3)

        return GeneriskBehandling(id, type, status)
    }
}

private data class GeneriskBehandling(val id: UUID, val type: BehandlingType, val status: String) {
    fun erRedigerbar(): Boolean {
        when (type) {
            BehandlingType.BEHANDLING -> return BehandlingStatus.valueOf(status).kanEndres()
            BehandlingType.TILBAKEKREVING -> return TilbakekrevingStatus.valueOf(status).kanEndres()
            BehandlingType.KLAGE -> return KlageStatus.kanEndres(KlageStatus.valueOf(status))
        }
    }
}

private enum class BehandlingType {
    BEHANDLING,
    TILBAKEKREVING,
    KLAGE,
}
