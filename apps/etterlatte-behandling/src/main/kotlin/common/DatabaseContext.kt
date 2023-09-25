package no.nav.etterlatte.common

import no.nav.etterlatte.DatabaseKontekst
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource

class DatabaseContext(private val ds: DataSource) : DatabaseKontekst {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(DatabaseContext::class.java)
    }

    private val transaktionOpen = AtomicBoolean(false)

    private var transactionalConnection: Connection? = null

    override fun activeTx(): Connection =
        transactionalConnection ?: throw IllegalStateException(
            "No currently open transaction",
        )

    override fun <T> inTransaction(
        gjenbruk: Boolean,
        block: () -> T,
    ): T {
        if (transaktionOpen.getAndSet(true)) {
            if (gjenbruk) {
                return block()
            }
            throw IllegalStateException("Støtter ikke nøstede transactsjoner")
        }
        val c = ds.connection
        val autocommit = c.autoCommit
        return try {
            c.autoCommit = false
            transactionalConnection = c
            val retur = block()
            c.commit()
            retur
        } catch (ex: Throwable) {
            c.rollback()
            logger.error("Reason for rollback", ex)
            throw ex
        } finally {
            transactionalConnection = null
            c.autoCommit = autocommit
            c.close()
            transaktionOpen.set(false)
        }
    }
}
