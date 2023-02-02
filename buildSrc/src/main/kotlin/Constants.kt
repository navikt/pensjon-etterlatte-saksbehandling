object NavFelles {
    const val RapidAndRiversKtor2 = "com.github.navikt:rapids-and-rivers:2022061809451655538329.d6deccc62862"
    const val TokenClientCore = "no.nav.security:token-client-core:2.1.2"
    const val TokenValidationKtor2 = "no.nav.security:token-validation-ktor-v2:2.1.2"
    const val MockOauth2Server = "no.nav.security:mock-oauth2-server:0.5.7"
}

object Cache {
    const val Caffeine = "com.github.ben-manes.caffeine:caffeine:3.1.1"
}

object Kotlinx {
    const val CoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4"
    const val CoroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4"
}

object Ktor2 {
    private const val version = "2.0.3"

    const val OkHttp = "io.ktor:ktor-client-okhttp:$version"
    const val ClientCore = "io.ktor:ktor-client-core:$version"
    const val ClientLoggingJvm = "io.ktor:ktor-client-logging-jvm:$version"
    const val ClientAuth = "io.ktor:ktor-client-auth:$version"
    const val ClientJackson = "io.ktor:ktor-client-jackson:$version"
    const val ClientLogging = "io.ktor:ktor-client-logging:$version"
    const val ClientCioJvm = "io.ktor:ktor-client-cio-jvm:$version"
    const val ServerCore = "io.ktor:ktor-server-core:$version"
    const val ServerCio = "io.ktor:ktor-server-cio:$version"
    const val Auth = "io.ktor:ktor-server-auth:$version"
    const val AuthJwt = "io.ktor:ktor-server-auth-jwt:$version"
    const val ServerContentNegotiation = "io.ktor:ktor-server-content-negotiation:$version"
    const val ClientContentNegotiation = "io.ktor:ktor-client-content-negotiation:$version"
    const val ServerHtmlBuilder = "io.ktor:ktor-server-html-builder:$version"
    const val Jackson = "io.ktor:ktor-serialization-jackson:$version"
    const val CallLogging = "io.ktor:ktor-server-call-logging:$version"
    const val StatusPages = "io.ktor:ktor-server-status-pages:$version"
    const val MetricsMicrometer = "io.ktor:ktor-server-metrics-micrometer:$version"
    const val Mustache = "io.ktor:ktor-server-mustache:$version"

    const val ClientMock = "io.ktor:ktor-client-mock:$version"
    const val ServerTests = "io.ktor:ktor-server-tests:$version"
}

object Kafka {
    const val Clients = "org.apache.kafka:kafka-clients:3.1.1"
    const val Avro = "org.apache.avro:avro:1.11.0"
    const val AvroSerializer = "io.confluent:kafka-avro-serializer:7.1.1"
    const val EmbeddedEnv = "no.nav:kafka-embedded-env:3.1.6"
}

object Jackson {
    private const val version = "2.13.5"

    const val DatatypeJsr310 = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$version"
    const val DatatypeJdk8 = "com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$version"
    const val ModuleKotlin = "com.fasterxml.jackson.module:jackson-module-kotlin:$version"
    const val Databind = "com.fasterxml.jackson.core:jackson-databind:$version"
    const val Core = "com.fasterxml.jackson.core:jackson-core:$version"
    const val Xml = "com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$version"
}

object Jupiter {
    private const val version = "5.9.2"

    const val Api = "org.junit.jupiter:junit-jupiter-api:$version"
    const val Params = "org.junit.jupiter:junit-jupiter-params:$version"
    const val Engine = "org.junit.jupiter:junit-jupiter-engine:$version"
    const val Root = "org.junit.jupiter:junit-jupiter:$version"
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

object WireMock {
    const val WireMock = "com.github.tomakehurst:wiremock:2.33.2"
}

object Kotest {
    private const val version = "4.6.3"

    const val AssertionsCore = "io.kotest:kotest-assertions-core:$version"
}

object Database {
    const val HikariCP = "com.zaxxer:HikariCP:5.0.0"
    const val FlywayDB = "org.flywaydb:flyway-core:8.5.13"
    const val Postgresql = "org.postgresql:postgresql:42.3.8"
    const val KotliQuery = "com.github.seratch:kotliquery:1.7.0"
}

object TestContainer {
    const val Jupiter = "org.testcontainers:junit-jupiter:1.17.3"
    const val Postgresql = "org.testcontainers:postgresql:1.17.3"
}