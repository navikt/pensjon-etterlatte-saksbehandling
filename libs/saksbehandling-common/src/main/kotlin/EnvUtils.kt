package no.nav.etterlatte.libs.common

fun Map<String, String>.requireEnvValue(key: String) =
    when (val value = this[key]) {
        null -> throw IllegalArgumentException("App env is missing required key $key")
        else -> value
    }

data class Miljoevariabler private constructor(
    val props: Map<String, String>,
) {
    fun requireEnvValue(key: String): String = props.requireEnvValue(key)

    fun requireEnvValue(key: EnvEnum): String = requireEnvValue(key.name())

    operator fun get(key: String) = props[key]

    operator fun get(key: EnvEnum) = get(key.name())

    fun getValue(key: EnvEnum): String = props.getValue(key.name())

    fun getOrDefault(
        key: EnvEnum,
        value: String,
    ) = props.getOrDefault(key.name(), value)

    fun append(
        key: EnvEnum,
        value: (Miljoevariabler) -> String,
    ) = this.apply { props.toMutableMap()[key.name()] = value(this) }

    fun append(props: Map<EnvEnum, String>): Miljoevariabler {
        val toMutableMap = this.props.toMutableMap()
        toMutableMap.putAll(
            props.entries.associate { it.key.name() to it.value },
        )
        return Miljoevariabler(toMutableMap)
    }

    fun containsKey(key: String) = props.containsKey(key)

    fun containsKey(key: EnvEnum) = containsKey(key.name())

    fun value(property: String): String = props.getValue(property)

    companion object {
        fun systemEnv() = Miljoevariabler(System.getenv())

        fun systemEnv(key: EnvEnum) = System.getenv(key.name())

        fun httpClient(props: Map<EnvEnum, String>) =
            props.entries
                .associate { it.key.name() to it.value }
                .let { Miljoevariabler(it) }
    }
}

interface EnvEnum {
    fun name(): String
}
