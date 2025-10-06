package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.GenerellDatabaseExtension
import no.nav.etterlatte.ResetDatabaseStatement
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

@ResetDatabaseStatement(
    """
TRUNCATE grunnlagshendelse
""",
)
class GrunnlagDbExtension :
    GenerellDatabaseExtension(),
    AfterEachCallback {
    override fun afterEach(context: ExtensionContext) {
        resetDb()
    }
}
