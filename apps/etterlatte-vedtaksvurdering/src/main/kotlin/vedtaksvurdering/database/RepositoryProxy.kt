package no.nav.etterlatte.vedtaksvurdering.database

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class RepositoryProxy(private val datasource: DataSource) {

    private val logger = LoggerFactory.getLogger(RepositoryProxy::class.java)

    internal fun opprett(query: String, params: Map<String, Any?>, loggtekst: String) =
        using(sessionOf(datasource)) { session ->
            queryOf(
                statement = query,
                paramMap = params
            )
                .also { logger.info(loggtekst) }
                .let { session.run(it.asExecute) }
        }

    internal fun oppdater(query: String, params: Map<String, Any>, loggtekst: String) =
        using(sessionOf(datasource)) { session ->
            queryOf(
                statement = query,
                paramMap = params
            )
                .also { logger.info(loggtekst) }
                .let { session.run(it.asUpdate) }
        }

    internal fun <T> hentMedKotliquery(
        query: String,
        params: Map<String, Any>,
        converter: (r: Row) -> T
    ) = using(sessionOf(datasource)) { session ->
        queryOf(statement = query, paramMap = params)
            .let { query ->
                session.run(
                    query.map { row -> converter.invoke(row) }
                        .asSingle
                )
            }
    }

    internal fun <T> hentListeMedKotliquery(
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