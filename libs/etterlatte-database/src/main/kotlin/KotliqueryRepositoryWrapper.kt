package no.nav.etterlatte.libs.database

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.slf4j.LoggerFactory
import java.io.Serializable
import javax.sql.DataSource

class KotliqueryRepositoryWrapper(private val datasource: DataSource) {

    private val logger = LoggerFactory.getLogger(KotliqueryRepositoryWrapper::class.java)

    fun opprett(query: String, params: Map<String, Any?>, loggtekst: String) =
        using(sessionOf(datasource)) { session ->
            queryOf(
                statement = query,
                paramMap = params
            )
                .also { logger.info(loggtekst) }
                .let { session.run(it.asExecute) }
        }

    fun oppdater(query: String, params: Map<String, Any>, loggtekst: String) =
        using(sessionOf(datasource)) { session ->
            queryOf(
                statement = query,
                paramMap = params
            )
                .also { logger.info(loggtekst) }
                .let { session.run(it.asUpdate) }
        }

    fun opprettFlere(query: String, params: List<Map<String, Serializable?>>, loggtekst: String) =
        using(sessionOf(datasource)) { session ->
            logger.info(loggtekst)
            session.batchPreparedNamedStatement(query, params)
        }

    fun <T> hentMedKotliquery(
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