package soeknad

import no.nav.etterlatte.appFromEnv
import org.testcontainers.containers.PostgreSQLContainer

fun main(){

    /*
    Krever kjørende docker
    Spinner opp appen uten sikkerhet (inkommende token blir godtatt uten validering)

     */

    val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:12")
    postgreSQLContainer.start()
    postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
    postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

    appFromEnv(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl, "profil" to "test")).start(true)
    postgreSQLContainer.stop()

}