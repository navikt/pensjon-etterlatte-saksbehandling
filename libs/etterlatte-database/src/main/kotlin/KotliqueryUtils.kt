package no.nav.etterlatte.libs.database

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger(DataSource::class.java)

fun <A> DataSource.transaction(returnGeneratedKey: Boolean = false, operation: (TransactionalSession) -> A): A =
    using(sessionOf(this, returnGeneratedKey)) { session ->
        session.transaction { operation(it) }
    }

fun DataSource.opprett(query: String, params: Map<String, Any?>, loggtekst: String) =
    this.transaction { tx ->
        queryOf(
            statement = query,
            paramMap = params
        )
            .also { logger.info(loggtekst) }
            .let { tx.run(it.asExecute) }
    }

fun DataSource.oppdater(
    query: String,
    params: Map<String, Any?>,
    loggtekst: String,
    ekstra: ((tx: TransactionalSession) -> Unit)? = null
) =
    this.transaction { tx ->
        queryOf(
            statement = query,
            paramMap = params
        )
            .also { logger.info(loggtekst) }
            .let { tx.run(it.asUpdate) }
            .also { ekstra?.invoke(tx) }
    }

fun <T> DataSource.hent(query: String, params: Map<String, Any>, converter: (r: Row) -> T) =
    using(sessionOf(this)) { session ->
        queryOf(statement = query, paramMap = params)
            .let { query -> session.run(query.map { row -> converter.invoke(row) }.asSingle) }
    }

fun <T> DataSource.hentListe(
    query: String,
    params: (s: Session) -> Map<String, Any?> = { mapOf() },
    converter: (r: Row) -> T
): List<T> = using(sessionOf(this)) { session ->
    queryOf(statement = query, paramMap = params.invoke(session))
        .let { query ->
            session.run(
                query.map { row -> converter.invoke(row) }
                    .asList
            )
        }
}

fun Row.tidspunkt(columnLabel: String) = sqlTimestamp(columnLabel).toTidspunkt()