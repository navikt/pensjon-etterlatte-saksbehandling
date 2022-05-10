package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.auth.*
import io.ktor.config.*
import no.nav.etterlatte.grunnlag.*
import no.nav.security.token.support.ktor.tokenValidationSupport

interface BeanFactory {
    fun datasourceBuilder(): DataSourceBuilder
    fun grunnlagsService(): GrunnlagService
    fun tokenValidering(): Authentication.Configuration.()->Unit
    fun opplysningDao(): OpplysningDao
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

    override fun behandlingsFactory(): GrunnlagFactory {
        return cached { GrunnlagFactory(opplysningDao()) }
    }

    override fun grunnlagsService(): GrunnlagService = RealGrunnlagService(opplysningDao(), GrunnlagFactory(opplysningDao())) //grunnlagHendelser().nyHendelse)

    override fun opplysningDao(): OpplysningDao = OpplysningDao { databaseContext().activeTx() }
}

class EnvBasedBeanFactory(val env: Map<String, String>): CommonFactory() {

    override fun datasourceBuilder(): DataSourceBuilder = cached { DataSourceBuilder(env) }
    override fun tokenValidering(): Authentication.Configuration.() -> Unit = { tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load())) }

}