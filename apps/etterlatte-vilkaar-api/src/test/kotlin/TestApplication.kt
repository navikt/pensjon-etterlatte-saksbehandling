import io.ktor.auth.LolSecMediator
import io.mockk.every
import io.mockk.spyk


fun main() {
    val applicationContext = spyk(ApplicationContext("/application-test.conf", System.getenv().toMutableMap())) {
        every { securityMediator() } returns LolSecMediator()
    }

    Server(applicationContext).run()
}
