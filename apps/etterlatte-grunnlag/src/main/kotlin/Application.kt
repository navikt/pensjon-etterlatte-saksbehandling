package no.nav.etterlatte

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.routing.route
import no.nav.etterlatte.EnvKey.HTTP_PORT
import no.nav.etterlatte.grunnlag.AldersovergangDao
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.initialisering.run

fun main() {
    ApplicationBuilder().run()
}

class ApplicationBuilder {
    private val env = Miljoevariabler.systemEnv()
    private val ds = DataSourceBuilder.createDataSource(env).also { it.migrate() }
    private val config: Config = ConfigFactory.load()

    private val opplysningDao = OpplysningDao(ds)
    private val aldersovergangDao = AldersovergangDao(ds)

    private val engine =
        initEmbeddedServer(
            routePrefix = "/api",
            httpPort = env.getOrDefault(HTTP_PORT, "8080").toInt(),
            applicationConfig = config,
        ) {
            route("grunnlag") {
                opplysningDaoRoutes(opplysningDao)
                aldersovergangDaoRoutes(aldersovergangDao)
            }
        }

    fun run() = engine.run()
}
