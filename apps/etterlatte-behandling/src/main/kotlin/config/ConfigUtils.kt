package no.nav.etterlatte.config

import com.typesafe.config.Config

fun samle(config: Config, env: Map<String, String>): Map<String, String> {
    val conf = config
        .root()
        .toList()
        .map { unwrap(mutableSetOf(), Pair(it.first, it.second.unwrapped())) }
        .filter { it.isNotEmpty() }
        .flatten()
        .associate { it.first to it.second }
    return conf + env
}

private fun unwrap(set: MutableSet<Pair<String, String>>, pair: Pair<String, Any?>): Set<Pair<String, String>> {
    if (pair.second is Map<*, *>) {
        (pair.second as Map<*, *>).entries.forEach {
            set.addAll(
                unwrap(
                    set,
                    Pair(pair.first + "." + it.key.toString(), it.value)
                )
            )
        }
        return set
    }
    if (pair.second is List<*>) {
        (pair.second as List<*>).forEach {
            set.addAll(
                unwrap(
                    set,
                    Pair(pair.first, it)
                )
            )
        }
        return set
    }
    if (pair.second is String) {
        return mutableSetOf(Pair(pair.first, pair.second.toString()))
    }
    return set
}