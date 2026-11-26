package no.nav.etterlatte.libs.database

import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class SerializableRetryIntegrationTest {
    companion object {
        @Container
        @JvmField
        val postgres = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    }

    @Test
    fun `ruller tilbake hele transaksjonen og leser ferske data ved nytt forsoek`() =
        runBlocking<Unit> {
            DataSourceBuilder
                .createDataSource(
                    jdbcUrl = postgres.jdbcUrl,
                    username = postgres.username,
                    password = postgres.password,
                ).unwrap(HikariDataSource::class.java)
                .use { dataSource ->
                    dataSource.connection.use { connection ->
                        connection.createStatement().use { statement ->
                            statement.execute("CREATE TABLE retry_verdi (id INT PRIMARY KEY, verdi INT NOT NULL)")
                            statement.execute("CREATE TABLE retry_forsoek (forsoek INT PRIMARY KEY)")
                            statement.execute("INSERT INTO retry_verdi (id, verdi) VALUES (1, 0)")
                        }
                    }
                    var forsoek = 0
                    val lesteVerdier = mutableListOf<Int>()

                    val resultat =
                        withSerializableRetry {
                            dataSource.transaction { tx ->
                                forsoek++
                                val lestVerdi =
                                    requireNotNull(
                                        tx.run(queryOf("SELECT verdi FROM retry_verdi WHERE id = 1").map { it.int("verdi") }.asSingle),
                                    )
                                lesteVerdier.add(lestVerdi)
                                tx.run(queryOf("INSERT INTO retry_forsoek (forsoek) VALUES (?)", forsoek).asUpdate)

                                if (forsoek == 1) {
                                    dataSource.connection.use { konkurrerendeConnection ->
                                        konkurrerendeConnection.createStatement().use { statement ->
                                            statement.executeUpdate("UPDATE retry_verdi SET verdi = 1 WHERE id = 1")
                                        }
                                    }
                                }
                                tx.run(queryOf("UPDATE retry_verdi SET verdi = verdi + 1 WHERE id = 1").asUpdate)
                                lestVerdi
                            }
                        }

                    assertEquals(2, forsoek)
                    assertEquals(listOf(0, 1), lesteVerdier)
                    assertEquals(1, resultat)
                    dataSource.transaction { tx ->
                        assertEquals(
                            listOf(2),
                            tx.run(queryOf("SELECT forsoek FROM retry_forsoek").map { it.int("forsoek") }.asList),
                        )
                        assertEquals(
                            2,
                            tx.run(queryOf("SELECT verdi FROM retry_verdi WHERE id = 1").map { it.int("verdi") }.asSingle),
                        )
                    }
                }
        }
}
