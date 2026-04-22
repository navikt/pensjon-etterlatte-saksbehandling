package no.nav.etterlatte

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.etterlatte.common.ConnectionAutoclosingImpl
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.libs.database.toList
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.sql.SQLException
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TransactionTest(
    val dataSource: DataSource,
) {
    companion object {
        @RegisterExtension
        private val dbExtension = DatabaseExtension()
    }

    private lateinit var databasecontxt: DatabaseContext

    @BeforeEach
    fun setup() {
        databasecontxt = DatabaseContext(dataSource)
    }

    @AfterEach
    fun tearDown() {
        inTransaction {
            ConnectionAutoclosingImpl(dataSource).hentConnection { connection ->
                connection.prepareStatement("drop table test_table").executeUpdate()
            }
        }
        Kontekst.remove()
    }

    @Test
    fun `skal kunne nøste transaksjoner`() {
        Kontekst.set(context(databasecontxt))

        val connectionAutoclosing = ConnectionAutoclosingImpl(dataSource)
        inTransaction { createTestTable(connectionAutoclosing) }

        inTransaction {
            connectionAutoclosing.hentConnection { connection ->
                connection.prepareStatement("insert INTO test_table values('Lorem')").executeUpdate()
            }

            inTransaction {
                connectionAutoclosing
                    .hentConnection { connection ->
                        connection.prepareStatement("INSERT INTO test_table VALUES('ipsum')").executeUpdate()
                    }
            }
        }

        inTransaction {
            connectionAutoclosing.hentConnection { connection ->
                val single =
                    connection
                        .prepareStatement("select text_column from test_table")
                        .executeQuery()
                        .toList { this.getString("text_column") }

                single shouldContainExactlyInAnyOrder listOf("Lorem", "ipsum")
            }
        }
    }

    @Test
    fun `skal rulle tilbake alt naar indre transaksjon feiler`() {
        Kontekst.set(context(databasecontxt))

        val connection = ConnectionAutoclosingImpl(dataSource)
        inTransaction { createTestTable(connection) }

        val ok = ThreadLocal.withInitial { false }
        shouldThrow<SQLException> {
            inTransaction {
                connection.hentConnection { connection ->
                    connection.prepareStatement("INSERT INTO test_table VALUES('bar')").executeUpdate()
                }
                ok.set(true)

                inTransaction {
                    connection
                        .hentConnection { connection ->
                            connection.prepareStatement("INSERT INTO test_table VALUES('a1234567890')").executeUpdate() // fails
                        }
                }
            }
        }

        ok.get() shouldBe true

        inTransaction {
            ConnectionAutoclosingImpl(dataSource).hentConnection { connection ->
                val single =
                    connection
                        .prepareStatement("select text_column from test_table")
                        .executeQuery()
                        .toList { this.getString("text_column") }

                single shouldBe emptyList()
            }
        }
    }

    private fun createTestTable(connectionAutoclosing: ConnectionAutoclosingImpl): Int =
        connectionAutoclosing.hentConnection { connection ->
            connection.prepareStatement("CREATE TABLE test_table(text_column varchar(10))").executeUpdate()
        }

    private fun context(databaseContext: DatabaseContext): Context =
        Context(
            AppUser = mockk(),
            databasecontxt = databaseContext,
            sakTilgangDao = mockk(),
            brukerTokenInfo = mockk(),
        )
}
