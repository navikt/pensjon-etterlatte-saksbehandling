package no.nav.etterlatte.prosessering

import efterlatte.prosessering.TaskId
import efterlatte.prosessering.TaskProdusent
import efterlatte.prosessering.TaskType
import efterlatte.prosessering.postgres.JdbcTransaksjon
import kotliquery.TransactionalSession
import no.nav.etterlatte.databaseContext
import java.time.Instant

/**
 * Bro mellom behandlingens kotliquery-transaksjoner og prosessering-bibliotekets
 * [efterlatte.prosessering.Transaksjon]-port. Dette er outbox-koblingen i praksis
 * (PoC Fase 4e — herding av produsent-API-et mot vertens virkelige transaksjon).
 *
 * Når behandling skriver forretningsdata (f.eks. sak/behandling) i en
 * kotliquery-transaksjon, kan den legge en prosessering-task på *samme* transaksjon:
 * begge committer eller ingen. Det er hele outbox-garantien — task-en skrives ikke
 * i en egen transaksjon som kan lykkes mens forretnings-skrivet ruller tilbake.
 *
 * Vi pakker kotliquerys underliggende [java.sql.Connection] i bibliotekets
 * [JdbcTransaksjon]. Produsenten skriver på den, men committer eller lukker den
 * **aldri** — kotliquery-transaksjonen eier connectionen og styrer commit/rollback.
 */
fun <P : Any> TaskProdusent.opprettISammeTransaksjon(
    transaksjon: TransactionalSession,
    type: TaskType<P>,
    payload: P,
    triggerTid: Instant? = null,
): TaskId =
    opprett(
        transaksjon = JdbcTransaksjon(transaksjon.connection.underlying),
        type = type,
        payload = payload,
        triggerTid = triggerTid,
    )

/**
 * Outbox-kobling mot behandlingens tråd-lokale transaksjon (PoC Fase 4e, Steg 2). Skal kalles
 * *inne i* en `inTransaction { … }`-blokk: behandling åpner da en `java.sql.Connection` med
 * `autoCommit=false` og legger den i den tråd-lokale [no.nav.etterlatte.common.DatabaseContext],
 * tilgjengelig via [databaseContext]. Vi henger task-en på nettopp den connectionen, så task-raden
 * committer eller ruller tilbake atomisk sammen med behandlings-skrivet.
 *
 * Kaster `IllegalStateException` («No currently open transaction») hvis den kalles utenfor en
 * `inTransaction`-blokk — en task uten et forretnings-skriv å henge på skal bruke
 * `opprettFrittstående`, ikke denne.
 */
fun <P : Any> TaskProdusent.opprettPaaAktivBehandlingstransaksjon(
    type: TaskType<P>,
    payload: P,
    triggerTid: Instant? = null,
): TaskId =
    opprett(
        transaksjon = JdbcTransaksjon(databaseContext().activeTx()),
        type = type,
        payload = payload,
        triggerTid = triggerTid,
    )
