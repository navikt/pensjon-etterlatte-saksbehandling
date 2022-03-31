package no.nav.etterlatte

import io.ktor.auth.*

import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.sikkerhet.tokenTestSupportAcceptsAllTokens
import org.testcontainers.containers.PostgreSQLContainer

fun main(){

    /*
    Krever kjørende docker
    Spinner opp appen uten sikkerhet (inkommende token blir godtatt uten validering)
    Kaller vilkårsvurdering i dev
     */

    val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:12")
    postgreSQLContainer.start()
    postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
    postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

    appFromBeanfactory(LocalAppBeanFactory(postgreSQLContainer.jdbcUrl)).run()
    postgreSQLContainer.stop()

}

class LocalAppBeanFactory(val jdbcUrl: String): CommonFactory(){
    override fun datasourceBuilder(): DataSourceBuilder = DataSourceBuilder(mapOf("DB_JDBC_URL" to jdbcUrl))
    override fun tokenValidering(): Authentication.Configuration.() -> Unit = Authentication.Configuration::tokenTestSupportAcceptsAllTokens
    override fun rapid(): KafkaProdusent<String, String> {
        return object: KafkaProdusent<String, String>{
            override fun publiser(noekkel: String, verdi: String, headers: Map<String, ByteArray>): Pair<Int, Long> {
                return 0 to 0
            }
        }
    }
}