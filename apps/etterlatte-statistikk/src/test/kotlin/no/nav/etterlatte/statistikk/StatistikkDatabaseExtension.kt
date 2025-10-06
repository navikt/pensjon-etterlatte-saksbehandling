package no.nav.etterlatte.statistikk

import no.nav.etterlatte.GenerellDatabaseExtension
import no.nav.etterlatte.ResetDatabaseStatement
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

@ResetDatabaseStatement(
    """
    TRUNCATE TABLE aktivitetsplikt;
""",
)
class StatistikkDatabaseExtension :
    GenerellDatabaseExtension(),
    AfterEachCallback {
    override fun afterEach(p0: ExtensionContext) {
        resetDb()
    }
}
