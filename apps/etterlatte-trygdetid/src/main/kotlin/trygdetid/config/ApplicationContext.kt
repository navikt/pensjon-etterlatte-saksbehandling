package no.nav.etterlatte.trygdetid.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.trygdetid.TrygdetidRepository
import no.nav.etterlatte.trygdetid.TrygdetidService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.*

class ApplicationContext {
    val config: Config = ConfigFactory.load()
    val properties: ApplicationProperties = ApplicationProperties.fromEnv(System.getenv())
    val datasource = InMemoryDs()
    val trygdetidService = TrygdetidService(TrygdetidRepository(datasource))
}

class InMemoryDs {

    object TrygdetidTable : Table() {
        val behandlingsId = uuid("behandling_id")
        val nasjonalTrygdetid = integer("nasjonalTrygdetid").nullable()
        val fremtidigTrygdetid = integer("fremtidigTrygdetid").nullable()
        val oppsummertTrygdetid = integer("oppsummertTrygdetid").nullable()
    }
    var trygdetidTable = TrygdetidTable

    object TrygdetidGrunnlagTable : Table() {
        val behandlingsId = uuid("behandling_id")
        val trygdetidType = varchar("type", 50)
        val bosted = varchar("bosted", 50)
        val periodeFra = date("periodeFra")
        val periodeTil = date("periodeTil")
    }

    var trygdetidGrunnlagTable = TrygdetidGrunnlagTable

    fun migrate() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "org.h2.Driver")
        transaction {
            SchemaUtils.create(TrygdetidTable, TrygdetidGrunnlagTable)
            fillDb()
        }
    }

    private fun fillDb() {
        trygdetidTable.insert {
            it[behandlingsId] = UUID.fromString("11bf9683-4edb-403c-99da-b6ec6ff7fc31")
        }
        trygdetidGrunnlagTable.insert {
            it[behandlingsId] = UUID.fromString("11bf9683-4edb-403c-99da-b6ec6ff7fc31")
            it[trygdetidType] = "NASJONAL_TRYGDETID"
            it[bosted] = "Norge"
            it[periodeFra] = LocalDate.now()
            it[periodeTil] = LocalDate.now()
        }
    }
}