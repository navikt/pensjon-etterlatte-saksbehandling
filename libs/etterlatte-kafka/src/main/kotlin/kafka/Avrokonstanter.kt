package no.nav.etterlatte.kafka

object Avrokonstanter {
    const val SPECIFIC_AVRO_READER_CONFIG = "specific.avro.reader"
    const val BASIC_AUTH_CREDENTIALS_SOURCE = SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE

    private object SchemaRegistryClientConfig {
        const val BASIC_AUTH_CREDENTIALS_SOURCE = "basic.auth.credentials.source"
    }
}
