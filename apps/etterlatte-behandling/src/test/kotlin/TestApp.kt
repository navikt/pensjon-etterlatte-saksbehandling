package no.nav.etterlatte

import io.ktor.auth.*

import no.nav.etterlatte.behandling.KtorVilkarClient
import no.nav.etterlatte.behandling.VilkaarKlient
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

    appFromBeanfactory(LocalAppBeanFactory(postgreSQLContainer.jdbcUrl)).start(true)
    postgreSQLContainer.stop()

}

class LocalAppBeanFactory(val jdbcUrl: String): CommonFactory(){
    override fun datasourceBuilder(): DataSourceBuilder = DataSourceBuilder(mapOf("DB_JDBC_URL" to jdbcUrl))
    override fun vilkaarKlient(): VilkaarKlient = KtorVilkarClient("https://etterlatte-vilkaar.dev.intern.nav.no")
    override fun tokenValidering(): Authentication.Configuration.() -> Unit = Authentication.Configuration::tokenTestSupportAcceptsAllTokens
}