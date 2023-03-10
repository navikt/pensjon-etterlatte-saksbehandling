package no.nav.etterlatte.trygdetid.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.trygdetid.TrygdetidRepository
import no.nav.etterlatte.trygdetid.TrygdetidService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

class ApplicationContext {
    val config: Config = ConfigFactory.load()
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
    val datasource = InMemoryDs()
    val trygdetidService = TrygdetidService(TrygdetidRepository(datasource))
}

class InMemoryDs {

    companion object TrygdetidGrunnlagTable : Table() {
        val bosted = varchar("bosted", 10)
        val periodeFra = date("periodeFra")
        val periodeTil = date("periodeTil")
    }

    var trygdetidTable = TrygdetidGrunnlagTable

    fun migrate() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")
        transaction {
            SchemaUtils.create(TrygdetidGrunnlagTable)
            fillDb()
        }
    }

    private fun fillDb() {
        trygdetidTable.insert {
            it[bosted] = "Norge"
            it[periodeFra] = DateTime.now()
            it[periodeTil] = DateTime.now()
        }
    }
}