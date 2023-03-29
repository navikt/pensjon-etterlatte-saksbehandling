package no.nav.etterlatte.config

import com.typesafe.config.Config

fun samle(config: Config, env: Map<String, String>): Map<String, String> {
    val conf = config
        .root()
        .toList()
        .map { unwrap(mutableSetOf(), Node(it.first, it.second.unwrapped())) }
        .filter { it.isNotEmpty() }
        .flatten()
        .associate { it.key to it.value!!.toString() }
    return conf + env
}

private fun unwrap(set: MutableSet<Node>, pair: Node): Set<Node> {
    when (pair.value) {
        is Map<*, *> -> {
            pair.value.entries.forEach {
                set.addAll(
                    unwrap(
                        set,
                        Node(pair.key + "." + it.key.toString(), it.value)
                    )
                )
            }
            return set
        }

        is List<*> -> {
            pair.value.forEach {
                set.addAll(
                    unwrap(
                        set,
                        Node(pair.key, it)
                    )
                )
            }
            return set
        }

        is String -> return mutableSetOf(Node(pair.key, pair.value.toString()))
        else -> return set
    }
}

private data class Node(val key: String, val value: Any?)