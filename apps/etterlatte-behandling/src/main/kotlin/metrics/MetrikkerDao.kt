package no.nav.etterlatte.metrics

import no.nav.etterlatte.libs.database.toList
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.UUID

class MetrikkerDao(private val connection: () -> Connection) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun antallOppgaver(): Double {
        with(connection()) {
            val statement = prepareStatement(
                """
                    SELECT id 
                    FROM oppgave
                """.trimIndent()
            )
            val saker = statement.executeQuery().toList{
                getObject("id") as UUID
            }
            return saker.size.toDouble()
        }
    }
}