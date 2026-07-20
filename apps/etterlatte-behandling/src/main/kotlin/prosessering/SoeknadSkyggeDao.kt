package no.nav.etterlatte.prosessering

import efterlatte.prosessering.Status
import javax.sql.DataSource

/**
 * Host-side idempotens-sjekk for skyggekjøring (PoC Fase 4d).
 *
 * Skygge-riveren er bevisst mutasjonsfri (ingen `precondition`/`publish`), så samme
 * `soeknad_innsendt`-event redeleveres på hver rapid-syklus → uten dedupe blir det én
 * ny task per syklus. Løsningen ligger **produsent-side** i host-en, ikke i biblioteket:
 * `soeknadId` er den naturlige idempotens-nøkkelen for denne task-typen, og hva som er
 * «samme task» er verts-domene, ikke gjenbrukbar infra. En generell
 * `(type, payload)`-unik-constraint i biblioteket er bevisst utsatt (jf. `04-outbox-api.md`).
 *
 * «Uferdig» = [Status.KLAR] eller [Status.KJØRER]. En [Status.STOPPET] task venter på en
 * operatør-beslutning (rekjør/avbryt) og skal ikke blokkere ny innkøing; en [Status.FULLFØRT]
 * er ferdig håndtert.
 */
class SoeknadSkyggeDao(
    private val dataSource: DataSource,
    skjema: String = "prosessering",
) {
    private val tabell = "$skjema.task"

    fun finnesUferdigTaskForSoeknad(soeknadId: String): Boolean =
        dataSource.connection.use { connection ->
            connection.prepareStatement(finnesUferdigSql).use { statement ->
                statement.setString(1, soeknadMottakSkyggeType.navn)
                statement.setString(2, Status.KLAR.name)
                statement.setString(3, Status.KJØRER.name)
                statement.setString(4, soeknadId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next() && resultSet.getBoolean(1)
                }
            }
        }

    private val finnesUferdigSql =
        """
        SELECT EXISTS(
            SELECT 1 FROM $tabell
             WHERE type = ?
               AND status IN (?, ?)
               AND payload::jsonb ->> 'soeknadId' = ?
        )
        """.trimIndent()
}
