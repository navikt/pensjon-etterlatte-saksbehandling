import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import ktortokenexchange.SecurityContextMediator
import ktortokenexchange.SecurityContextMediatorFactory


class ApplicationContext(configLocation: String? = null) {
    private val config: Config = configLocation?.let { ConfigFactory.load(it) } ?: ConfigFactory.load()

    fun securityMediator(): SecurityContextMediator = SecurityContextMediatorFactory.from(config)
    fun vilkaarDao() = VilkarDaoInMemory()
    fun vilkaarService(vilkaarDao: VilkaarDao) = VilkaarService(vilkaarDao)

}

fun main() {
    ApplicationContext()
        .also { Server(it).run() }
}
