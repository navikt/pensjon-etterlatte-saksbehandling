import org.codehaus.groovy.tools.shell.util.Logger.io

object NavFelles {
    const val RapidAndRivers = "com.github.navikt:rapids-and-rivers:20210617121814-3e67e4d"
    const val TokenClientCore = "no.nav.security:token-client-core:1.3.3"
    const val TokenValidationKtor = "no.nav.security:token-validation-ktor:1.3.3"
    const val MockOauth2Server = "no.nav.security:mock-oauth2-server:0.3.1"
}

object Kotlinx {
    const val CoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2"
}

object Ktor {
    private const val version = "1.6.1"

    const val OkHttp = "io.ktor:ktor-client-okhttp:$version"
    const val ServerNetty = "io.ktor:ktor-server-netty:$version"
    const val ClientCore = "io.ktor:ktor-client-core:$version"
    const val ClientCoreJvm = "io.ktor:ktor-client-core-jvm:$version"
    const val ClientLoggingJvm = "io.ktor:ktor-client-logging-jvm:$version"
    const val ClientAuth = "io.ktor:ktor-client-auth:$version"
    const val ClientAuthJvm = "io.ktor:ktor-client-auth-jvm:$version"
    const val ClientApache = "io.ktor:ktor-client-apache:$version"
    const val ClientJackson = "io.ktor:ktor-client-jackson:$version"
    const val ClientLogging = "io.ktor:ktor-client-logging:$version"
    const val ClientCioJvm = "io.ktor:ktor-client-cio-jvm:$version"
    const val ServerCore = "io.ktor:ktor-server-core:$version"
    const val ServerCio = "io.ktor:ktor-server-cio:$version"
    const val Auth = "io.ktor:ktor-auth:$version"
    const val AuthJwt = "io.ktor:ktor-auth-jwt:$version"
    const val Jackson = "io.ktor:ktor-jackson:$version"
    const val MetricsMicrometer = "io.ktor:ktor-metrics-micrometer:$version"

    const val ClientMock = "io.ktor:ktor-client-mock:$version"
    const val ServerTests = "io.ktor:ktor-server-tests:$version"
}

object Kafka {
    const val Clients = "org.apache.kafka:kafka-clients:2.5.0"
    const val Avro = "org.apache.avro:avro:1.10.0"
    const val AvroSerializer = "io.confluent:kafka-avro-serializer:5.0.0"
    const val EmbeddedEnv = "no.nav:kafka-embedded-env:2.7.0"
}

object Jackson {
    private const val version = "2.12.1"

    const val DatatypeJsr310 = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$version"
    const val DatatypeJdk8 = "com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$version"
    const val ModuleKotlin = "com.fasterxml.jackson.module:jackson-module-kotlin:$version"
}

object Jupiter {
    private const val version = "5.7.2"

    const val Api = "org.junit.jupiter:junit-jupiter-api:$version"
    const val Params = "org.junit.jupiter:junit-jupiter-params:$version"
    const val Engine = "org.junit.jupiter:junit-jupiter-engine:$version"
}

object Logging {
    const val Slf4jApi = "org.slf4j:slf4j-api:1.7.30"
    const val LogbackClassic = "ch.qos.logback:logback-classic:1.2.3"
    const val LogstashLogbackEncoder = "net.logstash.logback:logstash-logback-encoder:6.6"
}

object Micrometer {
    const val Prometheus = "io.micrometer:micrometer-registry-prometheus:1.5.5"
}

object MockK {
    const val MockK = "io.mockk:mockk:1.12.0"
}

object Kotest {
    private const val version = "4.6.3"

    const val AssertionsCore = "io.kotest:kotest-assertions-core:$version"
}
