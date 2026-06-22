package no.nav.etterlatte.metrics

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import javax.sql.DataSource

class BehandlingMetrikkerDao(
    private val dataSource: DataSource,
) {
    fun hentAttestertEldreEnnTimer(terskelTimer: Long): List<AttestertHengendeAntall> =
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    SELECT count(*) antall, s.saktype
                    FROM behandling b
                    JOIN sak s ON b.sak_id = s.id
                    WHERE b.status = 'ATTESTERT'
                      AND b.sist_endret < NOW() - INTERVAL '$terskelTimer hours'
                    GROUP BY s.saktype
                    """.trimIndent(),
                )
            statement.executeQuery().toList {
                AttestertHengendeAntall(
                    antall = getInt("antall"),
                    saktype = SakType.valueOf(getString("saktype")),
                )
            }
        }

    fun hent(): List<BehandlingAntall> {
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    select count(*) antall, saktype, behandlingstype, revurdering_aarsak, vedtaksloesning, status,
                       CASE virkningstidspunkt::JSONB -> 'kilde' ->> 'ident'
                           WHEN 'PESYS' THEN 'true'
                           ELSE 'false'
                       END automatisk
                    from behandling b join sak s on b.sak_id = s.id
                    group by saktype, behandlingstype, revurdering_aarsak, vedtaksloesning, status, automatisk;
                    """.trimIndent(),
                )
            return statement.executeQuery().toList {
                asBehandlingAntall()
            }
        }
    }

    private fun ResultSet.asBehandlingAntall(): BehandlingAntall =
        BehandlingAntall(
            antall = getInt("antall"),
            saktype = SakType.valueOf(getString("saktype")),
            behandlingstype = BehandlingType.valueOf(getString("behandlingstype")),
            revurderingsaarsak = getString("revurdering_aarsak")?.let { Revurderingaarsak.valueOf(it) },
            status = BehandlingStatus.valueOf(getString("status")),
            // Feltet er renamet i intern modell, men metrikken beholder kilde-navnet for å bevare eksternt
            // grensesnitt til grafana
            kilde = Vedtaksloesning.valueOf(getString("vedtaksloesning")),
            automatiskMigrering = getString("automatisk"),
        )
}

data class BehandlingAntall(
    val antall: Int,
    val saktype: SakType,
    val behandlingstype: BehandlingType,
    val revurderingsaarsak: Revurderingaarsak?,
    val status: BehandlingStatus,
    val kilde: Vedtaksloesning,
    val automatiskMigrering: String,
)

data class AttestertHengendeAntall(
    val antall: Int,
    val saktype: SakType,
)
