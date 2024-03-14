package no.nav.etterlatte.behandling.vedtaksbehandling

import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.behandling.BehandlingMedStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.database.toList

class FiksVedtaktstilstandDao(private val connection: ConnectionAutoclosing) {
    fun finnAktuelleBehandlinger(): List<BehandlingMedStatus> {
        val sql =
            """
            SELECT DISTINCT b.id as id, b.status as status, h.ident as ident FROM behandling b
             inner join behandlinghendelse h on b.id = h.behandlingid
              and h.id = 
             (select max(id) from behandlinghendelse h2 where h2.behandlingid = b.id
             and h2.inntruffet > to_timestamp('2024-03-13 15:00:00', 'YYYY-MM-DD HH24:MI:SS'
             and h2.inntruffet < to_timestamp('2024-03-14 13:00:00', 'YYYY-MM-DD HH24:MI:SS')
             )
            """.trimIndent()
        return connection.hentConnection {
            with(it) {
                val statement = prepareStatement(sql)
                statement.executeQuery().toList {
                    BehandlingMedStatus(
                        id = getUUID("id"),
                        status = BehandlingStatus.valueOf(getString("status")),
                        ident = getString("ident"),
                    )
                }
            }
        }
    }
}
