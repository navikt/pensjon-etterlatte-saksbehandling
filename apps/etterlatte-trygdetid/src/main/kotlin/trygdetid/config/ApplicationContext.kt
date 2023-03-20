package no.nav.etterlatte.trygdetid.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.trygdetid.TrygdetidRepository
import no.nav.etterlatte.trygdetid.TrygdetidService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction

class ApplicationContext {
    val config: Config = ConfigFactory.load()
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
    val datasource = InMemoryDs()
    val trygdetidService = TrygdetidService(TrygdetidRepository(datasource))
}

class InMemoryDs {

    object TrygdetidTable : Table() {
        val id = uuid("id")
        val behandlingsId = uuid("behandling_id")
        val opprettet = timestamp("opprettet")
        val nasjonalTrygdetid = integer("nasjonalTrygdetid").nullable()
        val fremtidigTrygdetid = integer("fremtidigTrygdetid").nullable()
        val totalTrygdetid = integer("totalTrygdetid").nullable()
    }
    var trygdetidTable = TrygdetidTable

    object TrygdetidGrunnlagTable : Table() {
        val id = uuid("id")
        val trygdetidId = uuid("trygdetid_id")
        val trygdetidType = varchar("type", 50)
        val bosted = varchar("bosted", 50)
        val periodeFra = date("periodeFra")
        val periodeTil = date("periodeTil")
        val kilde = varchar("kilde", 50)
    }

    var trygdetidGrunnlagTable = TrygdetidGrunnlagTable

    fun migrate() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")
        transaction {
            SchemaUtils.create(TrygdetidTable, TrygdetidGrunnlagTable)
        }
    }
}