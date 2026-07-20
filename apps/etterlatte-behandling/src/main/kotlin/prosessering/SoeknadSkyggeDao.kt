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
 * «Allerede håndtert» = det finnes en task for `soeknadId` i en hvilken som helst status
 * unntatt [Status.AVBRUTT]. Vi inkluderer bevisst [Status.FULLFØRT] og [Status.STOPPET],
 * ikke bare uferdige (KLAR/KJØRER): søknad-eventet redeleveres jevnt over tid, så en task
 * rekker å fullføre før neste redelivery — en ren uferdig-sjekk ville derfor sluppet gjennom
 * en ny task per redelivery. For et *mottak* skal én søknad gi nøyaktig én task; en STOPPET
 * task følges opp via rekjør (operatør), ikke ved ny innkøing. Bare [Status.AVBRUTT] (operatør
 * har eksplisitt avfeid tasken) åpner for at søknaden kan køes på nytt.
 */
class SoeknadSkyggeDao(
    private val dataSource: DataSource,
    skjema: String = "prosessering",
) {
    private val tabell = "$skjema.task"

    fun harAlleredeHaandtertSoeknad(soeknadId: String): Boolean =
        dataSource.connection.use { connection ->
            connection.prepareStatement(harAlleredeHaandtertSql).use { statement ->
                statement.setString(1, soeknadMottakSkyggeType.navn)
                statement.setString(2, Status.AVBRUTT.name)
                statement.setString(3, soeknadId)
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
               AND status <> ?
               AND payload::jsonb ->> 'soeknadId' = ?
        )
        """.trimIndent()
}
