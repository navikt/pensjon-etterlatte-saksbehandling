package no.nav.etterlatte.libs.common

fun Map<String, String>.requireEnvValue(key: String) =
    when (val value = this[key]) {
        null -> throw IllegalArgumentException("App env is missing required key $key")
        else -> value
    }

data class Miljoevariabler(
    val props: Map<String, String>,
) {
    fun requireEnvValue(key: String): String = props.requireEnvValue(key)

    fun maybeEnvValue(key: String): String? = props[key]

    operator fun get(key: String) = props[key]

    fun getValue(key: String): String = props.getValue(key)

    fun getOrDefault(
        key: String,
        default: String,
    ) = props.getOrDefault(key, default)

    fun append(
        key: String,
        value: (Miljoevariabler) -> String,
    ) = this.apply { props.toMutableMap()[key] = value(this) }

    fun containsKey(key: String) = props.containsKey(key)

    fun value(property: String): String = getValue(property)

    companion object {
        fun systemEnv() = Miljoevariabler(System.getenv())

        fun systemEnv(key: String) = System.getenv(key)
    }
}
