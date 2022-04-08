import io.ktor.auth.LolSecMediator
import io.mockk.every
import io.mockk.spyk


fun main() {
    val applicationContext = spyk(ApplicationContext("/application-test.conf")) {
        every { securityMediator() } returns LolSecMediator()
    }

    Server(applicationContext).run()
}
