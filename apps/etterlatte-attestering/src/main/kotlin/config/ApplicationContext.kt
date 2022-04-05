package no.nav.etterlatte.config

import no.nav.etterlatte.attestering.AttestasjonDao
import no.nav.etterlatte.attestering.AttestasjonService
import no.nav.etterlatte.attestering.VedtaksMottaker
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import javax.sql.DataSource

class ApplicationContext(
    private val env: Map<String, String>
) {
    fun rapidsConnection() = RapidApplication.create(env)

    fun dataSourceBuilder() = DataSourceBuilder(
        jdbcUrl = jdbcUrl(
            host = env.required("DB_HOST"),
            port = env.required("DB_PORT"),
            databaseName = env.required("DB_DATABASE")
        ),
        username = env.required("DB_USERNAME"),
        password = env.required("DB_PASSWORD"),
    )

    fun attestasjonsDao(dataSource: DataSource) = AttestasjonDao { dataSource.connection }

    fun attestasjonService(attestasjonDao: AttestasjonDao) = AttestasjonService(attestasjonDao)

    fun vedtaksMottaker(rapidsConnection: RapidsConnection, attestasjonService: AttestasjonService) =
        VedtaksMottaker(rapidsConnection, attestasjonService)

    private fun jdbcUrl(host: String, port: String, databaseName: String) =
        "jdbc:postgresql://${host}:$port/$databaseName"
}

fun Map<String, String>.required(property: String): String =
    requireNotNull(this[property]) { "Property $property was null" }
