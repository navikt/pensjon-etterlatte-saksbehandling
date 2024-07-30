package no.nav.etterlatte.libs.database

import no.nav.etterlatte.libs.common.EnvEnum

enum class DatabaseConfig : EnvEnum {
    DB_JDBC_URL,
    DB_HOST,
    DB_USERNAME,
    DB_PASSWORD,
    DB_PORT,
    DB_DATABASE,
    ;

    override fun key() = name
}
