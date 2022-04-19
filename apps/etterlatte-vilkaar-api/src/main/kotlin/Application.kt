package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.auth.Authentication
import io.ktor.config.HoconApplicationConfig
import no.nav.etterlatte.config.DataSourceBuilder
import no.nav.etterlatte.config.GcpJdbcUrlBuilder
import no.nav.security.token.support.ktor.tokenValidationSupport


class ApplicationContext(
    configLocation: String? = null,
    val env: Map<String, String>
) {
    private val config: Config = configLocation?.let { ConfigFactory.load(it) } ?: ConfigFactory.load()

    fun dataSourceBuilder() = DataSourceBuilder(
        GcpJdbcUrlBuilder(
            gcpProjectId = env.required("GCP_TEAM_PROJECT_ID"),
            databaseRegion = env.required("DB_REGION"),
            databaseInstance = env.required("DB_INSTANCE"),
            databaseName = env.required("DB_VILKAAR_API_DATABASE")
        ),
        databaseUsername = env.required("DB_VILKAAR_API_USERNAME"),
        databasePassword = env.required("DB_VILKAAR_API_PASSWORD"),
    )

    fun tokenValidering(): Authentication.Configuration.() -> Unit = {
        tokenValidationSupport(config = HoconApplicationConfig(config))
    }

    fun vilkaarDao() = VilkaarDaoJdbc(dataSourceBuilder().dataSource())
    fun vilkaarService(vilkaarDao: VilkaarDao) = VilkaarService(vilkaarDao)

}

fun Map<String, String>.required(property: String): String =
    requireNotNull(this[property]) { "Property $property was null" }

fun main() {
    ApplicationContext(env = System.getenv().toMutableMap())
        .also { Server(it).run() }
}
