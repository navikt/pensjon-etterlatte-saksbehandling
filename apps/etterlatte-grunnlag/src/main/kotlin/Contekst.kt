package no.nav.etterlatte

import java.sql.Connection

object Kontekst : ThreadLocal<Context>()

class Context(
    val AppUser: User,
    val databasecontxt: DatabaseKontekst
)

interface User {
    fun name(): String
    fun kanSetteKilde(): Boolean = false
}

class Self(private val prosess: String) : User {
    override fun name() = prosess
    override fun kanSetteKilde() = true
}

interface DatabaseKontekst {
    fun activeTx(): Connection
    fun <T> inTransaction(block: () -> T): T
}

fun <T> inTransaction(block: () -> T): T = Kontekst.get().databasecontxt.inTransaction {
    block()
}

fun databaseContext() = Kontekst.get().databasecontxt
