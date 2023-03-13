package no.nav.etterlatte.libs.common

fun Map<String, String>.requireEnvValue(key: String) =
    when (val value = this[key]) {
        null -> throw IllegalArgumentException("app env is missing required key $key")
        else -> value
    }