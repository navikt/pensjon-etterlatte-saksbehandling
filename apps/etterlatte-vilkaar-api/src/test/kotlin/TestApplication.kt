import io.ktor.auth.Authentication
import io.mockk.every
import io.mockk.spyk
import no.nav.etterlatte.ApplicationContext
import no.nav.etterlatte.Server
import sikkerhet.tokenTestSupportAcceptsAllTokens


fun main() {
    val applicationContext = spyk(ApplicationContext("/application-test.conf", System.getenv().toMutableMap())) {
        every { tokenValidering() } returns Authentication.Configuration::tokenTestSupportAcceptsAllTokens
    }

    Server(applicationContext).run()
}
