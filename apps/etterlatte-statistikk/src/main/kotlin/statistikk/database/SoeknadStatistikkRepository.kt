package no.nav.etterlatte.statistikk.database

import no.nav.etterlatte.libs.database.single
import no.nav.etterlatte.statistikk.domain.SoeknadStatistikk
import javax.sql.DataSource

class SoeknadStatistikkRepository(private val datasource: DataSource) {

    private val connection get() = datasource.connection

    fun lagreNedSoeknadStatistikk(soeknadStatistikk: SoeknadStatistikk): SoeknadStatistikk {
        connection.use {
            val statement = it.prepareStatement(
                """
                INSERT INTO soeknad_statistikk (soeknad_id, gyldig_for_behandling, saktype, kriterier_for_ingen_behandling)
                    VALUES (?, ?, ?, ?) 
                    ON CONFLICT(soeknad_id) DO UPDATE SET gyldig_for_behandling = EXCLUDED.gyldig_for_behandling, 
                    kriterier_for_ingen_behandling = EXCLUDED.kriterier_for_ingen_behandling
                """.trimIndent()
            )
            statement.setLong(1, soeknadStatistikk.soeknadId)
            statement.setBoolean(2, soeknadStatistikk.gyldigForBehandling)
            statement.setString(3, soeknadStatistikk.sakType.toString())
            statement.setJsonb(4, soeknadStatistikk.kriterierForIngenBehandling)
            statement.executeUpdate()
        }
        return soeknadStatistikk
    }

    fun hentAntallSoeknader(): Long {
        return connection.use {
            val statement = it.prepareStatement(
                """
                    SELECT COUNT(*) FROM soeknad_statistikk
                """.trimIndent()
            )
            val result = statement.executeQuery()
            result.single { getLong(1) }
        }
    }

    fun hentAntallSoeknaderGyldigForBehandling() = hentSoeknaderMedGyldigForBehandling(true)

    fun hentAntallSoeknaderIkkeGyldigForBehandling() = hentSoeknaderMedGyldigForBehandling(false)

    private fun hentSoeknaderMedGyldigForBehandling(gyldig: Boolean): Long {
        return connection.use {
            val statement = it.prepareStatement(
                """
                    SELECT COUNT(*) FROM soeknad_statistikk
                        WHERE gyldig_for_behandling = ?
                """.trimIndent()
            )
            statement.setBoolean(1, gyldig)
            val result = statement.executeQuery()
            result.single { getLong(1) }
        }
    }

    companion object {
        fun using(datasource: DataSource): SoeknadStatistikkRepository {
            return SoeknadStatistikkRepository(datasource)
        }
    }
}