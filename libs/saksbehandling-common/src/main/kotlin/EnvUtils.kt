package no.nav.etterlatte.libs.common

fun Map<String, String>.requireEnvValue(key: String) =
    when (val value = this[key]) {
        null -> throw IllegalArgumentException("App env is missing required key $key")
        else -> value
    }

data class Miljoevariabler private constructor(
    val props: Map<String, String>,
) {
    fun requireEnvValue(key: EnvEnum): String = props.requireEnvValue(key.name())

    operator fun get(key: String) = props[key]

    operator fun get(key: EnvEnum) = get(key.key())

    fun getValue(key: EnvEnum): String = props.getValue(key.key())

    fun getOrDefault(
        key: EnvEnum,
        value: String,
    ) = props.getOrDefault(key.key(), value)

    fun append(
        key: EnvEnum,
        value: (Miljoevariabler) -> String,
    ) = Miljoevariabler(props + (key.key() to value(this)))

    fun append(props: Map<EnvEnum, String>): Miljoevariabler {
        val toMutableMap = this.props.toMutableMap()
        toMutableMap.putAll(
            props.entries.associate { it.key.key() to it.value },
        )
        return Miljoevariabler(toMutableMap)
    }

    fun containsKey(key: EnvEnum) = props.containsKey(key.name())

    companion object {
        fun systemEnv(): Miljoevariabler = Miljoevariabler(System.getenv())

        fun systemEnv(key: EnvEnum) = System.getenv(key.key())

        fun httpClient(props: Map<EnvEnum, String>) =
            props.entries
                .associate { it.key.key() to it.value }
                .let { Miljoevariabler(it) }
    }
}

interface EnvEnum {
    fun key(): String
}
