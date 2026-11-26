package no.nav.etterlatte.common

import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.libs.database.EtterCommitException
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
    private val etterCommitHandlinger = mutableListOf<() -> Unit>()

    override fun harIntransaction(): Boolean = transaktionOpen.get()

    override fun activeTx(): Connection =
        transactionalConnection ?: throw IllegalStateException(
            "No currently open transaction",
        )

    override fun etterCommit(block: () -> Unit) {
        if (harIntransaction()) {
            etterCommitHandlinger.add(block)
        } else {
            block()
        }
    }

    override fun <T> inTransaction(block: () -> T): T {
        if (transaktionOpen.getAndSet(true)) {
            throw IllegalStateException("Støtter ikke nøstede transaksjoner")
        }
        val c = ds.connection
        val autocommit = c.autoCommit
        val (retur, etterCommit) =
            try {
                c.autoCommit = false
                transactionalConnection = c
                val retur = block()
                c.commit()
                retur to etterCommitHandlinger.toList()
            } catch (ex: Throwable) {
                c.rollback()
                logger.warn("Reason for rollback", ex)
                throw ex
            } finally {
                etterCommitHandlinger.clear()
                transactionalConnection = null
                c.autoCommit = autocommit
                c.close()
                transaktionOpen.set(false)
            }
        try {
            etterCommit.forEach { it() }
        } catch (ex: Exception) {
            throw EtterCommitException(ex)
        }
        return retur
    }
}
