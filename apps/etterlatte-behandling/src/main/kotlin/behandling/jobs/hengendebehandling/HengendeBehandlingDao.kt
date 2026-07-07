package no.nav.etterlatte.behandling.jobs.hengendebehandling

import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import no.nav.etterlatte.libs.database.toList
import java.util.UUID

data class HengendeBehandling(
    val behandlingId: UUID,
    val sakId: SakId,
    val sistEndret: Tidspunkt,
)

class HengendeBehandlingDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun hentBehandlingerHengendeIAttestert(sistEndretFoer: Tidspunkt): List<HengendeBehandling> =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, sak_id, sist_endret FROM behandling
                        WHERE status = 'ATTESTERT' AND sist_endret < ?
                        """.trimIndent(),
                    )
                stmt.setTidspunkt(1, sistEndretFoer)

                stmt.executeQuery().toList {
                    HengendeBehandling(
                        behandlingId = getUUID("id"),
                        sakId = SakId(getLong("sak_id")),
                        sistEndret = getTidspunkt("sist_endret"),
                    )
                }
            }
        }
}
