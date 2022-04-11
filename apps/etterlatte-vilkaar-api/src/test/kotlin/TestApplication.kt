import io.ktor.auth.LolSecMediator
import io.mockk.every
import io.mockk.spyk
import no.nav.etterlatte.ApplicationContext
import no.nav.etterlatte.Server


fun main() {
    val applicationContext = spyk(ApplicationContext("/application-test.conf", System.getenv().toMutableMap())) {
        every { securityMediator() } returns LolSecMediator()
    }

    Server(applicationContext).run()
}
