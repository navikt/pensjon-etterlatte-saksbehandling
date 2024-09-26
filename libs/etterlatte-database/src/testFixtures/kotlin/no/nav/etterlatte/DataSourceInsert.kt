package no.nav.etterlatte

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.etterlatte.libs.database.transaction
import javax.sql.DataSource

fun DataSource.insert(
    tabellnavn: String,
    params: (tx: TransactionalSession) -> Map<String, Any?>,
) {
    this.transaction { tx ->
        val paramMap = params(tx)
        queryOf(
            """INSERT INTO $tabellnavn 
                    (${paramMap.keys.joinToString(", ") { it }})
                     VALUES 
                    (${paramMap.keys.joinToString(", ") { ":$it" }})""",
            paramMap = paramMap,
        ).let { query -> tx.run(query.asUpdate) }
    }
}
