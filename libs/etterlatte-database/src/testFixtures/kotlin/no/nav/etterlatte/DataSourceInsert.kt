package no.nav.etterlatte

import kotliquery.queryOf
import no.nav.etterlatte.libs.database.transaction
import javax.sql.DataSource

fun DataSource.insert(
    tabellnavn: String,
    params: Map<String, Any>,
) {
    this.transaction { tx ->
        queryOf(
            """INSERT INTO $tabellnavn 
                    (${params.keys.joinToString(", ") { it }})
                     VALUES 
                    (${params.keys.joinToString(", ") { ":$it" }})""",
            paramMap = params,
        ).let { query -> tx.run(query.asUpdate) }
    }
}
