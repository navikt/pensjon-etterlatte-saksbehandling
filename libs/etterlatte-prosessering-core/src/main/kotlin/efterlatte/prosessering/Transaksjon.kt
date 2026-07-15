package efterlatte.prosessering

/**
 * Markørinterface for en transaksjon eid av vertsapplikasjonen. Core kjenner ikke
 * `java.sql`; postgres-modulen leverer en konkret `JdbcTransaksjon(connection)`.
 *
 * Poenget er outbox-garantien: `TaskProdusent.opprett` skriver task-raden på
 * *samme* transaksjon som forretnings-skrivet, slik at begge committer eller ingen.
 * Den som åpnet transaksjonen eier den — produsenten committer og lukker den aldri.
 */
interface Transaksjon

@JvmInline
value class TaskId(
    val verdi: Long,
)
