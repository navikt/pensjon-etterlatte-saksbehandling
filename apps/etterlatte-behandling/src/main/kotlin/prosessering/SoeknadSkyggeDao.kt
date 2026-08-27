package no.nav.etterlatte.prosessering

import javax.sql.DataSource

/** soeknadId er idempotens-nøkkelen: SoeknadSkyggeRiver er mutasjonsfri, så eventet redeleveres. */
class SoeknadSkyggeDao(
    private val dataSource: DataSource,
) {
    private val tabell = "public.prosessering_task"

    fun harAlleredeHaandtertSoeknad(soeknadId: String): Boolean =
        dataSource.connection.use { connection ->
            connection.prepareStatement(harAlleredeHaandtertSql).use { statement ->
                statement.setString(1, SoeknadMottakSkygge.navn)
                statement.setString(2, soeknadId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next() && resultSet.getBoolean(1)
                }
            }
        }

    private val harAlleredeHaandtertSql =
        """
        SELECT EXISTS(
            SELECT 1 FROM $tabell
             WHERE type = ?
               AND payload::jsonb ->> 'soeknadId' = ?
        )
        """.trimIndent()
}
