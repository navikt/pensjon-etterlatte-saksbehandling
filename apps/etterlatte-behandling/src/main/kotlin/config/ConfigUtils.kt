package no.nav.etterlatte.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueType

fun samle(config: Config, env: Map<String, String>) =
    config.entrySet().associate { it.key to it.value.asString() } + env

private fun ConfigValue.asString(): String = when (valueType()) {
    ConfigValueType.STRING, ConfigValueType.NUMBER, ConfigValueType.BOOLEAN -> toString()
    ConfigValueType.NULL, null -> ""
    ConfigValueType.OBJECT -> toString() // todo
    ConfigValueType.LIST -> toString() // todo
}