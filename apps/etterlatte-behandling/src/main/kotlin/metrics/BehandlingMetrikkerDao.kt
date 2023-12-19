package no.nav.etterlatte.metrics

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import javax.sql.DataSource

class BehandlingMetrikkerDao(private val dataSource: DataSource) {
    fun hent(): List<BehandlingAntall> {
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    select count(*), kilde, status,
                       CASE virkningstidspunkt::JSONB -> 'kilde' ->> 'ident'
                           WHEN 'PESYS' THEN 'true'
                           ELSE 'false'
                       END automatisk
                    from behandling
                    group by kilde, status, automatisk;
                    """.trimIndent(),
                )
            return statement.executeQuery().toList {
                asBehandlingAntall()
            }
        }
    }

    private fun ResultSet.asBehandlingAntall(): BehandlingAntall {
        return BehandlingAntall(
            antall = getInt("count"),
            status = BehandlingStatus.valueOf(getString("status")),
            kilde = Vedtaksloesning.valueOf(getString("kilde")),
            automatiskMigrering = getString("automatisk"),
        )
    }
}

data class BehandlingAntall(
    val antall: Int,
    val status: BehandlingStatus,
    val kilde: Vedtaksloesning,
    val automatiskMigrering: String,
)
