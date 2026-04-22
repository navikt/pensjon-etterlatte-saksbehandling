package no.nav.etterlatte.common

import no.nav.etterlatte.DatabaseKontekst
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource

class DatabaseContext(
    private val ds: DataSource,
) : DatabaseKontekst {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(DatabaseContext::class.java)
    }

    private val transaktionOpen = AtomicBoolean(false)

    private var transactionalConnection: Connection? = null

    override fun harIntransaction(): Boolean = transaktionOpen.get()

    override fun activeTx(): Connection =
        transactionalConnection ?: throw IllegalStateException(
            "No currently open transaction",
        )

    override fun <T> inTransaction(block: () -> T): T {
        val transactionAlreadyOpen = transaktionOpen.getAndSet(true)
        val connection = ds.connection

        if (transactionAlreadyOpen) {
            return block()
        }
        val autocommit = connection.autoCommit
        return try {
            connection.autoCommit = false
            transactionalConnection = connection
            val retur = block()
            connection.commit()
            retur
        } catch (ex: Throwable) {
            connection.rollback()
            logger.warn("Reason for rollback", ex)
            throw ex
        } finally {
            transactionalConnection = null
            connection.autoCommit = autocommit
            connection.close()
            transaktionOpen.set(false)
        }
    }
}
