package no.nav.etterlatte.vilkaarsvurdering

import kotliquery.TransactionalSession
import kotliquery.sessionOf
import kotliquery.using
import javax.sql.DataSource

interface SessionFactory {
    fun <A> withTransactionalSession(block: (transactionalSession: TransactionalSession) -> A): A
}

class PostgresSessionFactory(private val ds: DataSource) : SessionFactory {
    override fun <A> withTransactionalSession(block: (transactionalSession: TransactionalSession) -> A): A {
        return using(sessionOf(ds)) { session ->
            session.transaction {
                block(it)
            }
        }
    }
}