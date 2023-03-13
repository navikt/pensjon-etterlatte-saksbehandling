package no.nav.etterlatte.libs.common

fun Map<String, String>.requireEnvValue(key: String) =
    when (val value = this[key]) {
        null -> throw IllegalArgumentException("app env is missing required key $key")
        else -> value
    }

data class Miljoevariabler(val props: Map<String, String>) {
    fun toMutableMap() = props.toMutableMap() // Håper å få bort denne i løpet av refaktoriseringa
    fun requireEnvValue(key: String): String = props.requireEnvValue(key)
    operator fun get(key: String) = props[key]
}