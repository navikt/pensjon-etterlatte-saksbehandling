package no.nav.etterlatte.libs.common

import java.util.UUID

fun UUID.toUUID30() =
    this
        .toString()
        .replace("-", "")
        .substring(0, 30)
        .let { UUID30(it) }

data class UUID30(
    val value: String,
)
