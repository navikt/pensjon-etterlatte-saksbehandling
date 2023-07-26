package no.nav.etterlatte.libs.database

import kotliquery.Row
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

fun TransactionalSession.opprett(query: String, params: Map<String, Any?>, loggtekst: String) =
    this.let { tx ->
        queryOf(
            statement = query,
            paramMap = params
        )
            .also { logger.info(loggtekst) }
            .let { tx.run(it.asExecute) }
    }

fun TransactionalSession.oppdater(
    query: String,
    params: Map<String, Any?>,
    loggtekst: String,
    ekstra: ((tx: TransactionalSession) -> Unit)? = null
) =
    queryOf(statement = query, paramMap = params)
        .also { logger.info(loggtekst) }
        .let { this.run(it.asUpdate) }
        .also { ekstra?.invoke(this) }

fun <T> TransactionalSession.hent(query: String, params: Map<String, Any>, converter: (r: Row) -> T) =
    queryOf(statement = query, paramMap = params)
        .let { query -> this.run(query.map { row -> converter.invoke(row) }.asSingle) }

fun <T> TransactionalSession.hentListe(
    query: String,
    params: () -> Map<String, Any?> = { mapOf() },
    converter: (r: Row) -> T
): List<T> = queryOf(statement = query, paramMap = params.invoke())
    .let { query ->
        this.run(
            query.map { row -> converter.invoke(row) }
                .asList
        )
    }

fun Row.tidspunkt(columnLabel: String) = sqlTimestamp(columnLabel).toTidspunkt()