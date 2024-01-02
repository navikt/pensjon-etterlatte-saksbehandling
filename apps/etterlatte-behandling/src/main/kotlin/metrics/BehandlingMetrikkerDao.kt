package no.nav.etterlatte.metrics

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import javax.sql.DataSource

class BehandlingMetrikkerDao(private val dataSource: DataSource) {
    fun hent(): List<BehandlingAntall> {
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    select count(*), behandlingstype, revurdering_aarsak, kilde, status,
                       CASE virkningstidspunkt::JSONB -> 'kilde' ->> 'ident'
                           WHEN 'PESYS' THEN 'true'
                           ELSE 'false'
                       END automatisk
                    from behandling
                    group by behandlingstype, revurdering_aarsak, kilde, status, automatisk;
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
            behandlingstype = BehandlingType.valueOf(getString("behandlingstype")),
            revurderingsaarsak = getString("revurdering_aarsak")?.let { Revurderingaarsak.valueOf(it) },
            status = BehandlingStatus.valueOf(getString("status")),
            kilde = Vedtaksloesning.valueOf(getString("kilde")),
            automatiskMigrering = getString("automatisk"),
        )
    }
}

data class BehandlingAntall(
    val antall: Int,
    val behandlingstype: BehandlingType,
    val revurderingsaarsak: Revurderingaarsak?,
    val status: BehandlingStatus,
    val kilde: Vedtaksloesning,
    val automatiskMigrering: String,
)
