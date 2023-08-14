package no.nav.etterlatte.metrics

import no.nav.etterlatte.libs.database.toList
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

class MetrikkerDao(private val dataSource: DataSource) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun antallOppgaver(): Double {
        dataSource.connection.use {
            val statement = it.prepareStatement(
                """
                    SELECT id 
                    FROM oppgave
                """.trimIndent()
            )
            val saker = statement.executeQuery().toList {
                getObject("id") as UUID
            }
            logger.info("Antall saker: ${saker.size}")
            return saker.size.toDouble()
        }
    }
}