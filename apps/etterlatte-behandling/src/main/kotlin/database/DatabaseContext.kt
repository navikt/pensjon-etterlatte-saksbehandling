package no.nav.etterlatte.database

import no.nav.etterlatte.DatabaseKontekst
import java.sql.Connection
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource

class DatabaseContext (private val ds: DataSource ): DatabaseKontekst {
    private val transaktionOpen = AtomicBoolean(false)

    private var transactionalConnection: Connection? = null

    override fun activeTx(): Connection = transactionalConnection?: throw IllegalStateException("No currently open transaction")

    override fun <T> inTransaction(block: ()->T): T{
        if(transaktionOpen.getAndSet(true)){
            throw IllegalStateException("Støtter ikke nøstede transactsjoner")
        }
        val c = ds.connection
        val autocommit = c.autoCommit
        return try{
            c.autoCommit = false
            transactionalConnection = c
            val retur = block()
            c.commit()
            println("committed")
            retur
        }catch (ex:Throwable){
            c.rollback()
            println("rolled back")

            throw ex
        }finally {
            transactionalConnection = null
            c.autoCommit = autocommit
            c.close()
            transaktionOpen.set(false)
        }
    }

}