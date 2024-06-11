package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.GenerellDatabaseExtension
import no.nav.etterlatte.ResetDatabaseStatement
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

@ResetDatabaseStatement(
    """
    TRUNCATE hendelse CASCADE;
    TRUNCATE jobb CASCADE;
""",
)
class DatabaseExtension :
    GenerellDatabaseExtension(),
    AfterEachCallback {
    override fun afterEach(context: ExtensionContext) {
        resetDb()
    }
}
