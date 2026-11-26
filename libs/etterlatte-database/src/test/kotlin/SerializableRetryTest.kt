package no.nav.etterlatte.libs.database

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException

class SerializableRetryTest {
    @Test
    fun `returnerer resultatet uten aa gjenta vellykkede kall`() =
        runBlocking<Unit> {
            var forsoek = 0

            val resultat =
                withSerializableRetry {
                    forsoek++
                    "resultat"
                }

            assertEquals("resultat", resultat)
            assertEquals(1, forsoek)
        }

    @Test
    fun `proever inntil tre ganger ved innpakket serialiseringsfeil`() =
        runBlocking<Unit> {
            var forsoek = 0

            val resultat =
                withSerializableRetry {
                    forsoek++
                    if (forsoek < 3) {
                        throw IllegalStateException("Databasefeil", SQLException("Konflikt", "40001"))
                    }
                    "resultat"
                }

            assertEquals("resultat", resultat)
            assertEquals(3, forsoek)
        }

    @Test
    fun `kaster siste serialiseringsfeil etter tre forsoek`() =
        runBlocking<Unit> {
            val feil = SQLException("Konflikt", "40001")
            var forsoek = 0

            val kastet =
                assertThrows<SQLException> {
                    withSerializableRetry {
                        forsoek++
                        throw feil
                    }
                }

            assertSame(feil, kastet)
            assertEquals(3, forsoek)
        }

    @Test
    fun `gjentar ikke andre databasefeil`() =
        runBlocking<Unit> {
            val feil = SQLException("Unikhetsbrudd", "23505")
            var forsoek = 0

            val kastet =
                assertThrows<SQLException> {
                    withSerializableRetry {
                        forsoek++
                        throw feil
                    }
                }

            assertSame(feil, kastet)
            assertEquals(1, forsoek)
        }

    @Test
    fun `gjentar ikke kansellerte kall`() =
        runBlocking<Unit> {
            val feil = CancellationException("Avbrutt").apply { initCause(SQLException("Konflikt", "40001")) }
            var forsoek = 0

            val kastet =
                assertThrows<CancellationException> {
                    withSerializableRetry {
                        forsoek++
                        throw feil
                    }
                }

            assertSame(feil, kastet)
            assertEquals(1, forsoek)
        }

    @Test
    fun `gjentar ikke kall som allerede har committet selv om feilen er innpakket`() =
        runBlocking<Unit> {
            val feil = IllegalStateException("Publisering feilet", EtterCommitException(SQLException("Konflikt", "40001")))
            var forsoek = 0

            val kastet =
                assertThrows<IllegalStateException> {
                    withSerializableRetry {
                        forsoek++
                        throw feil
                    }
                }

            assertSame(feil, kastet)
            assertEquals(1, forsoek)
        }
}
