package no.nav.etterlatte.kafka

object Avrokonstanter {
    const val SPECIFIC_AVRO_READER_CONFIG = "specific.avro.reader"
    const val BASIC_AUTH_CREDENTIALS_SOURCE = SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE
    const val USER_INFO_CONFIG = SchemaRegistryClientConfig.USER_INFO_CONFIG
    const val SCHEMA_REGISTRY_URL_CONFIG = "schema.registry.url"

    private object SchemaRegistryClientConfig {
        const val BASIC_AUTH_CREDENTIALS_SOURCE = "basic.auth.credentials.source"
        const val USER_INFO_CONFIG = "basic.auth.user.info"
    }
}
