import no.nav.etterlatte.libs.database.InvalidMigrationScriptVersion
import no.nav.etterlatte.libs.database.ManglerDobbelUnderscore
import no.nav.etterlatte.libs.database.SqlMaaHaaStorforbokstav
import no.nav.etterlatte.libs.database.validateMigrationScriptVersions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DataSourceValidatorTest {
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

    @Test
    fun `Må ha dobbel underscore i migreringsfil`() {
        val migrationScripts = listOf("V1_numerouno.sql", "V2__endreoppgavetabell.sql")

        assertThrows<ManglerDobbelUnderscore> {
            validateMigrationScriptVersions(listOf(migrationScripts).flatten())
        }
    }

    @Test
    fun `Må ha stor forbokstav i migreringsfil`() {
        val migrationScripts = listOf("v1__numerouno.sql", "V2__endreoppgavetabell.sql")

        assertThrows<SqlMaaHaaStorforbokstav> {
            validateMigrationScriptVersions(listOf(migrationScripts).flatten())
        }
    }
}
