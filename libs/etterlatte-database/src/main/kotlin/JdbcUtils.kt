package no.nav.etterlatte.libs.database

import java.sql.ResultSet

fun <T> ResultSet.singleOrNull(block: ResultSet.() -> T): T? {
    return if (next()) {
        block().also {
            require(!next()) { "Skal være unik" }
        }
    } else {
        null
    }
}

fun <T> ResultSet.single(block: ResultSet.() -> T): T {
    return requireNotNull(singleOrNull(block)) {
        "Skal ha en verdi"
    }
}

fun <T> ResultSet.toList(block: ResultSet.() -> T): List<T> {
    val list = ArrayList<T>()
    while (next()) {
        list.add(block())
    }
    return list
}