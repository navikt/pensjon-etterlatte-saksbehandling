package no.nav.etterlatte.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.database.EtterCommitException
import no.nav.etterlatte.libs.database.withSerializableRetry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

class DatabaseContextTransactionTest {
    @Test
    fun `starter ny transaksjon og forkaster handlinger naar commit feiler`() =
        runBlocking<Unit> {
            val foersteConnection = mockk<Connection>(relaxed = true)
            val andreConnection = mockk<Connection>(relaxed = true)
            every { foersteConnection.autoCommit } returns true
            every { andreConnection.autoCommit } returns true
            every { foersteConnection.commit() } throws SQLException("Konflikt ved commit", "40001")
            val dataSource = mockk<DataSource>()
            every { dataSource.connection } returnsMany listOf(foersteConnection, andreConnection)
            val database = DatabaseContext(dataSource)
            val sendteHendelser = mutableListOf<Int>()
            var forsoek = 0

            val resultat =
                withSerializableRetry {
                    database.inTransaction {
                        val detteForsoeket = ++forsoek
                        assertTrue(database.harIntransaction())
                        database.etterCommit {
                            assertFalse(database.harIntransaction())
                            sendteHendelser.add(detteForsoeket)
                        }
                        assertTrue(sendteHendelser.isEmpty())
                        "resultat"
                    }
                }

            assertEquals("resultat", resultat)
            assertEquals(2, forsoek)
            assertEquals(listOf(2), sendteHendelser)
            verify(exactly = 1) {
                foersteConnection.rollback()
                foersteConnection.close()
                andreConnection.commit()
                andreConnection.close()
            }
            verify(exactly = 0) { andreConnection.rollback() }
        }

    @Test
    fun `gjentar ikke transaksjonen hvis en handling feiler etter commit`() =
        runBlocking<Unit> {
            val connection = mockk<Connection>(relaxed = true)
            every { connection.autoCommit } returns true
            val dataSource = mockk<DataSource>()
            every { dataSource.connection } returns connection
            val database = DatabaseContext(dataSource)
            val feil = SQLException("Feil etter commit", "40001")
            var forsoek = 0

            val kastet =
                assertThrows<EtterCommitException> {
                    withSerializableRetry {
                        database.inTransaction {
                            forsoek++
                            database.etterCommit { throw feil }
                        }
                    }
                }

            assertSame(feil, kastet.cause)
            assertEquals(1, forsoek)
            assertFalse(database.harIntransaction())
            verify(exactly = 1) { connection.commit() }
            verify(exactly = 0) { connection.rollback() }
        }

    @Test
    fun `kjoerer handlingen direkte uten en aktiv transaksjon`() {
        val database = DatabaseContext(mockk())
        var utfoert = false

        database.etterCommit { utfoert = true }

        assertTrue(utfoert)
    }
}
