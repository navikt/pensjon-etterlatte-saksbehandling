package no.nav.etterlatte.libs.database

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.slf4j.LoggerFactory
import java.io.Serializable
import javax.sql.DataSource

class KotliqueryRepository(private val datasource: DataSource) {

    private val logger = LoggerFactory.getLogger(KotliqueryRepository::class.java)

    fun opprett(query: String, params: Map<String, Any?>, loggtekst: String) =
        using(sessionOf(datasource)) { session ->
            session.transaction { tx ->
                queryOf(
                    statement = query,
                    paramMap = params
                )
                    .also { logger.info(loggtekst) }
                    .let { tx.run(it.asExecute) }
            }
        }

    fun oppdater(
        query: String,
        params: Map<String, Any?>,
        loggtekst: String,
        ekstra: ((tx: TransactionalSession) -> Unit)? = null
    ) =
        using(sessionOf(datasource)) { session ->
            session.transaction { tx ->
                queryOf(
                    statement = query,
                    paramMap = params
                )
                    .also { logger.info(loggtekst) }
                    .let { tx.run(it.asUpdate) }
                    .also { ekstra?.invoke(tx) }
            }
        }

    fun opprettFlere(query: String, params: List<Map<String, Serializable?>>, loggtekst: String) =
        using(sessionOf(datasource)) { session ->
            session.transaction { tx ->
                logger.info(loggtekst)
                tx.batchPreparedNamedStatement(query, params)
            }
        }

    fun <T> hentMedKotliquery(query: String, params: Map<String, Any>, converter: (r: Row) -> T) =
        using(sessionOf(datasource)) { session ->
            queryOf(statement = query, paramMap = params)
                .let { query -> session.run(query.map { row -> converter.invoke(row) }.asSingle) }
        }

    fun <T> hentMedKotliquery(
        query: String,
        params: Map<String, Any>,
        tx: TransactionalSession,
        converter: (r: Row) -> T
    ) = queryOf(statement = query, paramMap = params)
        .let { tx.run(it.map { row -> converter.invoke(row) }.asSingle) }

    fun <T> hentListeMedKotliquery(
        query: String,
        params: (s: Session) -> Map<String, Any?>,
        converter: (r: Row) -> T
    ): List<T> = using(sessionOf(datasource)) { session ->
        queryOf(statement = query, paramMap = params.invoke(session))
            .let { query ->
                session.run(
                    query.map { row -> converter.invoke(row) }
                        .asList
                )
            }
    }
}