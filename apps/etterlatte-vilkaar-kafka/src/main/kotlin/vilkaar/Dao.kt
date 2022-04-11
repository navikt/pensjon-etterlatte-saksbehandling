package vilkaar

import java.sql.Connection
import java.sql.ResultSet
import java.time.ZoneId
import javax.sql.DataSource


class Dao<D>(private val ds: DataSource, private val daoFactory: (Connection)->D){

    fun <T> withoutTransaction(block:D.()->T):T{
        val c = ds.connection
        val autocommit = c.autoCommit

        return try{
            daoFactory(c).block()
        }finally {
            c.autoCommit = autocommit
            c.close()
        }
    }

    fun <T> inTransaction(block:D.()->T):T{
        val c = ds.connection
        val autocommit = c.autoCommit
        return try{
            c.autoCommit = false
            val result = daoFactory(c).block()
            c.commit()
            result
        }catch (ex:Throwable){
            c.rollback()
            throw ex
        }finally {
            c.autoCommit = autocommit
            c.close()
        }
    }
}

private val postgresTimeZone = ZoneId.of("UTC")

fun <T> ResultSet.singleOrNull(block: ResultSet.() -> T): T? {
    return if (next()) {
        block().also {
            require(!next()) { "Skal være unik" }
        }
    } else {
        null
    }
}

fun <T> ResultSet.toList(block: ResultSet.() -> T): List<T> {
    return generateSequence {
        if (next()) block()
        else null
    }.toList()
}