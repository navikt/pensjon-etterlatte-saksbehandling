package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.auth.*
import io.ktor.config.*
import no.nav.etterlatte.grunnlag.*
import no.nav.security.token.support.ktor.tokenValidationSupport
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

interface BeanFactory {
    fun datasourceBuilder(): DataSourceBuilder
    fun grunnlagsService(): GrunnlagService
    fun tokenValidering(): Authentication.Configuration.()->Unit
    fun grunnlagDao(): GrunnlagDao
    fun opplysningDao(): OpplysningDao
    fun rapid():RapidsConnection
    fun grunnlagHendelser(): GrunnlagHendelser
    fun behandlingsFactory(): GrunnlagFactory
}

abstract class CommonFactory: BeanFactory{
    internal var cache: MutableMap<Any, Any> = mutableMapOf()
    internal inline fun <reified T> cached(creator: ()->T):T{
        if(!cache.containsKey(T::class.java)){
            cache[T::class.java] = creator() as Any
        }
        return cache[T::class.java] as T

    }

    override fun grunnlagHendelser(): GrunnlagHendelser {
        return cached { GrunnlagHendelser(rapid(), GrunnlagFactory(grunnlagDao(), opplysningDao()), datasourceBuilder().dataSource) }
    }
    override fun behandlingsFactory(): GrunnlagFactory {
        return cached { GrunnlagFactory(grunnlagDao(), opplysningDao()) }
    }

    override fun grunnlagsService(): GrunnlagService = RealGrunnlagService(grunnlagDao(), opplysningDao(), GrunnlagFactory(grunnlagDao(), opplysningDao())) //grunnlagHendelser().nyHendelse)

    override fun grunnlagDao(): GrunnlagDao = GrunnlagDao { databaseContext().activeTx() }
    override fun opplysningDao(): OpplysningDao = OpplysningDao { databaseContext().activeTx() }
}

class EnvBasedBeanFactory(val env: Map<String, String>): CommonFactory() {

    override fun datasourceBuilder(): DataSourceBuilder = cached { DataSourceBuilder(env) }
    override fun tokenValidering(): Authentication.Configuration.() -> Unit = { tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load())) }
    override fun rapid(): RapidsConnection {
        return RapidApplication.create(env)

    }
}