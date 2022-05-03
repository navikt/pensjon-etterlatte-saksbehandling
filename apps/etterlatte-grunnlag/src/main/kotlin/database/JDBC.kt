package no.nav.etterlatte.database

import java.sql.ResultSet
import java.time.LocalDateTime
import java.time.ZoneId


private val postgresTimeZone = ZoneId.of("UTC")

fun <T> ResultSet.singleOrNull(block: ResultSet.() -> T): T? {
    return if (next()) {
        block().also {
            require(!next()) { "Skal være unik" }
        }
    } else {
        null
    }
}

fun <T> ResultSet.toList(block: ResultSet.() -> T): List<T> {
    return generateSequence {
        if (next()) block()
        else null
    }.toList()
}