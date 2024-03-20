package no.nav.etterlatte.fordeler

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FordelerRiverRepositoryTest(datasource: DataSource) {
    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()
    }

    private val fordelerRepo = FordelerRepository(datasource)

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    @Test
    fun `For søknader som ikke har en fordeling skal det ikke returneres fordeling`() {
        assertNull(fordelerRepo.finnFordeling(3L))
    }

    @Test
    fun `For søknader som har en fordeling skal den returneres`() {
        assertNull(fordelerRepo.finnFordeling(3L))
        val ulagretFordeling = FordeltTransient(3, "GJENNY", emptyList())

        fordelerRepo.lagreFordeling(ulagretFordeling)

        val lagretFordeling =
            fordelerRepo.finnFordeling(ulagretFordeling.soeknadId)?.also {
                assertNotNull(it)
            }!!

        assertEquals(ulagretFordeling.soeknadId, lagretFordeling.soeknadId)
        assertEquals(ulagretFordeling.fordeling, lagretFordeling.fordeling)
    }

    @Test
    fun `Skal kunne lagre kriterier i en fordeling`() {
        assertNull(fordelerRepo.finnFordeling(3L))
        val ulagretFordeling = FordeltTransient(3, "PESYS", listOf("FOR_GAMMEL", "FOR_UNG"))

        fordelerRepo.lagreFordeling(ulagretFordeling)

        assertIterableEquals(ulagretFordeling.kriterier, fordelerRepo.finnKriterier(ulagretFordeling.soeknadId))
    }

    @Test
    fun `Skal kunne fine ut hvor mange om er fordelt til et system`() {
        assertEquals(0, fordelerRepo.antallFordeltTil("GJENNY"))

        fordelerRepo.lagreFordeling(FordeltTransient(2, "GJENNY", emptyList()))
        fordelerRepo.lagreFordeling(FordeltTransient(4, "PESYS", emptyList()))
        fordelerRepo.lagreFordeling(FordeltTransient(8, "GJENNY", emptyList()))

        assertEquals(2, fordelerRepo.antallFordeltTil("GJENNY"))
        assertEquals(1, fordelerRepo.antallFordeltTil("PESYS"))
    }
}
