package no.nav.etterlatte.libs.common

fun Map<String, String>.requireEnvValue(key: String) =
    when (val value = this[key]) {
        null -> throw IllegalArgumentException("App env is missing required key $key")
        else -> value
    }

data class Miljoevariabler(val props: Map<String, String>) {
    fun requireEnvValue(key: String): String = props.requireEnvValue(key)

    fun maybeEnvValue(key: String): String? {
        return props[key]
    }

    operator fun get(key: String) = props[key]

    fun getValue(key: String): String = props.getValue(key)

    fun getOrDefault(
        key: String,
        default: String,
    ) = props.getOrDefault(key, default)
}
