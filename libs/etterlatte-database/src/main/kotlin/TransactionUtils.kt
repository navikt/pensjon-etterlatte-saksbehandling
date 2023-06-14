package no.nav.etterlatte.libs.database

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf

fun <T> TransactionalSession.hent(query: String, params: Map<String, Any>, converter: (r: Row) -> T) =
    queryOf(statement = query, paramMap = params)
        .let { this.run(it.map { row -> converter.invoke(row) }.asSingle) }