package no.nav.etterlatte.libs.database

import kotliquery.TransactionalSession
import kotliquery.sessionOf
import kotliquery.using
import javax.sql.DataSource

fun <A> DataSource.transaction(returnGeneratedKey: Boolean = false, operation: (TransactionalSession) -> A): A =
    using(sessionOf(this, returnGeneratedKey)) { session ->
        session.transaction { operation(it) }
    }