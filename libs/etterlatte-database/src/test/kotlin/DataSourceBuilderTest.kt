import no.nav.etterlatte.libs.database.InvalidMigrationScriptVersion
import no.nav.etterlatte.libs.database.validateMigrationScriptVersions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DataSourceBuilderTest {
    @Test
    fun `Skal migrere unike versjoner i migrering og prod`() {
        val migrationScripts = listOf("V1__numerouno.sql", "V3__numerotres.sql")
        val migrationScriptsProd = listOf("V2__numerodos.sql", "V4__numerodos.sql")
        validateMigrationScriptVersions(listOf(migrationScripts, migrationScriptsProd).flatten())
    }

    @Test
    fun `Skal ikke migrere like versjoner i migrering og prod`() {
        val migrationScripts = listOf("V1__numerouno.sql", "V2__endreoppgavetabell.sql")
        val migrationScriptsProd = listOf("V2__numerodos.sql")

        assertThrows<InvalidMigrationScriptVersion> {
            validateMigrationScriptVersions(listOf(migrationScripts, migrationScriptsProd).flatten())
        }
    }
}
