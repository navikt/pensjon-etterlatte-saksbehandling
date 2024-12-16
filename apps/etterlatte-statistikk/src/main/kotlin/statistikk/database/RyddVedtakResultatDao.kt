package no.nav.etterlatte.statistikk.database

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.statistikk.domain.BehandlingResultat
import java.util.UUID
import javax.sql.DataSource

class RyddVedtakResultatDao(
    private val datasource: DataSource,
) {
    fun hentRaderMedPotensiellFeil(): List<RadMedKanskjeFeilResultat> {
        datasource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    select id, behandling_id, behandling_type, fikset from sak_rader_med_potensielt_feil_resultat 
                    where fikset = false order by behandling_id limit 100
                    """.trimIndent(),
                )

            return statement.executeQuery().toList {
                RadMedKanskjeFeilResultat(
                    id = getLong("id"),
                    behandlingId = getObject("behandling_id") as UUID,
                    behandlingType = enumValueOf(getString("behandling_type")),
                    fikset = getBoolean("fikset"),
                )
            }
        }
    }

    fun oppdaterResultat(
        rad: RadMedKanskjeFeilResultat,
        resultat: BehandlingResultat,
    ) {
        datasource.connection.use { connection ->
            connection.autoCommit = false
            val stmntOppdaterSak =
                connection.prepareStatement(
                    """
                    update sak set behandling_resultat = ? where id = ? and behandling_id = ?
                    """.trimIndent(),
                )

            stmntOppdaterSak.setString(1, resultat.name)
            stmntOppdaterSak.setLong(2, rad.id)
            stmntOppdaterSak.setObject(3, rad.behandlingId)
            stmntOppdaterSak.executeUpdate()

            val stmntOppdaterRydderad =
                connection.prepareStatement(
                    """
                    update sak_rader_med_potensielt_feil_resultat set fikset = true where id = ? and behandling_id = ?
                    """.trimIndent(),
                )

            stmntOppdaterRydderad.setLong(1, rad.id)
            stmntOppdaterRydderad.setObject(2, rad.behandlingId)
            stmntOppdaterRydderad.executeUpdate()

            connection.commit()
        }
    }
}

data class RadMedKanskjeFeilResultat(
    val id: Long,
    val behandlingId: UUID,
    val behandlingType: BehandlingType,
    val fikset: Boolean,
)
